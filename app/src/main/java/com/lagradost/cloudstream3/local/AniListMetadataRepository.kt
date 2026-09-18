package com.lagradost.cloudstream3.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** Public AniList metadata client. It only submits one parsed title at a time, never file paths. */
class AniListMetadataRepository(private val context: Context) {
    private val cache = context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE)
    private val posterDirectory = File(context.cacheDir, "rei-stream-posters").apply { mkdirs() }

    fun cached(seriesKey: String): LocalAnimeMetadata? =
        cache.getString(seriesKey, null)?.let(::decode)

    /** Uses the local cache unless [forceRefresh] is explicitly requested by the user. */
    suspend fun metadataFor(title: String, forceRefresh: Boolean = false): LocalAnimeMetadata? = withContext(Dispatchers.IO) {
        val key = LocalAnimeTitleParser.seriesKey(title)
        if (!forceRefresh) cached(key)?.let { cached ->
            if (cached.posterPath == null || File(cached.posterPath).exists()) return@withContext cached
            val restored = cached.copy(posterPath = cached.posterUrl?.let { downloadPoster(cached.id, it) })
            cache.edit().putString(key, encode(restored)).apply()
            return@withContext restored
        }
        val metadata = runCatching { requestMetadata(title) }.getOrNull() ?: return@withContext null
        val poster = metadata.posterUrl?.let { downloadPoster(metadata.id, it) }
        val cachedMetadata = metadata.copy(posterPath = poster)
        cache.edit().putString(key, encode(cachedMetadata)).apply()
        cachedMetadata
    }

    private fun requestMetadata(title: String): LocalAnimeMetadata? {
        val query = """query (${'$'}search: String) { Media(search: ${'$'}search, type: ANIME) { id title { romaji english native } description(asHtml: false) genres seasonYear status episodes coverImage { large } bannerImage } }"""
        val body = JSONObject().put("query", query).put("variables", JSONObject().put("search", title)).toString()
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val media = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                .optJSONObject("data")?.optJSONObject("Media") ?: return null
            val titles = media.getJSONObject("title")
            LocalAnimeMetadata(
                id = media.getInt("id"),
                title = titles.optString("english").ifBlank { titles.optString("romaji") },
                alternateTitles = listOf(titles.optString("romaji"), titles.optString("native")).filter { it.isNotBlank() }.distinct(),
                description = media.optString("description").takeIf { it.isNotBlank() },
                genres = media.optJSONArray("genres")?.let { array -> List(array.length()) { array.optString(it) } }.orEmpty(),
                year = media.optInt("seasonYear").takeIf { it != 0 }, status = media.optString("status").takeIf { it.isNotBlank() },
                totalEpisodes = media.optInt("episodes").takeIf { it != 0 },
                posterUrl = media.optJSONObject("coverImage")?.optString("large")?.takeIf { it.isNotBlank() },
                bannerUrl = media.optString("bannerImage").takeIf { it.isNotBlank() },
            )
        } finally { connection.disconnect() }
    }

    private fun downloadPoster(id: Int, url: String): String? = runCatching {
        val target = File(posterDirectory, "$id.jpg")
        if (!target.exists()) URL(url).openStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        target.absolutePath
    }.getOrNull()

    private fun encode(value: LocalAnimeMetadata) = JSONObject().apply {
        put("id", value.id); put("title", value.title); put("alternateTitles", value.alternateTitles); put("description", value.description)
        put("genres", value.genres); put("year", value.year); put("status", value.status); put("totalEpisodes", value.totalEpisodes)
        put("posterUrl", value.posterUrl); put("bannerUrl", value.bannerUrl); put("posterPath", value.posterPath)
    }.toString()
    private fun decode(raw: String) = runCatching { JSONObject(raw).let { json ->
        LocalAnimeMetadata(json.getInt("id"), json.getString("title"), json.optJSONArray("alternateTitles")?.let { a -> List(a.length()) { a.optString(it) } }.orEmpty(),
            json.optString("description").takeIf { it.isNotBlank() }, json.optJSONArray("genres")?.let { a -> List(a.length()) { a.optString(it) } }.orEmpty(),
            json.optInt("year").takeIf { it != 0 }, json.optString("status").takeIf { it.isNotBlank() }, json.optInt("totalEpisodes").takeIf { it != 0 },
            json.optString("posterUrl").takeIf { it.isNotBlank() }, json.optString("bannerUrl").takeIf { it.isNotBlank() }, json.optString("posterPath").takeIf { it.isNotBlank() })
    } }.getOrNull()

    private companion object { const val CACHE_NAME = "rei_stream_anilist_metadata"; const val ENDPOINT = "https://graphql.anilist.co" }
}

data class LocalAnimeMetadata(
    val id: Int,
    val title: String,
    val alternateTitles: List<String>,
    val description: String?,
    val genres: List<String>,
    val year: Int?,
    val status: String?,
    val totalEpisodes: Int?,
    val posterUrl: String?,
    val bannerUrl: String?,
    // Metadata is created before its poster download completes or if the download fails.
    val posterPath: String? = null,
)
