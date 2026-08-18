package com.payda.iptv.ui.playlist

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.payda.iptv.data.Channel
import com.payda.iptv.data.M3uRepository
import com.payda.iptv.data.stableFavoriteId
import com.payda.iptv.settings.SettingsRepository
import kotlinx.coroutines.launch

private const val SamplePlaylistUrl =
    "https://gist.github.com/shaunlynneberg/707e95e03fe9e86e2ecde274dd54611e/raw/af290030e22a77ae2a55e5468a262364984078a0/TestIPTVPlaylist.m3u"

@Composable
fun PayDaIptvApp() {
    val context = LocalContext.current
    val repository = remember { M3uRepository() }
    val settingsRepository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val favoriteChannelIds by settingsRepository.favoriteChannelIds.collectAsState(initial = emptySet())
    var playlistUrl by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedCategory by remember { mutableStateOf(AllCategoryName) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadingMessage by remember { mutableStateOf<String?>(null) }

    fun loadPlaylist(requestedUrl: String, message: String? = null) {
        coroutineScope.launch {
            isLoading = true
            loadingMessage = message
            errorMessage = null
            runCatching { repository.loadChannels(requestedUrl) }
                .onSuccess { loadedChannels ->
                    if (loadedChannels.isEmpty()) {
                        errorMessage = "La lista no contiene canales validos."
                    } else {
                        playlistUrl = requestedUrl
                        channels = loadedChannels
                        selectedCategory = AllCategoryName
                        searchQuery = ""
                        settingsRepository.saveLastPlaylistUrl(requestedUrl)
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message
                        ?: "No se pudo cargar la lista M3U."
                }
            loadingMessage = null
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val savedUrl = settingsRepository.getLastPlaylistUrl()
        if (!savedUrl.isNullOrBlank()) {
            playlistUrl = savedUrl
            loadPlaylist(savedUrl, "Cargando ultima lista...")
        }
    }

    when (val channel = selectedChannel) {
        null -> {
            if (channels.isEmpty()) {
                PlaylistScreen(
                        playlistUrl = playlistUrl,
                        onPlaylistUrlChange = {
                            playlistUrl = it
                            errorMessage = null
                        },
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    errorMessage = errorMessage,
                    onLoadPlaylist = {
                        val requestedUrl = playlistUrl.trim()
                        if (requestedUrl.isBlank()) {
                            errorMessage = "Introduce una URL M3U valida."
                            return@PlaylistScreen
                        }

                        loadPlaylist(requestedUrl)
                    },
                )
            } else {
                ChannelListScreen(
                    channels = channels,
                    selectedCategoryName = selectedCategory,
                    searchQuery = searchQuery,
                    favoriteChannelIds = favoriteChannelIds,
                    playlistUrl = playlistUrl,
                    onCategorySelected = { selectedCategory = it },
                    onSearchQueryChange = { searchQuery = it },
                    onClearSearch = { searchQuery = "" },
                    onToggleFavorite = { channelToToggle ->
                        coroutineScope.launch {
                            settingsRepository.toggleFavorite(channelToToggle.stableFavoriteId())
                        }
                    },
                    onChannelSelected = { selectedChannel = it },
                    onChangePlaylist = {
                        channels = emptyList()
                        selectedChannel = null
                        selectedCategory = AllCategoryName
                        errorMessage = null
                    },
                )
            }
        }
        else -> PlayerScreen(
            channel = channel,
            isFavorite = channel.stableFavoriteId() in favoriteChannelIds,
            onToggleFavorite = {
                coroutineScope.launch {
                    settingsRepository.toggleFavorite(channel.stableFavoriteId())
                }
            },
            onBack = { selectedChannel = null },
        )
    }
}
