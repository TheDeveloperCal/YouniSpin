package com.youni.younispin.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.youni.younispin.data.model.RadioStation
import com.youni.younispin.data.model.Song
import com.youni.younispin.data.repository.RadioRepository
import com.youni.younispin.data.repository.SomaFmClient
import com.youni.younispin.data.repository.SomaFmTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(UnstableApi::class)
class PlaybackController(
    private val context: Context,
    private val radioRepository: RadioRepository? = null,
    private val onTrackPlayed: (Song) -> Unit = {}
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var seekJob: Job? = null
    private var radioMetadataJob: Job? = null
    private var radioArtworkJob: Job? = null
    private var radioRetryJob: Job? = null
    private var radioRetryAttempt = 0
    private var lastRadioTrackKey: String? = null
    private var savedVolume: Float = 1f

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    val sleepTimer = SleepTimerManager(
        onTimerExpired = { pause(); restoreVolume() },
        onFadeVolume = { setPlayerVolume(it) }
    )

    val equalizerManager = EqualizerManager.getInstance(context)

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, YouniPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                setupControllerListener()
                syncStateFromPlayer()
            } catch (e: Exception) {
                Log.e(TAG, "Failed connecting MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupControllerListener() {
        val c = controller ?: return
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    _state.value.radioStation?.let { station ->
                        if (station.source == SomaFmClient.SOURCE_SOMAFM &&
                            radioMetadataJob?.isActive != true
                        ) {
                            startSomaMetadataUpdates(station)
                        }
                    }
                } else {
                    stopProgressTracker()
                    radioMetadataJob?.cancel()
                    radioMetadataJob = null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        streamError = if (playbackState == Player.STATE_READY) null else it.streamError
                    )
                }
                if (playbackState == Player.STATE_READY && _state.value.currentSong?.isStream == true) {
                    radioRetryAttempt = 0
                    radioRetryJob?.cancel()
                    radioRetryJob = null
                }
                syncStateFromPlayer()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncCurrentTrack(mediaItem)
            }

            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    if (entry is IcyInfo && !entry.title.isNullOrBlank()) {
                        applyIcyTitle(entry.title!!)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        streamError = error.localizedMessage ?: "The stream could not be played"
                    )
                }
                scheduleRadioRetry(error.localizedMessage ?: "The stream could not be played")
            }
        })
        try {
            val extras = c.sessionExtras
            val sessionId = extras.getInt(YouniPlaybackService.EXTRA_AUDIO_SESSION, 0)
            if (sessionId > 0) {
                _state.update { it.copy(audioSessionId = sessionId) }
            }
        } catch (_: Exception) {
        }
    }

    private fun syncStateFromPlayer() {
        val c = controller ?: return
        val isPlaying = c.isPlaying
        val pos = c.currentPosition.coerceAtLeast(0L)
        val dur = c.duration.coerceAtLeast(0L)
        val shuffle = c.shuffleModeEnabled
        val rep = when (c.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        _state.update {
            it.copy(
                isPlaying = isPlaying,
                currentPositionMs = pos,
                durationMs = dur,
                shuffleMode = shuffle,
                repeatMode = rep,
                playbackSpeed = c.playbackParameters.speed
            )
        }
        if (isPlaying) startProgressTracker() else stopProgressTracker()
    }

    private fun syncCurrentTrack(mediaItem: MediaItem?) {
        val c = controller ?: return
        val currentQueue = _state.value.queue
        val index = c.currentMediaItemIndex

        val matchedSong = if (index in currentQueue.indices) {
            currentQueue[index]
        } else if (mediaItem != null) {
            val title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown"
            val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown"
            Song(
                id = 0,
                title = title,
                artist = artist,
                album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "",
                duration = c.duration.coerceAtLeast(0L),
                uriString = mediaItem.mediaId,
                path = mediaItem.mediaId
            )
        } else null

        val previousState = _state.value
        val matchingRadio = previousState.radioStation?.takeIf {
            matchedSong?.isStream == true && it.url == matchedSong.path
        }
        _state.update {
            it.copy(
                currentSong = matchedSong,
                queueIndex = index,
                durationMs = c.duration.coerceAtLeast(0L),
                streamMetadataTitle = null,
                audioQuality = null,
                radioStation = matchingRadio,
                radioTrackHistory = if (matchingRadio != null) it.radioTrackHistory else emptyList(),
                radioTrackArtworkUrl = if (matchingRadio != null) it.radioTrackArtworkUrl else null,
                streamError = null,
                isBuffering = false
            )
        }
        if (matchingRadio == null) {
            radioMetadataJob?.cancel()
            radioMetadataJob = null
            radioArtworkJob?.cancel()
            radioArtworkJob = null
            lastRadioTrackKey = null
        }

        if (matchedSong != null) {
            refreshAudioQuality(matchedSong)
            onTrackPlayed(matchedSong)
        }
    }

    private fun refreshAudioQuality(song: Song?) {
        if (song == null) {
            _state.update { it.copy(audioQuality = null) }
            return
        }

        scope.launch(Dispatchers.IO) {
            val quality = AudioQualityInspector.inspect(context, song)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(audioQuality = quality) }
            }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.isPlaying) {
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    val dur = c.duration.coerceAtLeast(0L)
                    val extrasId = try {
                        c.sessionExtras.getInt(YouniPlaybackService.EXTRA_AUDIO_SESSION, 0)
                    } catch (_: Exception) { 0 }
                    _state.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            audioSessionId = if (extrasId > 0) extrasId else it.audioSessionId
                        )
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: return
        if (songs.isEmpty() || startIndex !in songs.indices) return

        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.displayTitle)
                .setArtist(song.displayArtist)
                .setAlbumTitle(song.displayAlbum)
                .setArtworkUri(if (song.artworkUri.isNotBlank()) Uri.parse(song.artworkUri) else null)
                .build()

            MediaItem.Builder()
                .setMediaId(song.uriString.ifBlank { song.path })
                .setUri(song.uriString.ifBlank { song.path })
                .setMediaMetadata(metadata)
                .build()
        }

        _state.update {
            it.copy(
                queue = songs,
                queueIndex = startIndex,
                currentSong = songs[startIndex],
                audioQuality = null
            )
        }

        refreshAudioQuality(songs[startIndex])
        c.setMediaItems(mediaItems, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playRadioStream(station: RadioStation) {
        val c = controller ?: return
        radioMetadataJob?.cancel()
        radioArtworkJob?.cancel()
        radioRetryJob?.cancel()
        radioRetryJob = null
        radioRetryAttempt = 0
        lastRadioTrackKey = null
        val streamSong = Song(
            id = station.id,
            title = station.name,
            artist = station.genre,
            album = "Live Stream & Radio",
            duration = 0L,
            path = station.url,
            uriString = station.url,
            isStream = true,
            customArtworkPath = null
        )

        val mediaItem = radioMediaItem(station, streamSong)

        _state.update {
            it.copy(
                queue = listOf(streamSong),
                queueIndex = 0,
                currentSong = streamSong,
                streamMetadataTitle = null,
                audioQuality = null,
                radioStation = station,
                radioTrackHistory = emptyList(),
                radioTrackArtworkUrl = null,
                isBuffering = true,
                streamError = null
            )
        }

        refreshAudioQuality(streamSong)
        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()
        if (station.source == SomaFmClient.SOURCE_SOMAFM && !station.sourceId.isNullOrBlank()) {
            startSomaMetadataUpdates(station)
        }
    }

    private fun radioMediaItem(station: RadioStation, song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.displayTitle.ifBlank { station.name })
            .setArtist(song.displayArtist.ifBlank { station.genre })
            .setAlbumTitle(song.album.ifBlank { station.name })
            .setArtworkUri(song.customArtworkPath?.let(Uri::parse))
            .build()
        return MediaItem.Builder()
            .setMediaId(station.url)
            .setUri(station.url)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun scheduleRadioRetry(message: String) {
        val station = _state.value.radioStation ?: return
        if (_state.value.currentSong?.isStream != true || radioRetryJob?.isActive == true) return
        radioRetryJob = scope.launch {
            while (isActive) {
                val delayMs = (RADIO_RETRY_BASE_MS shl radioRetryAttempt.coerceAtMost(6))
                    .coerceAtMost(RADIO_RETRY_MAX_MS)
                delay(delayMs)
                val c = controller ?: break
                val current = _state.value
                if (current.radioStation?.url != station.url ||
                    current.currentSong?.isStream != true ||
                    !c.playWhenReady
                ) break

                radioRetryAttempt++
                _state.update { it.copy(isBuffering = true, streamError = message) }
                val song = current.currentSong ?: break
                c.setMediaItem(radioMediaItem(station, song))
                c.prepare()
                c.play()

                val ready = withTimeoutOrNull(RADIO_RETRY_WAIT_MS) {
                    while (isActive) {
                        if (c.playbackState == Player.STATE_READY && c.isPlaying) return@withTimeoutOrNull true
                        delay(250)
                    }
                    false
                } ?: false
                if (ready) {
                    radioRetryAttempt = 0
                    _state.update { it.copy(isBuffering = false, streamError = null) }
                    break
                }
            }
            radioRetryJob = null
        }
    }

    private fun startSomaMetadataUpdates(station: RadioStation) {
        val repository = radioRepository ?: return
        radioMetadataJob?.cancel()
        radioMetadataJob = scope.launch {
            var lastTrackKey: String? = null
            while (isActive && _state.value.isPlaying &&
                _state.value.radioStation?.sourceId == station.sourceId
            ) {
                try {
                    val history = withContext(Dispatchers.IO) {
                        repository.nowPlaying(station.sourceId.orEmpty())
                    }
                    if (history.isNotEmpty()) {
                        _state.update { it.copy(radioTrackHistory = history) }
                        val current = history.first()
                        val trackKey = "${current.artist}\u0000${current.title}\u0000${current.album.orEmpty()}"
                        if (trackKey != lastTrackKey) {
                            lastTrackKey = trackKey
                            val art = current.artworkUrl
                                ?: repository.resolveArtwork(current.artist, current.title, current.album)
                            updateRadioTrack(current, art)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SomaFM now-playing refresh failed: ${e.message}")
                }
                delay(SOMAFM_METADATA_INTERVAL_MS)
            }
        }
    }

    private fun updateRadioTrack(track: SomaFmTrack, artwork: String?) {
        val current = _state.value.currentSong ?: return
        val updated = current.copy(
            title = track.title,
            artist = track.artist,
            album = track.album ?: current.album,
            customArtworkPath = artwork
        )
        _state.update {
            it.copy(
                currentSong = updated,
                queue = listOf(updated),
                streamMetadataTitle = track.title,
                radioTrackArtworkUrl = artwork,
                isBuffering = controller?.playbackState == Player.STATE_BUFFERING,
                streamError = null
            )
        }
    }

    private fun applyIcyTitle(rawTitle: String) {
        val title = rawTitle.trim()
        if (title.isBlank()) return
        val parts = title.split(" - ", limit = 2)
        val trackKey = if (parts.size == 2) {
            "${parts[0].trim().lowercase()}\u0000${parts[1].trim().lowercase()}"
        } else {
            title.lowercase()
        }
        if (trackKey == lastRadioTrackKey) return
        lastRadioTrackKey = trackKey
        _state.update { state ->
            val song = state.currentSong ?: return@update state.copy(streamMetadataTitle = title)
            val updatedSong = if (song.isStream && parts.size == 2) {
                song.copy(artist = parts[0].trim(), title = parts[1].trim(), customArtworkPath = null)
            } else if (song.isStream) {
                song.copy(title = title, customArtworkPath = null)
            } else song
            state.copy(
                currentSong = updatedSong,
                queue = if (updatedSong.isStream) listOf(updatedSong) else state.queue,
                streamMetadataTitle = if (parts.size == 2) parts[1].trim() else title,
                radioTrackArtworkUrl = null,
                streamError = null
            )
        }
        if (parts.size == 2) {
            val artist = parts[0].trim()
            val trackTitle = parts[1].trim()
            val repository = radioRepository ?: return
            radioArtworkJob?.cancel()
            radioArtworkJob = scope.launch {
                val artwork = withContext(Dispatchers.IO) {
                    repository.resolveArtwork(artist, trackTitle, null)
                }
                val state = _state.value
                val song = state.currentSong
                if (artwork != null && song?.isStream == true &&
                    song.artist.equals(artist, ignoreCase = true) &&
                    song.title.equals(trackTitle, ignoreCase = true)
                ) {
                    val updated = song.copy(customArtworkPath = artwork)
                    _state.update {
                        it.copy(
                            currentSong = updated,
                            queue = listOf(updated),
                            radioTrackArtworkUrl = artwork
                        )
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            c.play()
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipToNext() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
        } else if (_state.value.repeatMode == RepeatMode.ALL && _state.value.queue.isNotEmpty()) {
            c.seekTo(0, 0L)
        }
    }

    fun skipToPrevious(restartThresholdMs: Long = 3000L) {
        val c = controller ?: return
        if (c.currentPosition > restartThresholdMs) {
            c.seekTo(0L)
        } else if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
        } else {
            c.seekTo(0L)
        }
    }

    fun startSeeking(forward: Boolean, stepMs: Long = 1500L) {
        stopSeeking()
        seekJob = scope.launch {
            while (isActive) {
                val c = controller ?: break
                val next = if (forward) {
                    (c.currentPosition + stepMs).coerceAtMost(c.duration.coerceAtLeast(0L))
                } else {
                    (c.currentPosition - stepMs).coerceAtLeast(0L)
                }
                c.seekTo(next)
                _state.update { it.copy(currentPositionMs = next) }
                delay(220)
            }
        }
    }

    fun stopSeeking() {
        seekJob?.cancel()
        seekJob = null
    }

    fun setPlayerVolume(volume: Float) {
        val c = controller ?: return
        c.volume = volume.coerceIn(0f, 1f)
    }

    fun restoreVolume() {
        setPlayerVolume(savedVolume)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val nextMode = !c.shuffleModeEnabled
        c.shuffleModeEnabled = nextMode
        _state.update { it.copy(shuffleMode = nextMode) }
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        val exoMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        c.repeatMode = exoMode
        _state.update { it.copy(repeatMode = next) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val c = controller ?: return
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        c.playbackParameters = PlaybackParameters(safeSpeed)
        _state.update { it.copy(playbackSpeed = safeSpeed) }
    }

    fun jumpToQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
        }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
            val updatedQueue = _state.value.queue.toMutableList()
            if (index in updatedQueue.indices) {
                updatedQueue.removeAt(index)
                _state.update { it.copy(queue = updatedQueue) }
            }
        }
    }

    fun release() {
        stopSeeking()
        stopProgressTracker()
        radioMetadataJob?.cancel()
        radioMetadataJob = null
        radioArtworkJob?.cancel()
        radioRetryJob?.cancel()
        scope.cancel()
        sleepTimer.cancelTimer()
        controller?.release()
        controller = null
    }

    companion object {
        private const val TAG = "PlaybackController"
        private const val SOMAFM_METADATA_INTERVAL_MS = 30_000L
        private const val RADIO_RETRY_BASE_MS = 1_000L
        private const val RADIO_RETRY_MAX_MS = 60_000L
        private const val RADIO_RETRY_WAIT_MS = 20_000L
    }
}
