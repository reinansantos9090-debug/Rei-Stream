package com.lagradost.cloudstream3.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/** A video known to Android's MediaStore. The URI is used instead of an absolute file path. */
data class LocalAnimeVideo(
    val contentUri: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val modifiedAtSeconds: Long,
    val parsed: ParsedLocalEpisode,
)

data class LocalAnimeSeries(
    val key: String,
    val title: String,
    val episodes: List<LocalAnimeVideo>,
)

/**
 * Reads only the device video index. It never uploads files, thumbnails, or filenames.
 * Callers must request the platform's video-library permission before calling [scan].
 */
class LocalAnimeCatalog(private val context: Context) {
    fun scan(): List<LocalAnimeSeries> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        val videos = buildList {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Video.Media.SIZE} > 0",
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val modified = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(name) ?: continue
                    add(
                        LocalAnimeVideo(
                            contentUri = ContentUris.withAppendedId(collection, cursor.getLong(id)).toString(),
                            displayName = displayName,
                            sizeBytes = cursor.getLong(size),
                            durationMs = cursor.getLong(duration),
                            modifiedAtSeconds = cursor.getLong(modified),
                            parsed = LocalAnimeTitleParser.parse(displayName),
                        )
                    )
                }
            }
        }
        return videos.groupBy { LocalAnimeTitleParser.seriesKey(it.parsed.seriesTitle) }
            .map { (key, episodes) ->
                LocalAnimeSeries(key, episodes.first().parsed.seriesTitle, episodes)
            }
            .sortedBy { it.title.lowercase() }
    }
}
