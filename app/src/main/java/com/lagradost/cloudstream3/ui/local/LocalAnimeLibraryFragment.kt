package com.lagradost.cloudstream3.ui.local

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.local.EnrichedLocalAnime
import com.lagradost.cloudstream3.local.LocalLibraryRepository
import com.lagradost.cloudstream3.local.LocalLibrarySync
import com.lagradost.cloudstream3.ui.player.OfflinePlaybackHelper.playUri
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import kotlinx.coroutines.launch

/** A deliberately local-only browser: every episode button opens its persisted content:// URI. */
class LocalAnimeLibraryFragment : Fragment(R.layout.fragment_local_anime_library) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLibrary(view)
    }

    private fun loadLibrary(view: View) {
        val content = view.findViewById<LinearLayout>(R.id.local_library_content)
        val progress = view.findViewById<ProgressBar>(R.id.local_library_progress)
        progress.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            if (LocalLibraryRepository(context).folders.isEmpty()) {
                progress.isVisible = false
                content.addView(message("Nenhuma pasta foi autorizada. Volte em Configurações › Biblioteca local e toque em “Adicionar pasta de animes”."))
                return@launch
            }
            val result = runCatching { LocalLibrarySync(context).refresh() }
            progress.isVisible = false
            result.onSuccess { library ->
                if (library.series.isEmpty()) content.addView(message("Nenhum vídeo foi encontrado nas pastas autorizadas."))
                library.series.forEach { content.addView(seriesView(it)) }
                if (library.unrecognized.isNotEmpty()) content.addView(message("${library.unrecognized.size} arquivo(s) não puderam ser identificados."))
            }.onFailure {
                content.addView(message("Não foi possível acessar a biblioteca. Verifique se a permissão da pasta ainda está ativa."))
            }
        }
    }

    private fun seriesView(anime: EnrichedLocalAnime): View = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 16, 0, 24)
        anime.metadata?.posterPath?.let { poster ->
            addView(ImageView(context).apply {
                val width = (100 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(width, (150 * resources.displayMetrics.density).toInt())
                contentDescription = anime.metadata.title
                scaleType = ImageView.ScaleType.CENTER_CROP
                loadImage(poster)
            })
        }
        addView(TextView(context).apply {
            textSize = 20f
            text = anime.metadata?.title ?: anime.local.title
        })
        anime.metadata?.genres?.takeIf { it.isNotEmpty() }?.let { genres -> addView(message(genres.joinToString(" • "))) }
        anime.local.episodes.forEach { episode ->
            addView(Button(context).apply {
                text = episode.parsed.episode?.let { "EP $it — ${episode.displayName}" } ?: episode.displayName
                setOnClickListener { playUri(requireActivity(), Uri.parse(episode.contentUri)) }
            })
        }
    }

    private fun message(text: String) = TextView(requireContext()).apply { this.text = text; textSize = 16f; setPadding(0, 8, 0, 8) }
}
