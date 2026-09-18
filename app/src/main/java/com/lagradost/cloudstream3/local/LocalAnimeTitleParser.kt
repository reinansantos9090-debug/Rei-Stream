package com.lagradost.cloudstream3.local

import java.util.Locale

/**
 * Turns a video file name into a stable series key without sending the file name anywhere.
 *
 * This is deliberately conservative: unknown words stay in the title and metadata such as a
 * genre is never invented from a file name. A metadata provider can later enrich this result.
 */
object LocalAnimeTitleParser {
    private val extension = Regex("\\.[A-Za-z0-9]{2,5}$")
    private val releaseGroup = Regex("\\[[^]]*]|\\([^)]*\\)")
    private val episodeSuffix = Regex(
        """(?i)(?:[ ._\-]+)(?:s\\d{1,2}e\\d{1,3}|season[ ._-]?\\d+|ep(?:isode)?[ ._-]?\\d{1,4}|e\\d{1,4}|\\d{1,4}(?:v\\d+)?)$"""
    )
    private val technicalTokens = Regex(
        """(?i)\\b(?:480p|720p|1080p|1440p|2160p|4k|8k|web[ ._-]?dl|web[ ._-]?rip|bluray|b[dr]rip|x26[45]|hevc|av1|aac(?:[ ._-]?\d(?:\.\d)?)?|dts|flac|10bit|h264|h265)\\b"""
    )

    fun parse(displayName: String): ParsedLocalEpisode {
        val withoutExtension = displayName.replace(extension, "")
        val cleaned = withoutExtension
            .replace(releaseGroup, " ")
            .replace(technicalTokens, " ")
            .replace('_', ' ')
            .replace('.', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        val episodeMatch = episodeSuffix.find(cleaned)
        val title = episodeMatch?.let { cleaned.removeRange(it.range) }
            ?: cleaned
        val normalizedTitle = title.trim(' ', '-', '_')
        return ParsedLocalEpisode(
            seriesTitle = normalizedTitle.ifBlank { withoutExtension.trim() },
            episodeLabel = episodeMatch?.value?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    /** A locale-independent key intended only for grouping local files. */
    fun seriesKey(title: String): String = title
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

data class ParsedLocalEpisode(
    val seriesTitle: String,
    val episodeLabel: String?,
)
