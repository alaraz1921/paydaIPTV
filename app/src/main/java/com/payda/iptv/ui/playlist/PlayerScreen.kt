package com.payda.iptv.ui.playlist

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.payda.iptv.data.Channel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playbackMessage by remember(channel.streamUrl) { mutableStateOf("Cargando ${channel.name}...") }
    val player = remember(channel.streamUrl) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(buildMediaItem(channel.streamUrl))
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(lifecycleOwner, player) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackMessage = when (playbackState) {
                    Player.STATE_BUFFERING -> "Cargando ${channel.name}..."
                    Player.STATE_READY -> ""
                    Player.STATE_ENDED -> "Reproduccion finalizada"
                    Player.STATE_IDLE -> "Reproductor preparado"
                    else -> playbackMessage
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackMessage = "No se pudo cargar el canal: ${error.errorCodeName}"
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    player.play()
                }
                Lifecycle.Event.ON_RESUME -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    player.play()
                }
                Lifecycle.Event.ON_PAUSE -> if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    player.pause()
                }
                Lifecycle.Event.ON_STOP -> if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    player.pause()
                }
                else -> Unit
            }
        }

        player.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(playerListener)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                playerView.player = player
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }
            Button(
                onClick = onToggleFavorite,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (isFavorite) "Quitar favorito" else "Marcar favorito")
            }
            Text(
                text = channel.name,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (playbackMessage.isNotBlank()) {
            Text(
                text = playbackMessage,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun buildMediaItem(streamUrl: String): MediaItem {
    val builder = MediaItem.Builder().setUri(streamUrl)
    if (streamUrl.substringBefore("?").endsWith(".m3u8", ignoreCase = true)) {
        builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    }
    return builder.build()
}
