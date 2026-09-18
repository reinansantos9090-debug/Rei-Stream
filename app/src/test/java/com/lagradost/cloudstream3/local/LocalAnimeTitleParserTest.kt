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
}
