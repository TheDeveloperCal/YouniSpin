package com.youni.younispin.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.youni.younispin.data.model.Song
import java.io.File

object MetadataReader {
    private const val TAG = "MetadataReader"

    data class DetailedMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val albumArtist: String?,
        val genre: String?,
        val year: Int?,
        val trackNumber: Int?,
        val discNumber: Int?,
        val composer: String?,
        val comment: String?,
        val durationMs: Long,
        val bitrate: Int,
        val sampleRate: Int,
        val mimeType: String?,
        val embeddedArtwork: ByteArray?,
        val lyrics: String?
    )

    fun readDetails(context: Context, uriString: String, filePath: String): DetailedMetadata {
        val retriever = MediaMetadataRetriever()
        var embeddedArt: ByteArray? = null
        var lyrics: String? = null

        try {
            if (filePath.isNotBlank() && File(filePath).exists()) {
                retriever.setDataSource(filePath)
                // Check if .lrc file exists alongside audio
                val lrcFile = File(filePath.substringBeforeLast('.') + ".lrc")
                if (lrcFile.exists() && lrcFile.canRead()) {
                    lyrics = lrcFile.readText()
                }
            } else {
                retriever.setDataSource(context, Uri.parse(uriString))
            }

            embeddedArt = retriever.embeddedPicture

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE) ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = dateStr?.take(4)?.toIntOrNull()

            val trackNumStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackNumStr?.split('/')?.firstOrNull()?.trim()?.toIntOrNull()

            val discNumStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            val discNumber = discNumStr?.split('/')?.firstOrNull()?.trim()?.toIntOrNull() ?: 1

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toIntOrNull() ?: 44100
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            return DetailedMetadata(
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                discNumber = discNumber,
                composer = composer,
                comment = null,
                durationMs = durationMs,
                bitrate = bitrate,
                sampleRate = sampleRate,
                mimeType = mimeType,
                embeddedArtwork = embeddedArt,
                lyrics = lyrics
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata: ${e.message}")
            return DetailedMetadata(
                title = null, artist = null, album = null, albumArtist = null,
                genre = null, year = null, trackNumber = null, discNumber = 1,
                composer = null, comment = null, durationMs = 0L, bitrate = 0,
                sampleRate = 44100, mimeType = null, embeddedArtwork = null, lyrics = null
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
