package com.youni.younispin.data

import com.youni.younispin.data.model.Song
import java.text.Normalizer

object LibrarySearch {
    fun matches(song: Song, rawQuery: String): Boolean {
        val query = normalize(rawQuery)
        if (query.isBlank()) return true
        val haystack = normalize(
            listOf(
                song.displayTitle,
                song.displayArtist,
                song.displayAlbum,
                song.albumArtist.orEmpty(),
                song.genre.orEmpty(),
                song.composer.orEmpty(),
                song.path.substringAfterLast('/'),
                song.path
            ).joinToString("\u0000")
        )
        return query.split(' ').filter { it.isNotBlank() }.all { token -> haystack.contains(token) }
    }

    fun filter(songs: List<Song>, rawQuery: String): List<Song> {
        if (rawQuery.isBlank()) return songs
        return songs.filter { matches(it, rawQuery) }
    }

    fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        return decomposed.replace("\\p{Mn}+".toRegex(), "")
    }
}
