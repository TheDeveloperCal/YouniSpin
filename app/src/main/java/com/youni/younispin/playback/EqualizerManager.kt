package com.youni.younispin.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMilliB: Short,
    val maxLevelMilliB: Short,
    var currentLevelMilliB: Short
)

class EqualizerManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("younispin_eq", Context.MODE_PRIVATE)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    var isEnabled: Boolean
        get() = prefs.getBoolean("eq_enabled", false)
        set(value) {
            prefs.edit().putBoolean("eq_enabled", value).apply()
            applyState()
        }

    var bassBoostStrength: Short
        get() = prefs.getInt("bass_strength", 0).toShort()
        set(value) {
            prefs.edit().putInt("bass_strength", value.toInt()).apply()
            try {
                if (bassBoost?.strengthSupported == true) {
                    bassBoost?.setStrength(value)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed setting bass boost", e)
            }
        }

    var virtualizerStrength: Short
        get() = prefs.getInt("virt_strength", 0).toShort()
        set(value) {
            prefs.edit().putInt("virt_strength", value.toInt()).apply()
            try {
                if (virtualizer?.strengthSupported == true) {
                    virtualizer?.setStrength(value)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed setting virtualizer", e)
            }
        }

    var currentPresetIndex: Short
        get() = prefs.getInt("eq_preset", -1).toShort()
        set(value) {
            prefs.edit().putInt("eq_preset", value.toInt()).apply()
            try {
                if (value >= 0 && equalizer != null && value < (equalizer?.numberOfPresets ?: 0)) {
                    equalizer?.usePreset(value)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed setting preset", e)
            }
        }

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = this@EqualizerManager.isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = this@EqualizerManager.isEnabled
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = this@EqualizerManager.isEnabled
            }

            applyState()
            Log.d(TAG, "Audio effects attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing equalizer for session $audioSessionId", e)
        }
    }

    private fun applyState() {
        val enabled = isEnabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled

            if (enabled) {
                val preset = currentPresetIndex
                if (preset >= 0 && preset < (equalizer?.numberOfPresets ?: 0)) {
                    equalizer?.usePreset(preset)
                }
                bassBoost?.setStrength(bassBoostStrength)
                virtualizer?.setStrength(virtualizerStrength)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying EQ state", e)
        }
    }

    fun getBands(): List<EqBand> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands
        val min = eq.bandLevelRange[0]
        val max = eq.bandLevelRange[1]
        val list = mutableListOf<EqBand>()

        for (i in 0 until numBands) {
            val bandIndex = i.toShort()
            val freq = eq.getCenterFreq(bandIndex) / 1000 // Convert mHz to Hz
            val current = eq.getBandLevel(bandIndex)
            list.add(EqBand(bandIndex, freq, min, max, current))
        }
        return list
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            prefs.edit().putInt("band_$band", level.toInt()).apply()
            // Reset preset to custom
            prefs.edit().putInt("eq_preset", -1).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting band level", e)
        }
    }

    fun getPresetNames(): List<String> {
        val eq = equalizer ?: return listOf("Flat", "Rock", "Pop", "Jazz", "Classical")
        val numPresets = eq.numberOfPresets
        val list = mutableListOf<String>()
        for (i in 0 until numPresets) {
            list.add(eq.getPresetName(i.toShort()))
        }
        return if (list.isNotEmpty()) list else listOf("Flat", "Rock", "Pop", "Jazz", "Classical")
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
    }

    companion object {
        private const val TAG = "EqualizerManager"
    }
}
