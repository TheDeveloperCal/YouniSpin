package com.youni.younispin.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val duration: Long = 0L,
    val path: String = "",
    val uriString: String = "",
    val albumId: Long = 0L,
    val artistId: Long = 0L,
    val genre: String? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val mimeType: String = "audio/mpeg",
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val customArtworkPath: String? = null,
    val isFavorite: Boolean = false,
    val isStream: Boolean = false,
    val composer: String? = null,
    val comment: String? = null
) {
    val artworkUri: String
        get() = customArtworkPath ?: "content://media/external/audio/albumart/$albumId"

    val displayArtist: String
        get() = if (artist.isBlank() || artist == "<unknown>") "Unknown Artist" else artist

    val displayAlbum: String
        get() = if (album.isBlank() || album == "<unknown>") "Unknown Album" else album

    val displayTitle: String
        get() = if (title.isBlank()) {
            val fileName = path.substringAfterLast('/')
            if (fileName.isNotBlank()) fileName.substringBeforeLast('.') else "Unknown Track"
        } else title

    val formattedDuration: String
        get() {
            if (duration <= 0) return "--:--"
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int = 0,
    val customArtworkPath: String? = null
) {
    val artworkUri: String
        get() = customArtworkPath ?: "content://media/external/audio/albumart/$id"
}

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int = 1,
    val customArtworkPath: String? = null
)

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class RadioStation(
    val id: Long = 0L,
    val name: String,
    val url: String,
    val genre: String = "Various",
    val logoUrl: String? = null,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0L,
    val description: String? = null,
    val homepage: String? = null,
    val country: String? = null,
    val language: String? = null,
    val codec: String? = null,
    val bitrate: Int = 0,
    val source: String = "CUSTOM",
    val sourceId: String? = null,
    val isCustom: Boolean = true
)

data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int
)
