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
import com.payda.iptv.BuildConfig
import com.payda.iptv.data.Channel
import com.payda.iptv.data.EpgUrlSource
import com.payda.iptv.data.M3uRepository
import com.payda.iptv.data.isUsableEpgUrl
import com.payda.iptv.data.resolveEpgUrl
import com.payda.iptv.data.stableFavoriteId
import com.payda.iptv.epg.EpgData
import com.payda.iptv.epg.EpgRepository
import com.payda.iptv.settings.SettingsRepository
import com.payda.iptv.ui.tv.DeviceType
import com.payda.iptv.ui.tv.TvChannelListScreen
import com.payda.iptv.ui.tv.rememberDeviceType
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SamplePlaylistUrl =
    "https://gist.github.com/shaunlynneberg/707e95e03fe9e86e2ecde274dd54611e/raw/af290030e22a77ae2a55e5468a262364984078a0/TestIPTVPlaylist.m3u"

@Composable
fun PayDaIptvApp() {
    val context = LocalContext.current
    val deviceType = rememberDeviceType()
    val repository = remember { M3uRepository() }
    val epgRepository = remember { EpgRepository() }
    val settingsRepository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val favoriteChannelIds by settingsRepository.favoriteChannelIds.collectAsState(initial = emptySet())
    var playlistUrl by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var manualEpgUrlConfigured by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var epgData by remember { mutableStateOf<EpgData?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var lastSelectedChannelId by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf(AllCategoryName) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var epgMessage by remember { mutableStateOf<String?>(null) }
    var loadingMessage by remember { mutableStateOf<String?>(null) }
    var epgNow by remember { mutableStateOf(Instant.now()) }
    val testPlaylistOptions = remember {
        if (BuildConfig.DEBUG) {
            listOf(
                TestPlaylistOption("Prueba 1", BuildConfig.TEST_PLAYLIST_URL_1),
                TestPlaylistOption("Prueba 2", BuildConfig.TEST_PLAYLIST_URL_2),
            ).filter { it.url.isNotBlank() }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            epgNow = Instant.now()
        }
    }
    val testEpgOption = remember {
        if (BuildConfig.DEBUG && BuildConfig.TEST_EPG_URL.isNotBlank()) {
            TestPlaylistOption("EPG Prueba", BuildConfig.TEST_EPG_URL)
        } else {
            null
        }
    }

    fun loadEpg(
        requestedUrl: String,
        source: EpgUrlSource,
        matchingChannels: List<Channel> = channels,
    ) {
        if (!isUsableEpgUrl(requestedUrl)) {
            epgData = null
            epgMessage = null
            return
        }

        coroutineScope.launch {
            epgMessage = "Cargando EPG..."
            runCatching { epgRepository.loadEpg(requestedUrl) }
                .onSuccess { loadedEpg ->
                    epgData = loadedEpg
                    if (source == EpgUrlSource.MANUAL) {
                        epgUrl = requestedUrl
                        manualEpgUrlConfigured = true
                        settingsRepository.saveLastEpgUrl(requestedUrl)
                    }
                    epgMessage = if (matchingChannels.isNotEmpty() && !hasEpgMatches(matchingChannels, loadedEpg)) {
                        "EPG cargada sin coincidencias con la playlist."
                    } else {
                        null
                    }
                }
                .onFailure { error ->
                    epgData = null
                    val detail = error.message ?: "No se pudo cargar la EPG."
                    epgMessage = when (source) {
                        EpgUrlSource.MANUAL -> detail
                        EpgUrlSource.AUTO -> "No se pudo cargar la EPG detectada; la playlist sigue disponible. $detail"
                    }
                }
        }
    }

    fun loadPlaylist(requestedUrl: String, message: String? = null) {
        coroutineScope.launch {
            isLoading = true
            loadingMessage = message
            errorMessage = null
            epgMessage = null
            runCatching { repository.loadPlaylist(requestedUrl) }
                .onSuccess { loadedPlaylist ->
                    val loadedChannels = loadedPlaylist.channels
                    if (loadedChannels.isEmpty()) {
                        errorMessage = "La lista no contiene canales validos."
                    } else {
                        playlistUrl = requestedUrl
                        channels = loadedChannels
                        selectedCategory = AllCategoryName
                        searchQuery = ""
                        settingsRepository.saveLastPlaylistUrl(requestedUrl)

                        val epgResolution = resolveEpgUrl(
                            manualUrl = epgUrl,
                            manualConfigured = manualEpgUrlConfigured,
                            detectedUrl = loadedPlaylist.metadata.epgUrlDetected,
                        )

                        when {
                            epgResolution.url != null && epgResolution.source != null -> loadEpg(
                                requestedUrl = epgResolution.url,
                                source = epgResolution.source,
                                matchingChannels = loadedChannels,
                            )
                            epgResolution.ignoredManualUrl -> {
                                epgData = null
                                epgMessage = "La URL EPG manual no parece valida; se cargaron los canales sin EPG."
                            }
                            else -> {
                                epgData = null
                                epgMessage = null
                            }
                        }
                    }
                }
                .onFailure { error ->
                    val detail = error.message ?: "No se pudo cargar la lista M3U."
                    errorMessage = "No se pudo cargar la lista M3U. $detail"
                }
            loadingMessage = null
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val savedUrl = settingsRepository.getLastPlaylistUrl()
        val savedEpgUrl = settingsRepository.getLastEpgUrl()
        if (!savedEpgUrl.isNullOrBlank() && isUsableEpgUrl(savedEpgUrl)) {
            epgUrl = savedEpgUrl
            manualEpgUrlConfigured = false
        } else if (!savedEpgUrl.isNullOrBlank()) {
            settingsRepository.clearLastEpgUrl()
        }
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
                        epgUrl = epgUrl,
                        onPlaylistUrlChange = {
                            playlistUrl = it
                            errorMessage = null
                            epgMessage = null
                        },
                        onEpgUrlChange = {
                            epgUrl = it
                            manualEpgUrlConfigured = it.trim().isNotBlank()
                            epgMessage = null
                            if (it.isBlank()) {
                                epgData = null
                                coroutineScope.launch {
                                    settingsRepository.clearLastEpgUrl()
                                }
                            }
                        },
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    errorMessage = errorMessage,
                    testPlaylistOptions = testPlaylistOptions,
                    testEpgOption = testEpgOption,
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
                val sharedOnToggleFavorite: (Channel) -> Unit = { channelToToggle ->
                    coroutineScope.launch {
                        settingsRepository.toggleFavorite(channelToToggle.stableFavoriteId())
                    }
                }
                val sharedOnChannelSelected: (Channel) -> Unit = { channelToPlay ->
                    lastSelectedChannelId = channelToPlay.stableFavoriteId()
                    selectedChannel = channelToPlay
                }

                if (deviceType == DeviceType.TV) {
                    TvChannelListScreen(
                        channels = channels,
                        selectedCategoryName = selectedCategory,
                        searchQuery = searchQuery,
                        favoriteChannelIds = favoriteChannelIds,
                        epgData = epgData,
                        epgNow = epgNow,
                        epgMessage = epgMessage,
                        lastSelectedChannelId = lastSelectedChannelId,
                        onCategorySelected = { selectedCategory = it },
                        onSearchQueryChange = { searchQuery = it },
                        onClearSearch = { searchQuery = "" },
                        onToggleFavorite = sharedOnToggleFavorite,
                        onChannelSelected = sharedOnChannelSelected,
                        onChangePlaylist = {
                            channels = emptyList()
                            epgData = null
                            selectedChannel = null
                            lastSelectedChannelId = null
                            selectedCategory = AllCategoryName
                            errorMessage = null
                            epgMessage = null
                        },
                    )
                } else {
                    ChannelListScreen(
                        channels = channels,
                        selectedCategoryName = selectedCategory,
                        searchQuery = searchQuery,
                        favoriteChannelIds = favoriteChannelIds,
                        epgData = epgData,
                        epgNow = epgNow,
                        epgMessage = epgMessage,
                        playlistUrl = playlistUrl,
                        onCategorySelected = { selectedCategory = it },
                        onSearchQueryChange = { searchQuery = it },
                        onClearSearch = { searchQuery = "" },
                        onToggleFavorite = sharedOnToggleFavorite,
                        onChannelSelected = sharedOnChannelSelected,
                        onChangePlaylist = {
                            channels = emptyList()
                            epgData = null
                            selectedChannel = null
                            lastSelectedChannelId = null
                            selectedCategory = AllCategoryName
                            errorMessage = null
                            epgMessage = null
                        },
                    )
                }
            }
        }
        else -> PlayerScreen(
            channel = channel,
            epgInfo = epgData?.programmeFor(channel, epgNow),
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

private fun hasEpgMatches(
    channels: List<Channel>,
    epgData: EpgData,
): Boolean {
    return channels.any { channel ->
        val tvgId = channel.tvgId?.trim()?.takeIf { it.isNotEmpty() }
        tvgId != null && epgData.programmesByChannelId.containsKey(tvgId)
    }
}
