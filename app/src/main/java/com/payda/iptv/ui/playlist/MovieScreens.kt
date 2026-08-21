package com.payda.iptv.ui.playlist

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.payda.iptv.BuildConfig
import com.payda.iptv.data.Movie
import com.payda.iptv.data.MovieCategory
import com.payda.iptv.data.MovieProgress
import com.payda.iptv.ui.theme.PayDaBackground
import com.payda.iptv.ui.theme.PayDaButton
import com.payda.iptv.ui.theme.PayDaCard
import com.payda.iptv.ui.theme.PayDaError
import com.payda.iptv.ui.theme.PayDaPlaceholderLogo
import com.payda.iptv.ui.theme.PayDaSurface
import com.payda.iptv.ui.theme.PayDaTextFieldColors
import com.payda.iptv.ui.theme.PayDaTextPrimary
import com.payda.iptv.ui.theme.PayDaTextSecondary

internal const val MovieAllCategory = "Todos"
internal const val MovieFavoritesCategory = "Favoritos"

@Composable
fun MovieCatalogScreen(
    movies: List<Movie>,
    categories: List<MovieCategory>,
    favoriteIds: Set<String>,
    selectedCategoryId: String,
    searchQuery: String,
    isTv: Boolean,
    errorMessage: String?,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMovieSelected: (Movie) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val visibleMovies = remember(movies, selectedCategoryId, searchQuery, favoriteIds) {
        movies.asSequence()
            .filter { movie ->
                when (selectedCategoryId) {
                    MovieAllCategory -> true
                    MovieFavoritesCategory -> movie.favoriteId in favoriteIds
                    else -> movie.categoryId == selectedCategoryId
                }
            }
            .filter { movie ->
                searchQuery.isBlank() || movie.name.contains(searchQuery.trim(), ignoreCase = true)
            }
            .toList()
    }
    val allCategories = remember(categories, movies, favoriteIds) {
        listOf(
            MovieCategory(MovieAllCategory, MovieAllCategory),
            MovieCategory(MovieFavoritesCategory, "Favoritos"),
        ) + categories
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 16.dp),
    ) {
        Text(
            text = "Peliculas",
            color = PayDaTextPrimary,
            style = if (isTv) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar pelicula") },
            singleLine = true,
            colors = PayDaTextFieldColors(),
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(top = 8.dp),
                color = PayDaError,
                style = MaterialTheme.typography.bodyMedium,
            )
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
                    MovieCategoryRow(
                        category = category,
                        selected = category.id == selectedCategoryId,
                        count = when (category.id) {
                            MovieAllCategory -> movies.size
                            MovieFavoritesCategory -> movies.count { it.favoriteId in favoriteIds }
                            else -> movies.count { it.categoryId == category.id }
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
                items(visibleMovies, key = { it.favoriteId }) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        isFavorite = movie.favoriteId in favoriteIds,
                        onClick = { onMovieSelected(movie) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieCategoryRow(
    category: MovieCategory,
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
            Text(text = count.toString(), color = PayDaTextSecondary)
        }
    }
}

@Composable
private fun MoviePosterCard(
    movie: Movie,
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    PayDaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column {
            MoviePoster(
                movie = movie,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isFavorite) "* ${movie.name}" else movie.name,
                    color = PayDaTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = listOfNotNull(movie.year, movie.rating).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        color = PayDaTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailScreen(
    movie: Movie,
    progress: MovieProgress?,
    isFavorite: Boolean,
    isTv: Boolean,
    onPlay: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val meta = listOfNotNull(movie.year, movie.rating, movie.duration, movie.categoryName)
        .joinToString(" · ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground)
            .padding(if (isTv) 42.dp else 18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            MoviePoster(
                movie = movie,
                modifier = Modifier
                    .width(if (isTv) 240.dp else 140.dp)
                    .aspectRatio(0.68f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.name,
                    color = PayDaTextPrimary,
                    style = if (isTv) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (meta.isNotBlank()) {
                    Text(text = meta, color = PayDaTextSecondary, modifier = Modifier.padding(top = 8.dp))
                }
                if (!movie.plot.isNullOrBlank()) {
                    Text(
                        text = movie.plot,
                        color = PayDaTextSecondary,
                        modifier = Modifier.padding(top = 18.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                val resumeText = progress?.takeIf { it.shouldResume() }
                    ?.let { "Continuar desde ${formatMillis(it.positionMillis)}" }
                if (resumeText != null) {
                    PayDaButton(text = resumeText, onClick = { onPlay(true) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(10.dp))
                    PayDaButton(text = "Empezar desde el principio", onClick = { onPlay(false) }, modifier = Modifier.fillMaxWidth())
                } else {
                    PayDaButton(text = "Reproducir", onClick = { onPlay(false) }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(10.dp))
                PayDaButton(
                    text = if (isFavorite) "Quitar favorito" else "Favorito",
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MoviePlayerScreen(
    movie: Movie,
    startPositionMillis: Long,
    onSaveProgress: (MovieProgress?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(movie.streamUrl) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,
                90_000,
                2_500,
                5_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
            setMediaItem(buildMediaItem(movie.streamUrl))
            playWhenReady = true
            prepare()
            if (startPositionMillis > 0) {
                seekTo(startPositionMillis)
            }
        }
    }

    fun persistProgressAndBack() {
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition.coerceAtLeast(0L)
        onSaveProgress(
            if (
                duration > 0 &&
                position >= MovieProgress.MinimumResumeMillis &&
                position.toDouble() < duration * MovieProgress.WatchedThreshold
            ) {
                MovieProgress(movie.favoriteId, position, duration)
            } else {
                null
            },
        )
        onBack()
    }

    BackHandler { persistProgressAndBack() }

    DisposableEffect(lifecycleOwner, player) {
        var videoDecoder = "Desconocido"
        var audioDecoder = "Desconocido"
        var droppedFrames = 0
        var rebuffers = 0
        val analyticsListener = object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                videoDecoder = decoderName
                logMovieDiagnostics(movie, player, videoDecoder, audioDecoder, droppedFrames, rebuffers)
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                audioDecoder = decoderName
                logMovieDiagnostics(movie, player, videoDecoder, audioDecoder, droppedFrames, rebuffers)
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFramesCount: Int,
                elapsedMs: Long,
            ) {
                droppedFrames += droppedFramesCount
                logMovieDiagnostics(movie, player, videoDecoder, audioDecoder, droppedFrames, rebuffers)
            }
        }
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
            override fun onTracksChanged(tracks: Tracks) {
                logMovieDiagnostics(movie, player, videoDecoder, audioDecoder, droppedFrames, rebuffers)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING && player.playWhenReady) {
                    rebuffers += 1
                    logMovieDiagnostics(movie, player, videoDecoder, audioDecoder, droppedFrames, rebuffers)
                }
                if (playbackState == Player.STATE_ENDED) {
                    onSaveProgress(null)
                }
            }
        }
        player.addListener(listener)
        player.addAnalyticsListener(analyticsListener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeAnalyticsListener(analyticsListener)
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PayDaBackground),
    ) {
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
    }
}

private fun logMovieDiagnostics(
    movie: Movie,
    player: ExoPlayer,
    videoDecoder: String,
    audioDecoder: String,
    droppedFrames: Int,
    rebuffers: Int,
) {
    if (!BuildConfig.DEBUG) return
    val videoFormat = player.currentTracks.groups
        .firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        ?.getTrackFormat(0)
    val audioFormat = player.currentTracks.groups
        .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
        ?.getTrackFormat(0)
    val resolution = if (videoFormat != null && videoFormat.width > 0 && videoFormat.height > 0) {
        "${videoFormat.width}x${videoFormat.height}"
    } else {
        "Desconocida"
    }
    val fps = videoFormat?.frameRate?.takeIf { it > 0 }?.let { "%.2f".format(it) } ?: "Desconocido"
    val bitrate = videoFormat?.bitrate?.takeIf { it > 0 }?.toString() ?: "Desconocido"
    Log.d(
        MovieDiagnosticsTag,
        "Movie playback diagnostics: title=${movie.name.take(60)}, " +
            "Resolution=$resolution, " +
            "Video codec=${videoFormat?.sampleMimeType ?: "Desconocido"}, " +
            "Audio codec=${audioFormat?.sampleMimeType ?: "Desconocido"}, " +
            "FPS=$fps, " +
            "Bitrate=$bitrate, " +
            "Video decoder=$videoDecoder, " +
            "Audio decoder=$audioDecoder, " +
            "Dropped frames=$droppedFrames, " +
            "Rebuffers=$rebuffers, " +
            "Buffered=${player.bufferedPercentage}%",
    )
}

private const val MovieDiagnosticsTag = "PayDaMoviePlayback"

@Composable
private fun MoviePoster(
    movie: Movie,
    modifier: Modifier = Modifier,
) {
    val poster = produceState<ImageBitmap?>(initialValue = null, key1 = movie.posterUrl) {
        value = movie.posterUrl?.let { loadChannelLogo(it) }
    }
    Box(
        modifier = modifier
            .background(PayDaSurface, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (poster.value != null) {
            Image(
                bitmap = poster.value!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            PayDaPlaceholderLogo(movie.name, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun formatMillis(value: Long): String {
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
