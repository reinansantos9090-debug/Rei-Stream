package com.lagradost.cloudstream3.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** Owns user-selected SAF trees. No broad storage permission or file path is required. */
class LocalLibraryRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("rei_stream_local_library", Context.MODE_PRIVATE)
    val folders: Set<String> get() = prefs.getStringSet(FOLDERS, emptySet()).orEmpty()

    fun addFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit().putStringSet(FOLDERS, folders + uri.toString()).apply()
    }
    fun removeFolder(uri: Uri) { prefs.edit().putStringSet(FOLDERS, folders - uri.toString()).apply() }

    fun scan(): LocalScanResult {
        val videos = mutableListOf<LocalAnimeVideo>(); val unreadable = mutableListOf<String>()
        folders.forEach { saved ->
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(saved)) ?: return@forEach
            scanTree(tree, videos, unreadable)
        }
        return LocalLibraryIndexer.index(videos, unreadable)
    }
    private fun scanTree(node: DocumentFile, videos: MutableList<LocalAnimeVideo>, unreadable: MutableList<String>) {
        node.listFiles().forEach { file -> when {
            file.isDirectory -> scanTree(file, videos, unreadable)
            isVideo(file) && file.canRead() -> {
                val name = file.name ?: return@forEach
                videos += LocalAnimeVideo(file.uri.toString(), name, file.length(), 0, file.lastModified() / 1000, LocalAnimeTitleParser.parse(name))
            }
            isVideo(file) -> unreadable += (file.name ?: file.uri.toString())
        } }
    }
    private fun isVideo(file: DocumentFile): Boolean =
        file.type?.startsWith("video/") == true || file.name?.substringAfterLast('.', "")?.lowercase() in VIDEO_EXTENSIONS
    private companion object {
        const val FOLDERS = "folders"
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts")
    }
}

data class LocalScanResult(val series: List<LocalAnimeSeries>, val duplicates: List<LocalDuplicate>, val unrecognized: List<String>)
data class LocalDuplicate(val seriesKey: String, val season: Int?, val episode: Int?, val files: List<LocalAnimeVideo>)
object LocalLibraryIndexer {
    fun index(videos: List<LocalAnimeVideo>, unreadable: List<String> = emptyList()): LocalScanResult {
        val recognized = videos.filter { it.parsed.seriesTitle.isNotBlank() }
        val series = recognized.groupBy { LocalAnimeTitleParser.seriesKey(it.parsed.seriesTitle) }.map { (key, files) ->
            LocalAnimeSeries(key, files.first().parsed.seriesTitle, files.sortedWith(compareBy({ it.parsed.season ?: 1 }, { it.parsed.episode ?: Int.MAX_VALUE }, { it.displayName })))
        }.sortedBy { it.title.lowercase() }
        val duplicates = recognized.filter { it.parsed.episode != null }.groupBy { Triple(LocalAnimeTitleParser.seriesKey(it.parsed.seriesTitle), it.parsed.season, it.parsed.episode) }
            .filterValues { it.size > 1 }.map { (key, files) -> LocalDuplicate(key.first, key.second, key.third, files) }
        return LocalScanResult(series, duplicates, unreadable + videos.filter { it.parsed.seriesTitle.isBlank() }.map { it.displayName })
    }
}
