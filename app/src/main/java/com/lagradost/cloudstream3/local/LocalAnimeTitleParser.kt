package com.lagradost.cloudstream3.local

import java.util.Locale

/** Local-only filename parser. It deliberately makes no network or AI request. */
object LocalAnimeTitleParser {
    private val extension = Regex("\\.[A-Za-z0-9]{2,5}$")
    private val brackets = Regex("\\[[^]]*]|\\([^)]*\\)")
    private val technical = Regex("""(?i)\\b(?:480p|720p|1080p|1440p|2160p|4k|8k|web[ ._-]?(?:dl|rip)|blu[ .-]?ray|b[dr]rip|x26[45]|h[ .-]?26[45]|hevc|av1|aac|dts|flac|10bit|hdr(?:10)?|multi|dual[ .-]?audio)\\b""")
    private val seasonEpisode = Regex("""(?i)(?:^|[ ._-])s(\\d{1,2})e(\\d{1,4})(?:v\\d+)?$""")
    private val episode = Regex("""(?i)(?:^|[ ._-])(?:ep(?:isode)?|e)[ ._-]?(\\d{1,4})(?:v\\d+)?$|(?:^|[ ._-])(\\d{1,4})(?:v\\d+)?$""")
    private val special = Regex("""(?i)(?:^|[ ._-])(ova|ona|special|sp|movie|film)(?:[ ._-]?(\\d+))?$""")

    fun parse(displayName: String): ParsedLocalEpisode {
        val raw = displayName.replace(extension, "")
        val clean = raw.replace(brackets, " ").replace(technical, " ")
            .replace('_', ' ').replace('.', ' ').replace(Regex("\\s+"), " ").trim()
        val specialMatch = special.find(clean)
        val se = seasonEpisode.find(clean)
        val ep = if (se == null && specialMatch == null) episode.find(clean) else null
        val suffix = specialMatch ?: se ?: ep
        val title = suffix?.let { clean.removeRange(it.range) }?.trim(' ', '-', '_') ?: clean
        val number = se?.groupValues?.get(2)?.toIntOrNull()
            ?: ep?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.toIntOrNull()
            ?: specialMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
        return ParsedLocalEpisode(
            seriesTitle = title.ifBlank { raw.trim() },
            episodeLabel = suffix?.value?.trim(),
            season = se?.groupValues?.get(1)?.toIntOrNull(),
            episode = number,
            kind = specialMatch?.groupValues?.get(1)?.lowercase(Locale.ROOT)?.let(LocalMediaKind::fromToken)
                ?: LocalMediaKind.EPISODE,
        )
    }

    fun seriesKey(title: String): String = title.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ").trim()
}

enum class LocalMediaKind { EPISODE, MOVIE, OVA, ONA, SPECIAL;
    companion object { fun fromToken(value: String) = when (value) { "movie", "film" -> MOVIE; "ova" -> OVA; "ona" -> ONA; else -> SPECIAL } }
}
data class ParsedLocalEpisode(val seriesTitle: String, val episodeLabel: String?, val season: Int? = null, val episode: Int? = null, val kind: LocalMediaKind = LocalMediaKind.EPISODE)
