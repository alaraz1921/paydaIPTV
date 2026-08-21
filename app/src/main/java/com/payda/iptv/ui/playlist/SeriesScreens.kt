package com.payda.iptv.ui.playlist

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.payda.iptv.data.Episode
import com.payda.iptv.data.EpisodeProgress
import com.payda.iptv.data.Season
import com.payda.iptv.data.Series
import com.payda.iptv.data.SeriesCategory
import com.payda.iptv.data.SeriesDetail
import com.payda.iptv.ui.theme.PayDaBackground
import com.payda.iptv.ui.theme.PayDaButton
import com.payda.iptv.ui.theme.PayDaCard
import com.payda.iptv.ui.theme.PayDaError
import com.payda.iptv.ui.theme.PayDaPlaceholderLogo
import com.payda.iptv.ui.theme.PayDaSurface
import com.payda.iptv.ui.theme.PayDaTextField
import com.payda.iptv.ui.theme.PayDaTextPrimary
import com.payda.iptv.ui.theme.PayDaTextSecondary

internal const val SeriesAllCategory = "Todos"
internal const val SeriesFavoritesCategory = "Favoritos"

@Composable
fun SeriesCatalogScreen(
    series: List<Series>,
    categories: List<SeriesCategory>,
    favoriteIds: Set<String>,
    selectedCategoryId: String,
    searchQuery: String,
    isTv: Boolean,
    errorMessage: String?,
    continuingProgress: List<Pair<Series, EpisodeProgress>>,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSeriesSelected: (Series) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val visibleSeries = remember(series, selectedCategoryId, searchQuery, favoriteIds) {
        series.asSequence()
            .filter { item ->
                when (selectedCategoryId) {
                    SeriesAllCategory -> true
                    SeriesFavoritesCategory -> item.favoriteId in favoriteIds
                    else -> item.categoryId == selectedCategoryId
                }
            }
            .filter { item -> searchQuery.isBlank() || item.name.contains(searchQuery.trim(), ignoreCase = true) }
            .toList()
    }
    val allCategories = remember(categories) {
        listOf(
            SeriesCategory(SeriesAllCategory, SeriesAllCategory),
            SeriesCategory(SeriesFavoritesCategory, "Favoritos"),
        ) + categories
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 16.dp),
    ) {
        Text(
            text = "Series",
            color = PayDaTextPrimary,
            style = if (isTv) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PayDaTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar serie") },
            singleLine = true,
            isTvStyle = isTv,
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(top = 8.dp),
                color = PayDaError,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (continuingProgress.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Continuar viendo", color = PayDaTextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(continuingProgress.take(8), key = { it.first.favoriteId }) { (item, progress) ->
                    PayDaCard(
                        modifier = Modifier.width(if (isTv) 220.dp else 180.dp),
                        onClick = { onSeriesSelected(item) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    ) {
                        Column {
                            Text(item.name, color = PayDaTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Continuar desde ${formatEpisodeMillis(progress.positionMillis)}",
                                color = PayDaTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 22.dp else 12.dp),
        ) {
            LazyColumn(
                modifier = Modifier.width(if (isTv) 260.dp else 132.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(allCategories, key = { it.id }) { category ->
                    SeriesCategoryRow(
                        category = category,
                        selected = category.id == selectedCategoryId,
                        count = when (category.id) {
                            SeriesAllCategory -> series.size
                            SeriesFavoritesCategory -> series.count { it.favoriteId in favoriteIds }
                            else -> series.count { it.categoryId == category.id }
                        },
                        onClick = { onCategorySelected(category.id) },
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTv) 5 else 2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(visibleSeries, key = { it.favoriteId }) { item ->
                    SeriesPosterCard(
                        series = item,
                        isFavorite = item.favoriteId in favoriteIds,
                        onClick = { onSeriesSelected(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesCategoryRow(
    category: SeriesCategory,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    PayDaCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (selected) "${category.name} *" else category.name,
                modifier = Modifier.weight(1f),
                color = PayDaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(count.toString(), color = PayDaTextSecondary)
        }
    }
}

@Composable
private fun SeriesPosterCard(
    series: Series,
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    PayDaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column {
            SeriesPoster(
                title = series.name,
                url = series.coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isFavorite) "* ${series.name}" else series.name,
                    color = PayDaTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = listOfNotNull(series.year, series.rating).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, color = PayDaTextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SeriesDetailScreen(
    detail: SeriesDetail,
    episodeProgress: Map<String, EpisodeProgress>,
    isFavorite: Boolean,
    isTv: Boolean,
    errorMessage: String?,
    onSeasonSelected: (Season) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val watchedCount = remember(episodeProgress) { episodeProgress.values.count { it.watched } }
    val totalCount = detail.episodesBySeasonId.values.sumOf { it.size }
    val meta = listOfNotNull(detail.series.year ?: detail.series.releaseDate, detail.series.rating, detail.series.genre)
        .joinToString(" · ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            SeriesPoster(
                title = detail.series.name,
                url = detail.series.coverUrl,
                modifier = Modifier
                    .width(if (isTv) 220.dp else 130.dp)
                    .aspectRatio(0.68f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.series.name, color = PayDaTextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (meta.isNotBlank()) Text(meta, color = PayDaTextSecondary, modifier = Modifier.padding(top = 8.dp))
                if (totalCount > 0) {
                    Text("$watchedCount de $totalCount episodios vistos", color = PayDaTextSecondary, modifier = Modifier.padding(top = 6.dp))
                }
                if (!detail.series.plot.isNullOrBlank()) {
                    Text(detail.series.plot, color = PayDaTextSecondary, modifier = Modifier.padding(top = 14.dp), maxLines = if (isTv) 4 else 6)
                }
                Spacer(modifier = Modifier.height(12.dp))
                PayDaButton(
                    text = if (isFavorite) "Quitar favorito" else "Favorito",
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = PayDaError, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("Temporadas", color = PayDaTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        if (detail.seasons.isEmpty()) {
            Text("Esta serie no contiene temporadas.", color = PayDaTextSecondary)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTv) 4 else 2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(detail.seasons, key = { it.id }) { season ->
                    val episodes = detail.episodesBySeasonId[season.id].orEmpty()
                    val watched = episodes.count { episodeProgress[it.progressId]?.watched == true }
                    PayDaCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSeasonSelected(season) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    ) {
                        Column {
                            Text(season.name, color = PayDaTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$watched de ${episodes.size} episodios vistos",
                                color = PayDaTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesEpisodesScreen(
    series: Series,
    season: Season,
    episodes: List<Episode>,
    episodeProgress: Map<String, EpisodeProgress>,
    isTv: Boolean,
    onEpisodeSelected: (Episode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 18.dp),
    ) {
        Text(
            text = series.name,
            color = PayDaTextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = season.name,
            color = PayDaTextSecondary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (episodes.isEmpty()) {
            Text("Esta temporada no contiene episodios.", color = PayDaTextSecondary)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(episodes, key = { it.progressId }) { episode ->
                    EpisodeRow(
                        episode = episode,
                        progress = episodeProgress[episode.progressId],
                        onClick = { onEpisodeSelected(episode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    progress: EpisodeProgress?,
    onClick: () -> Unit,
) {
    PayDaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SeriesPoster(
                title = episode.title,
                url = episode.imageUrl,
                modifier = Modifier
                    .width(92.dp)
                    .aspectRatio(16f / 9f),
            )
            Column(modifier = Modifier.weight(1f)) {
                val code = listOfNotNull(
                    episode.seasonNumber?.let { "T$it" },
                    episode.number?.let { "E${"%02d".format(it)}" },
                ).joinToString(" · ")
                if (code.isNotBlank()) Text(code, color = PayDaTextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(episode.title, color = PayDaTextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val status = when {
                    progress?.watched == true -> "Visto"
                    progress?.shouldResume() == true -> "Continuar desde ${formatEpisodeMillis(progress.positionMillis)}"
                    else -> episode.duration
                }
                if (!status.isNullOrBlank()) Text(status, color = PayDaTextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun EpisodeResumeScreen(
    episode: Episode,
    progress: EpisodeProgress?,
    onPlay: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(episode.title, color = PayDaTextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (progress?.shouldResume() == true) {
            PayDaButton(text = "Continuar desde ${formatEpisodeMillis(progress.positionMillis)}", onClick = { onPlay(true) }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
            PayDaButton(text = "Empezar desde el principio", onClick = { onPlay(false) }, modifier = Modifier.fillMaxWidth())
        } else {
            PayDaButton(text = "Reproducir", onClick = { onPlay(false) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun EpisodePlayerScreen(
    episode: Episode,
    startPositionMillis: Long,
    hasNextEpisode: Boolean,
    onSaveProgress: (EpisodeProgress?) -> Unit,
    onPlayNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(episode.streamUrl) {
        val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 90_000, 2_500, 5_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setMediaItem(buildMediaItem(episode.streamUrl))
                playWhenReady = true
                prepare()
                if (startPositionMillis > 0) seekTo(startPositionMillis)
            }
    }

    fun currentProgress(): EpisodeProgress? {
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition.coerceAtLeast(0L)
        return when {
            duration > 0 && position.toDouble() >= duration * EpisodeProgress.WatchedThreshold -> EpisodeProgress(
                episodeId = episode.progressId,
                positionMillis = 0L,
                durationMillis = duration,
                watched = true,
            )
            duration > 0 && position >= EpisodeProgress.MinimumResumeMillis -> EpisodeProgress(
                episodeId = episode.progressId,
                positionMillis = position,
                durationMillis = duration,
            )
            else -> null
        }
    }

    BackHandler {
        onSaveProgress(currentProgress())
        onBack()
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) player.play()
                Lifecycle.Event.ON_RESUME -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) player.play()
                Lifecycle.Event.ON_PAUSE -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) player.pause()
                Lifecycle.Event.ON_STOP -> if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) player.pause()
                else -> Unit
            }
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onSaveProgress(EpisodeProgress(episode.progressId, 0L, player.duration.coerceAtLeast(0L), watched = true))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        player.addListener(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(PayDaBackground)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = true
                }
            },
            update = { it.player = player },
        )
        if (hasNextEpisode) {
            PayDaButton(
                text = "Reproducir siguiente episodio",
                onClick = onPlayNext,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun SeriesPoster(
    title: String,
    url: String?,
    modifier: Modifier = Modifier,
) {
    val image = produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = url?.let { loadChannelLogo(it) }
    }
    Box(
        modifier = modifier.background(PayDaSurface, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (image.value != null) {
            Image(
                bitmap = image.value!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            PayDaPlaceholderLogo(title, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun formatEpisodeMillis(value: Long): String {
    val totalSeconds = value / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
