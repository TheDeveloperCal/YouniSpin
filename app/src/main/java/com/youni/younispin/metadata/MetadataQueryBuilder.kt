package com.youni.younispin.metadata

data class TrackSearchQuery(
    val title: String,
    val artist: String?,
    val album: String?,
    val source: String
)

object MetadataQueryBuilder {
    private val unknownTitles = setOf("", "unknown track", "<unknown>", "unknown")
    private val unknownArtists = setOf("", "unknown artist", "<unknown>", "unknown")
    private val unknownAlbums = setOf("", "unknown album", "<unknown>", "unknown")

    fun fromSongFields(
        title: String,
        artist: String?,
        album: String?,
        filePath: String?
    ): TrackSearchQuery {
        val filename = filePath
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        var queryTitle = usableTitle(title)
        var queryArtist = usableArtist(artist)
        var queryAlbum = usableAlbum(album)
        var source = "tagged-fields"

        val combined = queryTitle ?: filename
        val parsedTitle = parseArtistTitle(combined)
        val parsedFile = parseArtistTitle(filename)
        if (queryArtist == null && parsedTitle != null) {
            queryArtist = parsedTitle.first
            queryTitle = parsedTitle.second
            source = "split-title"
        } else if (queryArtist == null && parsedFile != null) {
            queryArtist = parsedFile.first
            if (queryTitle == null) queryTitle = parsedFile.second
            source = "split-filename"
        } else if (queryTitle == null && filename.isNotBlank()) {
            queryTitle = filename
            source = "filename"
        }

        if (queryAlbum == null) {
            parseArtistAlbumTitle(combined)?.let { triple ->
                if (queryArtist == null) queryArtist = triple.first
                queryAlbum = triple.second
                queryTitle = triple.third
                source = "split-artist-album-title"
            }
        }

        return TrackSearchQuery(
            title = queryTitle.orEmpty(),
            artist = queryArtist,
            album = queryAlbum,
            source = source
        )
    }

    fun luceneQuery(query: TrackSearchQuery, quoted: Boolean, includeAlbum: Boolean): String {
        val title = sanitize(query.title)
        val builder = StringBuilder()
        builder.append("recording:").append(field(title, quoted))
        val artist = query.artist?.let(::sanitize)?.takeIf { it.isNotBlank() }
        if (artist != null) {
            builder.append(" AND artist:").append(field(artist, quoted))
        }
        val album = query.album?.let(::sanitize)?.takeIf { it.isNotBlank() }
        if (includeAlbum && album != null) {
            builder.append(" AND release:").append(field(album, quoted))
        }
        return builder.toString()
    }

    internal fun parseArtistTitle(value: String): Pair<String, String>? {
        val parts = value.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
        return if (parts.size == 2) parts[0] to parts[1] else null
    }

    internal fun parseArtistAlbumTitle(value: String): Triple<String, String, String>? {
        val parts = value.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
        return if (parts.size >= 3) Triple(parts[0], parts[1], parts.drop(2).joinToString(" - ")) else null
    }

    private fun usableTitle(value: String): String? =
        value.trim().takeUnless { it.lowercase() in unknownTitles }

    private fun usableArtist(value: String?): String? =
        value?.trim()?.takeUnless { it.lowercase() in unknownArtists }

    private fun usableAlbum(value: String?): String? =
        value?.trim()?.takeUnless { it.lowercase() in unknownAlbums }

    private fun sanitize(value: String): String =
        value.replace(Regex("[^\\p{L}\\p{N} ]+"), " ").replace(Regex("\\s+"), " ").trim()

    private fun field(value: String, quoted: Boolean): String =
        if (quoted) "\"$value\"" else "($value)"
}
