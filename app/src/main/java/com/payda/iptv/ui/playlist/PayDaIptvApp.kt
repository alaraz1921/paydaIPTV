package com.payda.iptv.ui.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.payda.iptv.data.Channel
import com.payda.iptv.data.M3uRepository
import kotlinx.coroutines.launch

private const val SamplePlaylistUrl =
    "https://gist.github.com/shaunlynneberg/707e95e03fe9e86e2ecde274dd54611e/raw/af290030e22a77ae2a55e5468a262364984078a0/TestIPTVPlaylist.m3u"

@Composable
fun PayDaIptvApp() {
    val repository = remember { M3uRepository() }
    val coroutineScope = rememberCoroutineScope()
    var playlistUrl by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedCategory by remember { mutableStateOf(AllCategoryName) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    errorMessage = errorMessage,
                    onLoadPlaylist = {
                        val requestedUrl = playlistUrl.trim()
                        if (requestedUrl.isBlank()) {
                            errorMessage = "Introduce una URL M3U valida."
                            return@PlaylistScreen
                        }

                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            runCatching { repository.loadChannels(requestedUrl) }
                                .onSuccess { loadedChannels ->
                                    if (loadedChannels.isEmpty()) {
                                        errorMessage = "La lista no contiene canales validos."
                                    } else {
                                        channels = loadedChannels
                                        selectedCategory = AllCategoryName
                                    }
                                }
                                .onFailure { error ->
                                    errorMessage = error.message
                                        ?: "No se pudo cargar la lista M3U."
                                }
                            isLoading = false
                        }
                    },
                )
            } else {
                ChannelListScreen(
                    channels = channels,
                    selectedCategoryName = selectedCategory,
                    playlistUrl = playlistUrl,
                    onCategorySelected = { selectedCategory = it },
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
            onBack = { selectedChannel = null },
        )
    }
}
