package com.lagradost.cloudstream3.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans SAF folders and enriches new series incrementally; no scan runs when the home screen opens. */
class LocalLibrarySync(context: Context) {
    private val library = LocalLibraryRepository(context)
    private val metadata = AniListMetadataRepository(context)

    suspend fun refresh(forceMetadata: Boolean = false): EnrichedLocalLibrary = withContext(Dispatchers.IO) {
        val result = library.scan()
        val entries = result.series.map { series ->
            EnrichedLocalAnime(series, metadata.metadataFor(series.title, forceMetadata))
        }
        EnrichedLocalLibrary(entries, result.duplicates, result.unrecognized)
    }
}

data class EnrichedLocalLibrary(val series: List<EnrichedLocalAnime>, val duplicates: List<LocalDuplicate>, val unrecognized: List<String>)
data class EnrichedLocalAnime(val local: LocalAnimeSeries, val metadata: LocalAnimeMetadata?)
