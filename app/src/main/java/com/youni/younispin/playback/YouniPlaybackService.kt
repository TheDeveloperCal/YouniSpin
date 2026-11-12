package com.youni.younispin.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.youni.younispin.MainActivity
import com.youni.younispin.R
import com.youni.younispin.visualizer.PcmTapRenderersFactory

@OptIn(UnstableApi::class)
class YouniPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var equalizerManager: EqualizerManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        equalizerManager = EqualizerManager(this)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this, PcmTapRenderersFactory(this))
            .setAudioAttributes(audioAttributes, true) // handle audio focus automatically
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    equalizerManager.attachAudioSession(player.audioSessionId)
                    publishAudioSession()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizerManager.attachAudioSession(audioSessionId)
                publishAudioSession()
            }

            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    if (entry is IcyInfo) {
                        val title = entry.title?.trim().orEmpty()
                        if (title.isNotBlank()) {
                            Log.d(TAG, "ICY stream title update: $title")
                            updateStreamMetadata(title)
                        }
                    }
                }
            }
        })

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
        publishAudioSession()
        refreshNowPlayingNotification()
    }

    private fun refreshNowPlayingNotification() {
        val session = mediaSession ?: return
        val title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: getString(R.string.app_name)
        val subtitle = player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Playing"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(MediaStyle().setMediaSession(session.sessionCompatToken))
            .build()

        startForeground(1001, notification)
    }

    private fun publishAudioSession() {
        val extras = Bundle().apply {
            putInt(EXTRA_AUDIO_SESSION, player.audioSessionId)
        }
        mediaSession?.setSessionExtras(extras)
    }

    private fun updateStreamMetadata(rawTitle: String) {
        val index = player.currentMediaItemIndex
        val current = player.currentMediaItem ?: return
        if (index < 0 || current.mediaMetadata.title?.toString() == rawTitle) return
        val parts = rawTitle.split(" - ", limit = 2)
        val metadata = current.mediaMetadata.buildUpon()
            .setTitle(if (parts.size == 2) parts[1].trim() else rawTitle)
            .apply { if (parts.size == 2) setArtist(parts[0].trim()) }
            .build()
        val updated = current.buildUpon().setMediaMetadata(metadata).build()
        val position = player.currentPosition.coerceAtLeast(0L)
        player.replaceMediaItem(index, updated)
        player.seekTo(index, position)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playback_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        equalizerManager.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "younispin_playback_channel"
        const val EXTRA_AUDIO_SESSION = "audio_session_id"
        private const val TAG = "YouniPlaybackService"
    }
}
