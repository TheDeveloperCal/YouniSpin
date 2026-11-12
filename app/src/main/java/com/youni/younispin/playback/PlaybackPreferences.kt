package com.youni.younispin.playback

import android.content.Context
import android.content.SharedPreferences

enum class AudioQualityMode(val title: String, val subtitle: String) {
    STANDARD("Standard", "Default mixer path with optional DSP"),
    HIGH("High Quality", "No silence skip; preserve source rate where the device allows"),
    DIRECT("Direct", "Disable EQ and loudness processing; closest to source samples")
}

class PlaybackPreferences(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var crossfadeEnabled: Boolean
        get() = prefs.getBoolean(KEY_CROSSFADE, false)
        set(value) { prefs.edit().putBoolean(KEY_CROSSFADE, value).apply() }

    var crossfadeSeconds: Int
        get() = prefs.getInt(KEY_CROSSFADE_SEC, 2).coerceIn(1, 12)
        set(value) { prefs.edit().putInt(KEY_CROSSFADE_SEC, value.coerceIn(1, 12)).apply() }

    var volumeNormEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_NORM, true)
        set(value) { prefs.edit().putBoolean(KEY_VOLUME_NORM, value).apply() }

    var skipSilence: Boolean
        get() = prefs.getBoolean(KEY_SKIP_SILENCE, false)
        set(value) { prefs.edit().putBoolean(KEY_SKIP_SILENCE, value).apply() }

    var qualityMode: AudioQualityMode
        get() {
            val name = prefs.getString(KEY_QUALITY, AudioQualityMode.HIGH.name)
            return AudioQualityMode.entries.find { it.name == name } ?: AudioQualityMode.HIGH
        }
        set(value) { prefs.edit().putString(KEY_QUALITY, value.name).apply() }

    var playbackSpeed: Float
        get() = prefs.getFloat(KEY_SPEED, 1f)
        set(value) { prefs.edit().putFloat(KEY_SPEED, value.coerceIn(0.5f, 2f)).apply() }

    var headsetAutoplay: Boolean
        get() = prefs.getBoolean(KEY_HEADSET_AUTOPLAY, false)
        set(value) { prefs.edit().putBoolean(KEY_HEADSET_AUTOPLAY, value).apply() }

    val dspAllowed: Boolean
        get() = qualityMode != AudioQualityMode.DIRECT

    val effectiveCrossfadeEnabled: Boolean
        get() = crossfadeEnabled && dspAllowed

    val effectiveVolumeNorm: Boolean
        get() = volumeNormEnabled && dspAllowed

    companion object {
        const val PREFS_NAME = "younispin_prefs"
        const val KEY_CROSSFADE = "crossfade_enabled"
        const val KEY_CROSSFADE_SEC = "crossfade_seconds"
        const val KEY_VOLUME_NORM = "volume_norm_enabled"
        const val KEY_SKIP_SILENCE = "skip_silence"
        const val KEY_QUALITY = "audio_quality_mode"
        const val KEY_SPEED = "playback_speed"
        const val KEY_HEADSET_AUTOPLAY = "headset_autoplay"
        const val KEY_QUEUE_URIS = "saved_queue_uris"
        const val KEY_QUEUE_INDEX = "saved_queue_index"
        const val KEY_QUEUE_POSITION = "saved_queue_position"
    }
}
