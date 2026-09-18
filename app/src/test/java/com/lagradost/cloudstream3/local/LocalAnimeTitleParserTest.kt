package com.lagradost.cloudstream3.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalAnimeTitleParserTest {
    @Test
    fun `removes release details and extracts episode`() {
        val parsed = LocalAnimeTitleParser.parse("[SubsPlease] Frieren - 01 (1080p) [ABCD1234].mkv")

        assertEquals("Frieren", parsed.seriesTitle)
        assertEquals("- 01", parsed.episodeLabel)
    }

    @Test
    fun `keeps a title when no episode convention is present`() {
        val parsed = LocalAnimeTitleParser.parse("A Silent Voice.2016.1080p.BluRay.x265.mkv")

        assertEquals("A Silent Voice 2016", parsed.seriesTitle)
        assertNull(parsed.episodeLabel)
    }

    @Test
    fun `uses the same key for punctuation variants`() {
        assertEquals(
            LocalAnimeTitleParser.seriesKey("My Hero Academia"),
            LocalAnimeTitleParser.seriesKey("My.Hero_Academia"),
        )
    }
    @Test
    fun `detects season episode and technical tags`() {
        val parsed = LocalAnimeTitleParser.parse("Anime.Name.S02E003.1080p.WEB-DL.x265.mkv")
        assertEquals("Anime Name", parsed.seriesTitle)
        assertEquals(2, parsed.season)
        assertEquals(3, parsed.episode)
    }

    @Test
    fun `classifies specials without forcing them into episodes`() {
        val parsed = LocalAnimeTitleParser.parse("One Piece - OVA 2 [1080p].mkv")
        assertEquals(LocalMediaKind.OVA, parsed.kind)
        assertEquals(2, parsed.episode)
    }
}
