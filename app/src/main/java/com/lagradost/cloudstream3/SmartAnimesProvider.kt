package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class SmartAnimesProvider : MainAPI() {
    override var mainUrl = "https://smartanimes.net"
    override var name = "SmartAnimes"
    override var lang = "pt-br"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime)

    override async fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("article, div.poster, div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3, h2, .title, a")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override async fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1, .entry-title")?.text() ?: "SmartAnimes"
        val poster = document.selectFirst("div.poster img, img.wp-post-image")?.attr("src")
        val description = document.selectFirst("div.description, div.entry-content")?.text()

        val episodes = document.select("ul.episodes li, div.episodios div.item").mapNotNull {
            val epHref = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val epTitle = it.selectFirst("a, .title")?.text() ?: "Episódio"
            newEpisode(epHref) {
                this.name = epTitle
            }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override async fun loadLinks(
        data: String,
        isCdn: Boolean,
        handler: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframeUrl = document.selectFirst("iframe")?.attr("src")

        if (iframeUrl != null) {
            handler.invoke(
                ExtractorLink(
                    name = this.name,
                    source = this.name,
                    url = iframeUrl,
                    referer = mainUrl,
                    quality = Qualities.Unknown.value
                )
            )
            return true
        }
        return false
    }
}
