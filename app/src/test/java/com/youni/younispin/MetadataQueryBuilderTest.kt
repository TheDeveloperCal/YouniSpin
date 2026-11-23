package com.youni.younispin

import com.youni.younispin.metadata.MetadataQueryBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataQueryBuilderTest {
    @Test
    fun splitsArtistAndTitleFromDownloadedTrackFilenameWhenTagsAreMissing() {
        val query = MetadataQueryBuilder.fromSongFields(
            title = "Death Became Her",
            artist = "<unknown>",
            album = "Televised Tragedies",
            filePath = "/storage/emulated/0/Music/18 Carat Affair - Death Became Her.m4a"
        )

        assertEquals("Death Became Her", query.title)
        assertEquals("18 Carat Affair", query.artist)
        assertEquals("Televised Tragedies", query.album)
        assertEquals("split-filename", query.source)
    }

    @Test
    fun retriesWithoutAlbumHintAndKeepsArtistAndRecordingFieldsSeparate() {
        val query = MetadataQueryBuilder.fromSongFields(
            title = "Death Became Her",
            artist = "18 Carat Affair",
            album = "Televised Tragedies",
            filePath = null
        )

        assertEquals(
            "recording:\"Death Became Her\" AND artist:\"18 Carat Affair\"",
            MetadataQueryBuilder.luceneQuery(query, quoted = true, includeAlbum = false)
        )
    }
}
