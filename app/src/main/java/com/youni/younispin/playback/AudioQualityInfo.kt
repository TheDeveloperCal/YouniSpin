package com.youni.younispin.playback

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.youni.younispin.data.model.Song
import java.io.File

data class AudioQualityInfo(
    val codec: String = "Unknown",
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channels: Int? = null,
    val bitrateKbps: Int? = null,
    val mimeType: String? = null,
    val sourcePath: String = ""
) {
    val sampleRateLabel: String
        get() = sampleRateHz?.let { "$it Hz" } ?: "N/A"

    val bitDepthLabel: String
        get() = bitDepth?.let { "$it-bit" } ?: "N/A"

    val channelLabel: String
        get() = when {
            channels == null -> "N/A"
            channels <= 0 -> "N/A"
            channels == 1 -> "Mono"
            channels == 2 -> "Stereo"
            else -> "$channels ch"
        }

    val bitrateLabel: String
        get() = bitrateKbps?.let { "$it kbps" } ?: "N/A"

    val summary: String
        get() = listOfNotNull(
            codec.ifBlank { null },
            sampleRateLabel,
            channelLabel
        ).joinToString(" • ")
}

object AudioQualityInspector {
    fun inspect(context: Context, song: Song): AudioQualityInfo {
        var codec = inferCodec(song.path, song.mimeType)
        var sampleRateHz: Int? = null
        var bitDepth: Int? = null
        var channels: Int? = null
        var bitrateKbps: Int? = null
        var mimeType: String? = song.mimeType.takeIf { it.isNotBlank() }

        val extractor = MediaExtractor()
        try {
            val sourcePath = song.path
            val uriString = song.uriString

            if (sourcePath.isNotBlank() && File(sourcePath).exists()) {
                extractor.setDataSource(sourcePath)
            } else if (uriString.isNotBlank()) {
                extractor.setDataSource(context, Uri.parse(uriString), null)
            }

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME)
                if (!trackMime.isNullOrBlank() && trackMime.startsWith("audio/")) {
                    mimeType = trackMime
                    codec = inferCodec(song.path, trackMime)
                    sampleRateHz = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        format.getInteger(MediaFormat.KEY_SAMPLE_RATE).takeIf { it > 0 }
                    } else null
                    channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).takeIf { it > 0 }
                    } else null
                    bitrateKbps = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        (format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000).takeIf { it > 0 }
                    } else null
                    bitDepth = runCatching {
                        if (format.containsKey("bits-per-sample")) {
                            format.getInteger("bits-per-sample").takeIf { it > 0 }
                        } else null
                    }.getOrNull()
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }

        return AudioQualityInfo(
            codec = codec,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            channels = channels,
            bitrateKbps = bitrateKbps,
            mimeType = mimeType,
            sourcePath = song.path
        )
    }

    private fun inferCodec(path: String, mimeType: String?): String {
        val lowerPath = path.lowercase()
        val lowerMime = mimeType?.lowercase() ?: ""

        return when {
            lowerPath.endsWith(".flac") || lowerMime.contains("flac") -> "FLAC"
            lowerPath.endsWith(".wav") || lowerPath.endsWith(".wave") || lowerMime.contains("wav") -> "WAV/PCM"
            lowerPath.endsWith(".mp3") || lowerMime.contains("mp3") -> "MP3"
            lowerPath.endsWith(".aac") || lowerPath.endsWith(".m4a") || lowerPath.endsWith(".mp4") || lowerMime.contains("aac") -> {
                if (lowerMime.contains("alac") || lowerPath.endsWith(".alac")) "ALAC" else "AAC"
            }
            lowerPath.endsWith(".opus") || lowerMime.contains("opus") -> "Opus"
            lowerPath.endsWith(".ogg") || lowerMime.contains("ogg") -> "Ogg Vorbis"
            lowerPath.endsWith(".aiff") || lowerPath.endsWith(".aif") -> "AIFF/PCM"
            else -> when {
                lowerMime.contains("flac") -> "FLAC"
                lowerMime.contains("wav") -> "WAV/PCM"
                lowerMime.contains("mp3") -> "MP3"
                lowerMime.contains("aac") -> "AAC"
                lowerMime.contains("opus") -> "Opus"
                lowerMime.contains("alac") -> "ALAC"
                lowerMime.contains("mpeg") -> "MP3"
                else -> "Unknown"
            }
        }
    }
}
