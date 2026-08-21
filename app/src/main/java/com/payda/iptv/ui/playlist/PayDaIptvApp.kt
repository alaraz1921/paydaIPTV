package com.payda.iptv.ui.playlist

import androidx.activity.compose.BackHandler
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
import com.payda.iptv.data.NetworkConnectivity
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.data.XtreamConfig
import com.payda.iptv.data.XtreamRepository
import com.payda.iptv.data.isUsableEpgUrl
import com.payda.iptv.data.normalizeXtreamServer
import com.payda.iptv.data.resolveEpgUrl
import com.payda.iptv.data.stableFavoriteId
import com.payda.iptv.epg.EpgData
import com.payda.iptv.epg.EpgRepository
import com.payda.iptv.settings.SettingsRepository
import com.payda.iptv.ui.tv.DeviceType
import com.payda.iptv.ui.tv.TvChannelListScreen
import com.payda.iptv.ui.tv.TvChannelPreviewScreen
import com.payda.iptv.ui.tv.TvHomeScreen
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
    val xtreamRepository = remember { XtreamRepository() }
    val epgRepository = remember { EpgRepository() }
    val networkConnectivity = remember { NetworkConnectivity(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val favoriteChannelIds by settingsRepository.favoriteChannelIds.collectAsState(initial = emptySet())
    var playlistUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(PlaylistSourceType.M3U) }
    var epgUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUsername by remember { mutableStateOf("") }
    var xtreamPassword by remember { mutableStateOf("") }
    var manualEpgUrlConfigured by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var epgData by remember { mutableStateOf<EpgData?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var tvScreen by remember { mutableStateOf(TvScreen.HOME) }
    var mobileScreen by remember { mutableStateOf(MobileScreen.HOME) }
    var tvPreviewChannel by remember { mutableStateOf<Channel?>(null) }
    var tvPreviewChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
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
    val testXtreamOption = remember {
        if (
            BuildConfig.DEBUG &&
            BuildConfig.TEST_XTREAM_SERVER.isNotBlank() &&
            BuildConfig.TEST_XTREAM_USER.isNotBlank() &&
            BuildConfig.TEST_XTREAM_PASSWORD.isNotBlank()
        ) {
            TestXtreamOption(
                label = "Xtream Prueba",
                server = BuildConfig.TEST_XTREAM_SERVER,
                username = BuildConfig.TEST_XTREAM_USER,
                password = BuildConfig.TEST_XTREAM_PASSWORD,
            )
        } else {
            null
        }
    }

    fun clearLoadedContent() {
        channels = emptyList()
        epgData = null
        selectedChannel = null
        tvScreen = TvScreen.HOME
        mobileScreen = MobileScreen.HOME
        tvPreviewChannel = null
        tvPreviewChannels = emptyList()
        lastSelectedChannelId = null
        selectedCategory = AllCategoryName
        searchQuery = ""
        errorMessage = null
        epgMessage = null
    }

    fun changeSourceType(newSourceType: PlaylistSourceType) {
        sourceType = newSourceType
        clearLoadedContent()
        coroutineScope.launch {
            settingsRepository.savePlaylistSourceType(newSourceType)
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
            if (!networkConnectivity.hasInternetConnection()) {
                epgData = null
                epgMessage = "Sin conexion a Internet"
                return@launch
            }
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
            if (!networkConnectivity.hasInternetConnection()) {
                errorMessage = "Sin conexion a Internet"
                loadingMessage = null
                isLoading = false
                return@launch
            }
            runCatching { repository.loadPlaylist(requestedUrl) }
                .onSuccess { loadedPlaylist ->
                    val loadedChannels = loadedPlaylist.channels
                    if (loadedChannels.isEmpty()) {
                        errorMessage = "La lista no contiene canales validos."
                    } else {
                        playlistUrl = requestedUrl
                        channels = loadedChannels
                        tvScreen = TvScreen.HOME
                        mobileScreen = MobileScreen.HOME
                        tvPreviewChannel = null
                        tvPreviewChannels = emptyList()
                        selectedCategory = AllCategoryName
                        searchQuery = ""
                        sourceType = PlaylistSourceType.M3U
                        settingsRepository.savePlaylistSourceType(PlaylistSourceType.M3U)
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

    fun loadXtream(config: XtreamConfig, message: String? = null) {
        coroutineScope.launch {
            isLoading = true
            loadingMessage = message
            errorMessage = null
            epgMessage = null
            if (!networkConnectivity.hasInternetConnection()) {
                errorMessage = "Sin conexion a Internet"
                loadingMessage = null
                isLoading = false
                return@launch
            }
            val normalizedConfig = runCatching {
                config.copy(server = normalizeXtreamServer(config.server))
            }.getOrElse { error ->
                errorMessage = error.message ?: "Servidor Xtream invalido."
                loadingMessage = null
                isLoading = false
                return@launch
            }

            runCatching { xtreamRepository.loadLiveData(normalizedConfig) }
                .onSuccess { liveData ->
                    if (liveData.channels.isEmpty()) {
                        errorMessage = "La fuente Xtream no contiene canales live validos."
                    } else {
                        sourceType = PlaylistSourceType.XTREAM
                        xtreamServer = normalizedConfig.server
                        xtreamUsername = normalizedConfig.username
                        xtreamPassword = normalizedConfig.password
                        channels = liveData.channels
                        epgData = null
                        tvScreen = TvScreen.HOME
                        mobileScreen = MobileScreen.HOME
                        tvPreviewChannel = null
                        tvPreviewChannels = emptyList()
                        selectedCategory = AllCategoryName
                        searchQuery = ""
                        settingsRepository.savePlaylistSourceType(PlaylistSourceType.XTREAM)
                        settingsRepository.saveXtreamConfig(normalizedConfig)
                        epgMessage = null
                    }
                }
                .onFailure { error ->
                    errorMessage = error.message ?: "No se pudo conectar con Xtream."
                }
            loadingMessage = null
            isLoading = false
        }
    }

    fun submitConfiguredSource() {
        if (sourceType == PlaylistSourceType.M3U) {
            val requestedUrl = playlistUrl.trim()
            if (requestedUrl.isBlank()) {
                errorMessage = "Introduce una URL M3U valida."
                return
            }
            loadPlaylist(requestedUrl)
        } else {
            val config = XtreamConfig(
                server = xtreamServer.trim(),
                username = xtreamUsername.trim(),
                password = xtreamPassword,
            )
            if (config.server.isBlank() || config.username.isBlank() || config.password.isBlank()) {
                errorMessage = "Introduce servidor, usuario y contrasena Xtream."
                return
            }
            loadXtream(config)
        }
    }

    LaunchedEffect(Unit) {
        val savedSourceType = settingsRepository.getPlaylistSourceType()
        val savedUrl = settingsRepository.getLastPlaylistUrl()
        val savedEpgUrl = settingsRepository.getLastEpgUrl()
        val savedXtreamConfig = settingsRepository.getXtreamConfig()
        sourceType = savedSourceType
        if (savedXtreamConfig != null) {
            xtreamServer = savedXtreamConfig.server
            xtreamUsername = savedXtreamConfig.username
            xtreamPassword = savedXtreamConfig.password
        }
        if (!savedEpgUrl.isNullOrBlank() && isUsableEpgUrl(savedEpgUrl)) {
            epgUrl = savedEpgUrl
            manualEpgUrlConfigured = false
        } else if (!savedEpgUrl.isNullOrBlank()) {
            settingsRepository.clearLastEpgUrl()
        }
        if (savedSourceType == PlaylistSourceType.XTREAM && savedXtreamConfig != null) {
            loadXtream(savedXtreamConfig, "Conectando con Xtream...")
        } else if (!savedUrl.isNullOrBlank()) {
            playlistUrl = savedUrl
            loadPlaylist(savedUrl, "Cargando ultima lista...")
        }
    }

    when (val channel = selectedChannel) {
        null -> {
            if (channels.isEmpty()) {
                PlaylistScreen(
                        sourceType = sourceType,
                        playlistUrl = playlistUrl,
                        epgUrl = epgUrl,
                        xtreamServer = xtreamServer,
                        xtreamUsername = xtreamUsername,
                        xtreamPassword = xtreamPassword,
                        onSourceTypeChange = ::changeSourceType,
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
                        onXtreamServerChange = {
                            xtreamServer = it
                            errorMessage = null
                        },
                        onXtreamUsernameChange = {
                            xtreamUsername = it
                            errorMessage = null
                        },
                        onXtreamPasswordChange = {
                            xtreamPassword = it
                            errorMessage = null
                        },
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    errorMessage = errorMessage,
                    testPlaylistOptions = testPlaylistOptions,
                    testEpgOption = testEpgOption,
                    testXtreamOption = testXtreamOption,
                    onLoadPlaylist = ::submitConfiguredSource,
                    isTvStyle = deviceType == DeviceType.TV,
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
                val sharedOnChangePlaylist: () -> Unit = {
                    channels = emptyList()
                    epgData = null
                    selectedChannel = null
                    tvScreen = TvScreen.HOME
                    mobileScreen = MobileScreen.HOME
                    tvPreviewChannel = null
                    tvPreviewChannels = emptyList()
                    lastSelectedChannelId = null
                    selectedCategory = AllCategoryName
                    errorMessage = null
                    epgMessage = null
                }

                if (deviceType == DeviceType.TV) {
                    val previewChannel = tvPreviewChannel
                    when {
                        previewChannel != null -> TvChannelPreviewScreen(
                            initialChannel = previewChannel,
                            categoryChannels = tvPreviewChannels.ifEmpty { channels },
                            favoriteChannelIds = favoriteChannelIds,
                            epgData = epgData,
                            epgNow = epgNow,
                            onChannelChanged = { changedChannel ->
                                lastSelectedChannelId = changedChannel.stableFavoriteId()
                                tvPreviewChannel = changedChannel
                            },
                            onToggleFavorite = sharedOnToggleFavorite,
                            onBackToGrid = { tvPreviewChannel = null },
                        )
                        tvScreen == TvScreen.HOME -> TvHomeScreen(
                            channelCount = channels.size,
                            onOpenLiveTv = { tvScreen = TvScreen.LIVE_TV },
                            onChangePlaylist = { tvScreen = TvScreen.CONFIG },
                            onOpenSettings = { tvScreen = TvScreen.CONFIG },
                        )
                        tvScreen == TvScreen.CONFIG -> PlaylistScreen(
                            sourceType = sourceType,
                            playlistUrl = playlistUrl,
                            epgUrl = epgUrl,
                            xtreamServer = xtreamServer,
                            xtreamUsername = xtreamUsername,
                            xtreamPassword = xtreamPassword,
                            onSourceTypeChange = ::changeSourceType,
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
                            onXtreamServerChange = {
                                xtreamServer = it
                                errorMessage = null
                            },
                            onXtreamUsernameChange = {
                                xtreamUsername = it
                                errorMessage = null
                            },
                            onXtreamPasswordChange = {
                                xtreamPassword = it
                                errorMessage = null
                            },
                            isLoading = isLoading,
                            loadingMessage = loadingMessage,
                            errorMessage = errorMessage,
                            testPlaylistOptions = testPlaylistOptions,
                            testEpgOption = testEpgOption,
                            testXtreamOption = testXtreamOption,
                            onLoadPlaylist = ::submitConfiguredSource,
                            isTvStyle = true,
                            onBack = { tvScreen = TvScreen.HOME },
                        )
                        else -> TvChannelListScreen(
                            channels = channels,
                            selectedCategoryName = selectedCategory,
                            categorySearchQuery = searchQuery,
                            favoriteChannelIds = favoriteChannelIds,
                            epgData = epgData,
                            epgNow = epgNow,
                            epgMessage = epgMessage,
                            lastSelectedChannelId = lastSelectedChannelId,
                            onCategorySelected = { selectedCategory = it },
                            onCategorySearchChange = { searchQuery = it },
                            onClearCategorySearch = { searchQuery = "" },
                            onToggleFavorite = sharedOnToggleFavorite,
                            onChannelSelected = { channelToPreview, categoryChannels ->
                                lastSelectedChannelId = channelToPreview.stableFavoriteId()
                                tvPreviewChannel = channelToPreview
                                tvPreviewChannels = categoryChannels
                            },
                            onBackToHome = { tvScreen = TvScreen.HOME },
                        )
                    }
                } else {
                    when (mobileScreen) {
                        MobileScreen.HOME -> MobileHomeScreen(
                            channelCount = channels.size,
                            onOpenLiveTv = { mobileScreen = MobileScreen.LIVE_TV },
                            onOpenPlaylist = { mobileScreen = MobileScreen.CONFIG },
                            onOpenSettings = { mobileScreen = MobileScreen.CONFIG },
                        )
                        MobileScreen.CONFIG -> PlaylistScreen(
                            sourceType = sourceType,
                            playlistUrl = playlistUrl,
                            epgUrl = epgUrl,
                            xtreamServer = xtreamServer,
                            xtreamUsername = xtreamUsername,
                            xtreamPassword = xtreamPassword,
                            onSourceTypeChange = ::changeSourceType,
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
                            onXtreamServerChange = {
                                xtreamServer = it
                                errorMessage = null
                            },
                            onXtreamUsernameChange = {
                                xtreamUsername = it
                                errorMessage = null
                            },
                            onXtreamPasswordChange = {
                                xtreamPassword = it
                                errorMessage = null
                            },
                            isLoading = isLoading,
                            loadingMessage = loadingMessage,
                            errorMessage = errorMessage,
                            testPlaylistOptions = testPlaylistOptions,
                            testEpgOption = testEpgOption,
                            testXtreamOption = testXtreamOption,
                            onLoadPlaylist = ::submitConfiguredSource,
                            onBack = { mobileScreen = MobileScreen.HOME },
                        )
                        MobileScreen.LIVE_TV -> {
                            BackHandler {
                                mobileScreen = MobileScreen.HOME
                            }
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
                                onChangePlaylist = { mobileScreen = MobileScreen.CONFIG },
                            )
                        }
                    }
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

private enum class TvScreen {
    HOME,
    LIVE_TV,
    CONFIG,
}

private enum class MobileScreen {
    HOME,
    LIVE_TV,
    CONFIG,
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
