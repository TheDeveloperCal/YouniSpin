package com.youni.younispin.playback

import java.io.File
import java.nio.charset.Charset
import kotlin.math.pow

object ReplayGain {
    private val gainRegex = Regex(
        """replaygain_track_gain[^0-9+\-]*([+\-]?\d+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns a linear volume multiplier in 0.25..1.0.
     * Missing tags fall back to a conservative −1.5 dB headroom when normalization is on.
     */
    fun volumeMultiplier(filePath: String?, enabled: Boolean, directMode: Boolean): Float {
        if (!enabled || directMode) return 1f
        val parsedDb = filePath?.let { readTrackGainDb(it) }
        val db = parsedDb ?: -1.5f
        val linear = 10.0.pow((db / 20.0)).toFloat()
        return linear.coerceIn(0.25f, 1.0f)
    }

    fun readTrackGainDb(path: String): Float? {
        val file = File(path)
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            file.inputStream().use { input ->
                val buffer = ByteArray(minOf(96 * 1024, file.length().toInt().coerceAtLeast(0)))
                val read = input.read(buffer)
                if (read <= 0) return null
                val text = String(buffer, 0, read, Charset.forName("ISO-8859-1"))
                gainRegex.find(text)?.groupValues?.getOrNull(1)?.toFloatOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }
}
