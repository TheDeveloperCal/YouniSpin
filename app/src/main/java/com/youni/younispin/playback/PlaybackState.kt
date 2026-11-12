package com.youni.younispin.playback

import com.youni.younispin.data.model.Song
import com.youni.younispin.data.model.RadioStation
import com.youni.younispin.data.repository.SomaFmTrack

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val playbackSpeed: Float = 1.0f,
    val streamMetadataTitle: String? = null,
    val radioStation: RadioStation? = null,
    val radioTrackHistory: List<SomaFmTrack> = emptyList(),
    val radioTrackArtworkUrl: String? = null,
    val isBuffering: Boolean = false,
    val streamError: String? = null,
    val audioSessionId: Int = 0,
    val audioQuality: AudioQualityInfo? = null,
    val isCrossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 0
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
