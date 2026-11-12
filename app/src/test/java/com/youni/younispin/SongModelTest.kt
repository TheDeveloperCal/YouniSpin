package com.youni.younispin

import com.youni.younispin.data.model.Song
import com.youni.younispin.metadata.MatchConfidence
import com.youni.younispin.metadata.MetadataMatch
import com.youni.younispin.metadata.MetadataMatchResolver
import com.youni.younispin.playback.PlaybackState
import com.youni.younispin.playback.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongModelTest {

    @Test
    fun testFormattedDuration() {
        val song1 = Song(id = 1, title = "Test", artist = "Artist", album = "Album", duration = 65000L)
        assertEquals("01:05", song1.formattedDuration)

        val song2 = Song(id = 2, title = "Test2", artist = "Artist", album = "Album", duration = 3665000L)
        assertEquals("1:01:05", song2.formattedDuration)

        val songZero = Song(id = 3, title = "Zero", artist = "Artist", album = "Album", duration = 0L)
        assertEquals("--:--", songZero.formattedDuration)
    }

    @Test
    fun testDisplayNamesFallback() {
        val blankArtist = Song(id = 1, title = "My Song", artist = "", album = "<unknown>", path = "/sdcard/Music/CoolSong.mp3")
        assertEquals("Unknown Artist", blankArtist.displayArtist)
        assertEquals("Unknown Album", blankArtist.displayAlbum)
        assertEquals("My Song", blankArtist.displayTitle)

        val blankTitle = Song(id = 2, title = "", artist = "Band", album = "Album", path = "/sdcard/Music/CoolSong.mp3")
        assertEquals("CoolSong", blankTitle.displayTitle)
    }

    @Test
    fun testPlaybackStateProgressFraction() {
        val state = PlaybackState(
            currentPositionMs = 30000L,
            durationMs = 60000L,
            isPlaying = true,
            repeatMode = RepeatMode.ALL
        )
        assertEquals(0.5f, state.progressFraction, 0.001f)
        assertEquals(RepeatMode.ALL, state.repeatMode)
        assertTrue(state.isPlaying)
    }

    @Test
    fun testMetadataResolverPrefersStrongMatch() {
        val candidates = listOf(
            MetadataMatch(
                id = "weak",
                score = 60,
                title = "A Different Song",
                artist = "Another Artist",
                album = "Other Album",
                releaseId = null,
                date = null,
                trackNumber = null,
                coverArtUrl = null,
                confidence = MatchConfidence.LOW_MATCH
            ),
            MetadataMatch(
                id = "strong",
                score = 95,
                title = "Midnight Drive",
                artist = "Solar Echo",
                album = "Afterglow",
                releaseId = null,
                date = null,
                trackNumber = 3,
                coverArtUrl = null,
                confidence = MatchConfidence.STRONG_MATCH
            )
        )

        val chosen = MetadataMatchResolver.chooseBestMatch(
            title = "Midnight Drive",
            artist = "Solar Echo",
            album = "Afterglow",
            durationMs = 240000L,
            candidates = candidates
        )

        assertEquals("strong", chosen?.id)
        assertTrue(MetadataMatchResolver.canAutoApply("Midnight Drive", "Solar Echo", "Afterglow", chosen!!))
    }

    @Test
    fun testMetadataResolverRejectsUnreliableMatch() {
        val candidates = listOf(
            MetadataMatch(
                id = "guess",
                score = 35,
                title = "Wrong Song",
                artist = "Band X",
                album = "Unrelated",
                releaseId = null,
                date = null,
                trackNumber = null,
                coverArtUrl = null,
                confidence = MatchConfidence.LOW_MATCH
            )
        )

        val chosen = MetadataMatchResolver.chooseBestMatch(
            title = "Midnight Drive",
            artist = "Solar Echo",
            album = "Afterglow",
            durationMs = 240000L,
            candidates = candidates
        )

        assertNull(chosen)
    }

    @Test
    fun testMetadataResolverDoesNotAutoApplyAmbiguousMatch() {
        val ambiguous = MetadataMatch(
            id = "ambiguous",
            score = 100,
            title = "Digital Love",
            artist = "Daft Punk",
            album = null,
            releaseId = null,
            date = null,
            trackNumber = null,
            coverArtUrl = null,
            confidence = MatchConfidence.MULTIPLE_MATCHES
        )

        assertFalse(
            MetadataMatchResolver.canAutoApply(
                songTitle = "Digital Love",
                songArtist = "Daft Punk",
                songAlbum = "",
                match = ambiguous
            )
        )
    }
}
