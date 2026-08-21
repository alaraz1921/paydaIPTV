package com.payda.iptv.ui.tv

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.payda.iptv.data.Channel
import com.payda.iptv.data.stableFavoriteId
import com.payda.iptv.epg.EpgData
import com.payda.iptv.epg.timeRangeText
import com.payda.iptv.ui.playlist.buildMediaItem
import java.time.Instant

@OptIn(UnstableApi::class)
@Composable
fun TvChannelPreviewScreen(
    initialChannel: Channel,
    categoryChannels: List<Channel>,
    favoriteChannelIds: Set<String>,
    epgData: EpgData?,
    epgNow: Instant,
    onChannelChanged: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onBackToGrid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentChannel by remember(initialChannel.stableFavoriteId()) { mutableStateOf(initialChannel) }
    var playbackMessage by remember { mutableStateOf("Cargando ${initialChannel.name}...") }
    var isFullScreen by remember { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            onBackToGrid()
        }
    }

    LaunchedEffect(currentChannel.streamUrl) {
        playbackMessage = "Cargando ${currentChannel.name}..."
        player.setMediaItem(buildMediaItem(currentChannel.streamUrl))
        player.prepare()
        player.play()
        onChannelChanged(currentChannel)
    }

    DisposableEffect(lifecycleOwner, player) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playbackMessage = when (playbackState) {
                    Player.STATE_BUFFERING -> "Cargando ${currentChannel.name}..."
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

    if (isFullScreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            PlayerViewBox(
                player = player,
                useController = true,
                modifier = Modifier.fillMaxSize(),
            )
            if (playbackMessage.isNotBlank()) {
                PlaybackMessage(playbackMessage)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(horizontal = 42.dp, vertical = 30.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "PayDa IPTV · TV EN DIRECTO",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = { onToggleFavorite(currentChannel) }) {
                Text(if (currentChannel.stableFavoriteId() in favoriteChannelIds) "Quitar favorito" else "Favorito")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(modifier = Modifier.weight(1.45f)) {
                var videoHasFocus by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onFocusChanged { videoHasFocus = it.hasFocus }
                        .focusable()
                        .clickable { isFullScreen = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(
                        width = if (videoHasFocus) 3.dp else 1.dp,
                        color = if (videoHasFocus) Color.White else Color(0xFF2F3B46),
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PlayerViewBox(
                            player = player,
                            useController = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (playbackMessage.isNotBlank()) {
                            PlaybackMessage(playbackMessage)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { isFullScreen = true }) {
                    Text("Pantalla completa")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categoryChannels, key = { it.stableFavoriteId() }) { channel ->
                    PreviewChannelRow(
                        channel = channel,
                        selected = channel.stableFavoriteId() == currentChannel.stableFavoriteId(),
                        onClick = { currentChannel = channel },
                    )
                }
            }
        }

        ProgrammeInfo(
            channel = currentChannel,
            epgData = epgData,
            epgNow = epgNow,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerViewBox(
    player: ExoPlayer,
    useController: Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                keepScreenOn = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.useController = useController
                controllerShowTimeoutMs = 3_000
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { playerView ->
            playerView.player = player
            playerView.useController = useController
        },
    )
}

@Composable
private fun PreviewChannelRow(
    channel: Channel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = when {
            hasFocus -> Color(0xFF26374A)
            selected -> Color(0xFF1F2933)
            else -> Color(0xFF12191F)
        },
        border = if (hasFocus) BorderStroke(2.dp, Color.White) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvChannelLogo(
                logoUrl = channel.logoUrl,
                channelName = channel.name,
                modifier = Modifier.height(38.dp).fillMaxWidth(0.13f),
            )
            Text(
                text = channel.name,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgrammeInfo(
    channel: Channel,
    epgData: EpgData?,
    epgNow: Instant,
) {
    val epgInfo = epgData?.programmeFor(channel, epgNow)
    val currentProgramme = epgInfo?.current
    val nextProgramme = epgInfo?.next
    if (currentProgramme == null && nextProgramme == null) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (currentProgramme != null) {
            Text(
                text = "Programa actual",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${currentProgramme.timeRangeText()} · ${currentProgramme.title}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!currentProgramme.description.isNullOrBlank()) {
                Text(
                    text = currentProgramme.description,
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (nextProgramme != null) {
            Text(
                text = "Siguiente: ${nextProgramme.timeRangeText()} · ${nextProgramme.title}",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BoxScope.PlaybackMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}
