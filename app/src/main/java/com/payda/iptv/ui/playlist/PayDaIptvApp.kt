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
import com.payda.iptv.data.Movie
import com.payda.iptv.data.MovieCategory
import com.payda.iptv.data.MovieProgress
import com.payda.iptv.data.NetworkConnectivity
import com.payda.iptv.data.Episode
import com.payda.iptv.data.EpisodeProgress
import com.payda.iptv.data.PlaylistConfig
import com.payda.iptv.data.PlaylistConfigStatus
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.data.Season
import com.payda.iptv.data.Series
import com.payda.iptv.data.SeriesCategory
import com.payda.iptv.data.SeriesDetail
import com.payda.iptv.data.XtreamConfig
import com.payda.iptv.data.XtreamAccountInfo
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
    val playlistConfigs by settingsRepository.playlistConfigs.collectAsState(initial = emptyList())
    var currentPlaylistConfigId by remember { mutableStateOf<String?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var editingPlaylistConfig by remember { mutableStateOf<PlaylistConfig?>(null) }
    var showPlaylistConfigEditor by remember { mutableStateOf(false) }
    var playlistUrl by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(PlaylistSourceType.M3U) }
    var epgUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUsername by remember { mutableStateOf("") }
    var xtreamPassword by remember { mutableStateOf("") }
    var manualEpgUrlConfigured by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var epgData by remember { mutableStateOf<EpgData?>(null) }
    var xtreamAccountInfo by remember { mutableStateOf<XtreamAccountInfo?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var movieCategories by remember { mutableStateOf<List<MovieCategory>>(emptyList()) }
    var selectedMovieCategoryId by remember { mutableStateOf(MovieAllCategory) }
    var movieSearchQuery by remember { mutableStateOf("") }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var playingMovie by remember { mutableStateOf<Movie?>(null) }
    var movieProgress by remember { mutableStateOf<MovieProgress?>(null) }
    var moviePlayerStartPosition by remember { mutableStateOf(0L) }
    var movieErrorMessage by remember { mutableStateOf<String?>(null) }
    var seriesCatalog by remember { mutableStateOf<List<Series>>(emptyList()) }
    var seriesCategories by remember { mutableStateOf<List<SeriesCategory>>(emptyList()) }
    var selectedSeriesCategoryId by remember { mutableStateOf(SeriesAllCategory) }
    var seriesSearchQuery by remember { mutableStateOf("") }
    var selectedSeriesDetail by remember { mutableStateOf<SeriesDetail?>(null) }
    var selectedSeriesSeasonId by remember { mutableStateOf<String?>(null) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    var playingEpisode by remember { mutableStateOf<Episode?>(null) }
    var episodePlayerStartPosition by remember { mutableStateOf(0L) }
    var episodeProgressMap by remember { mutableStateOf<Map<String, EpisodeProgress>>(emptyMap()) }
    var seriesDetailCache by remember { mutableStateOf<Map<String, SeriesDetail>>(emptyMap()) }
    var seriesErrorMessage by remember { mutableStateOf<String?>(null) }
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
        xtreamAccountInfo = null
        selectedChannel = null
        movies = emptyList()
        movieCategories = emptyList()
        selectedMovieCategoryId = MovieAllCategory
        movieSearchQuery = ""
        selectedMovie = null
        playingMovie = null
        movieProgress = null
        moviePlayerStartPosition = 0L
        movieErrorMessage = null
        seriesCatalog = emptyList()
        seriesCategories = emptyList()
        selectedSeriesCategoryId = SeriesAllCategory
        seriesSearchQuery = ""
        selectedSeriesDetail = null
        selectedSeriesSeasonId = null
        selectedEpisode = null
        playingEpisode = null
        episodePlayerStartPosition = 0L
        episodeProgressMap = emptyMap()
        seriesDetailCache = emptyMap()
        seriesErrorMessage = null
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

    fun defaultConfigName(type: PlaylistSourceType, value: String): String {
        val host = runCatching { java.net.URL(value).host }.getOrNull()
        return playlistName.trim().ifBlank {
            host?.takeIf { it.isNotBlank() } ?: if (type == PlaylistSourceType.XTREAM) "Xtream 1" else "Playlist 1"
        }
    }

    fun draftPlaylistConfig(status: PlaylistConfigStatus = PlaylistConfigStatus.UNKNOWN): PlaylistConfig {
        val now = System.currentTimeMillis()
        val id = editingPlaylistConfig?.id ?: currentPlaylistConfigId ?: "playlist-$now"
        return PlaylistConfig(
            id = id,
            displayName = defaultConfigName(sourceType, if (sourceType == PlaylistSourceType.XTREAM) xtreamServer else playlistUrl),
            sourceType = sourceType,
            playlistUrl = playlistUrl.trim(),
            server = xtreamServer.trim(),
            username = xtreamUsername.trim(),
            password = xtreamPassword,
            epgUrl = epgUrl.trim(),
            isActive = true,
            createdAtEpochMillis = editingPlaylistConfig?.createdAtEpochMillis ?: now,
            lastUsedAtEpochMillis = now,
            status = status,
        )
    }

    fun applyPlaylistConfigToForm(config: PlaylistConfig) {
        editingPlaylistConfig = config
        currentPlaylistConfigId = config.id
        playlistName = config.displayName
        sourceType = config.sourceType
        playlistUrl = config.playlistUrl
        epgUrl = config.epgUrl
        manualEpgUrlConfigured = config.epgUrl.isNotBlank()
        xtreamServer = config.server
        xtreamUsername = config.username
        xtreamPassword = config.password
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

    fun loadPlaylist(requestedUrl: String, message: String? = null, saveConfig: Boolean = true) {
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
                        xtreamAccountInfo = null
                        movies = emptyList()
                        movieCategories = emptyList()
                        selectedMovie = null
                        playingMovie = null
                        movieProgress = null
                        movieErrorMessage = null
                        seriesCatalog = emptyList()
                        seriesCategories = emptyList()
                        selectedSeriesDetail = null
                        selectedEpisode = null
                        playingEpisode = null
                        episodeProgressMap = emptyMap()
                        seriesDetailCache = emptyMap()
                        seriesErrorMessage = null
                        tvScreen = TvScreen.HOME
                        mobileScreen = MobileScreen.HOME
                        showPlaylistConfigEditor = false
                        tvPreviewChannel = null
                        tvPreviewChannels = emptyList()
                        selectedCategory = AllCategoryName
                        searchQuery = ""
                        sourceType = PlaylistSourceType.M3U
                        settingsRepository.savePlaylistSourceType(PlaylistSourceType.M3U)
                        settingsRepository.saveLastPlaylistUrl(requestedUrl)
                        if (saveConfig) {
                            val savedConfig = draftPlaylistConfig(PlaylistConfigStatus.AVAILABLE)
                            currentPlaylistConfigId = savedConfig.id
                            playlistName = savedConfig.displayName
                            editingPlaylistConfig = savedConfig
                            settingsRepository.upsertPlaylistConfig(savedConfig, makeActive = true)
                        }

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

    fun loadXtream(config: XtreamConfig, message: String? = null, saveConfig: Boolean = true) {
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
                        xtreamAccountInfo = liveData.accountInfo
                        movies = emptyList()
                        movieCategories = emptyList()
                        selectedMovie = null
                        playingMovie = null
                        movieProgress = null
                        movieErrorMessage = null
                        seriesCatalog = emptyList()
                        seriesCategories = emptyList()
                        selectedSeriesDetail = null
                        selectedEpisode = null
                        playingEpisode = null
                        episodeProgressMap = emptyMap()
                        seriesDetailCache = emptyMap()
                        seriesErrorMessage = null
                        epgData = null
                        tvScreen = TvScreen.HOME
                        mobileScreen = MobileScreen.HOME
                        showPlaylistConfigEditor = false
                        tvPreviewChannel = null
                        tvPreviewChannels = emptyList()
                        selectedCategory = AllCategoryName
                        searchQuery = ""
                        settingsRepository.savePlaylistSourceType(PlaylistSourceType.XTREAM)
                        settingsRepository.saveXtreamConfig(normalizedConfig)
                        if (saveConfig) {
                            val savedConfig = draftPlaylistConfig(
                                if (liveData.accountInfo?.status.equals("Expired", ignoreCase = true)) {
                                    PlaylistConfigStatus.EXPIRED
                                } else {
                                    PlaylistConfigStatus.ACTIVE
                                },
                            ).copy(server = normalizedConfig.server)
                            currentPlaylistConfigId = savedConfig.id
                            playlistName = savedConfig.displayName
                            editingPlaylistConfig = savedConfig
                            settingsRepository.upsertPlaylistConfig(savedConfig, makeActive = true)
                        }
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

    fun currentXtreamConfigOrNull(): XtreamConfig? {
        val config = XtreamConfig(
            server = xtreamServer.trim(),
            username = xtreamUsername.trim(),
            password = xtreamPassword,
        )
        if (config.server.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            return null
        }
        return runCatching { config.copy(server = normalizeXtreamServer(config.server)) }.getOrNull()
    }

    fun loadMovies(message: String? = "Cargando peliculas...") {
        if (sourceType != PlaylistSourceType.XTREAM) {
            movieErrorMessage = "Peliculas disponible con Xtream."
            tvScreen = TvScreen.MOVIES
            mobileScreen = MobileScreen.MOVIES
            return
        }
        val config = currentXtreamConfigOrNull()
        if (config == null) {
            movieErrorMessage = "Configura Xtream para ver peliculas."
            tvScreen = TvScreen.MOVIES
            mobileScreen = MobileScreen.MOVIES
            return
        }
        coroutineScope.launch {
            isLoading = true
            loadingMessage = message
            movieErrorMessage = null
            if (!networkConnectivity.hasInternetConnection()) {
                movieErrorMessage = "Sin conexion a Internet"
                tvScreen = TvScreen.MOVIES
                mobileScreen = MobileScreen.MOVIES
                loadingMessage = null
                isLoading = false
                return@launch
            }
            runCatching { xtreamRepository.loadMovieCatalog(config) }
                .onSuccess { catalog ->
                    movies = catalog.movies
                    movieCategories = catalog.categories
                    selectedMovieCategoryId = MovieAllCategory
                    movieSearchQuery = ""
                    selectedMovie = null
                    playingMovie = null
                    movieProgress = null
                    movieErrorMessage = if (catalog.movies.isEmpty()) {
                        "La fuente Xtream no contiene peliculas VOD validas."
                    } else {
                        null
                    }
                    tvScreen = TvScreen.MOVIES
                    mobileScreen = MobileScreen.MOVIES
                }
                .onFailure { error ->
                    movieErrorMessage = error.message ?: "No se pudo cargar peliculas Xtream."
                    tvScreen = TvScreen.MOVIES
                    mobileScreen = MobileScreen.MOVIES
                }
            loadingMessage = null
            isLoading = false
        }
    }

    fun openMovie(movie: Movie) {
        selectedMovie = movie
        movieProgress = null
        coroutineScope.launch {
            movieProgress = settingsRepository.getMovieProgress(movie.favoriteId)
            val config = currentXtreamConfigOrNull()
            if (config != null && networkConnectivity.hasInternetConnection()) {
                runCatching { xtreamRepository.loadMovieInfo(config, movie) }
                    .onSuccess { detailedMovie ->
                        selectedMovie = detailedMovie
                    }
            }
        }
    }

    fun loadSeries(message: String? = "Cargando series...") {
        if (sourceType != PlaylistSourceType.XTREAM) {
            seriesErrorMessage = "Series disponible con Xtream."
            tvScreen = TvScreen.SERIES
            mobileScreen = MobileScreen.SERIES
            return
        }
        val config = currentXtreamConfigOrNull()
        if (config == null) {
            seriesErrorMessage = "Configura Xtream para ver series."
            tvScreen = TvScreen.SERIES
            mobileScreen = MobileScreen.SERIES
            return
        }
        coroutineScope.launch {
            isLoading = true
            loadingMessage = message
            seriesErrorMessage = null
            if (!networkConnectivity.hasInternetConnection()) {
                seriesErrorMessage = "Sin conexion a Internet"
                tvScreen = TvScreen.SERIES
                mobileScreen = MobileScreen.SERIES
                loadingMessage = null
                isLoading = false
                return@launch
            }
            runCatching { xtreamRepository.loadSeriesCatalog(config) }
                .onSuccess { catalog ->
                    seriesCatalog = catalog.series
                    seriesCategories = catalog.categories
                    selectedSeriesCategoryId = SeriesAllCategory
                    seriesSearchQuery = ""
                    selectedSeriesDetail = null
                    selectedEpisode = null
                    playingEpisode = null
                    episodeProgressMap = emptyMap()
                    seriesErrorMessage = if (catalog.series.isEmpty()) {
                        "La fuente Xtream no contiene series validas."
                    } else {
                        null
                    }
                    tvScreen = TvScreen.SERIES
                    mobileScreen = MobileScreen.SERIES
                }
                .onFailure { error ->
                    seriesErrorMessage = error.message ?: "No se pudo cargar series Xtream."
                    tvScreen = TvScreen.SERIES
                    mobileScreen = MobileScreen.SERIES
                }
            loadingMessage = null
            isLoading = false
        }
    }

    fun openSeries(series: Series) {
        selectedSeriesDetail = seriesDetailCache[series.id]
        selectedSeriesSeasonId = null
        selectedEpisode = null
        seriesErrorMessage = null
        coroutineScope.launch {
            val cached = seriesDetailCache[series.id]
            val detail = if (cached != null) {
                cached
            } else {
                val config = currentXtreamConfigOrNull()
                if (config == null || !networkConnectivity.hasInternetConnection()) {
                    seriesErrorMessage = "No se pudo cargar la ficha de la serie."
                    return@launch
                }
                runCatching { xtreamRepository.loadSeriesInfo(config, series) }
                    .onSuccess { loadedDetail ->
                        seriesDetailCache = seriesDetailCache + (series.id to loadedDetail)
                    }
                    .getOrElse { error ->
                        seriesErrorMessage = error.message ?: "No se pudo cargar la ficha de la serie."
                        return@launch
                    }
            }
            selectedSeriesDetail = detail
            selectedSeriesSeasonId = null
            val episodeIds = detail.episodesBySeasonId.values.flatten().map { it.progressId }.toSet()
            episodeProgressMap = settingsRepository.getEpisodeProgressForIds(episodeIds)
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
        settingsRepository.migrateLegacyConfigIfNeeded()
        val activeConfig = settingsRepository.getActivePlaylistConfig()
        if (activeConfig != null) {
            applyPlaylistConfigToForm(activeConfig)
            if (activeConfig.sourceType == PlaylistSourceType.XTREAM) {
                loadXtream(
                    XtreamConfig(activeConfig.server, activeConfig.username, activeConfig.password),
                    "Conectando con Xtream...",
                    saveConfig = false,
                )
            } else if (activeConfig.playlistUrl.isNotBlank()) {
                loadPlaylist(activeConfig.playlistUrl, "Cargando ultima lista...", saveConfig = false)
            }
            return@LaunchedEffect
        }
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
                    movies = emptyList()
                    movieCategories = emptyList()
                    selectedMovieCategoryId = MovieAllCategory
                    movieSearchQuery = ""
                    selectedMovie = null
                    playingMovie = null
                    movieProgress = null
                    moviePlayerStartPosition = 0L
                    movieErrorMessage = null
                    seriesCatalog = emptyList()
                    seriesCategories = emptyList()
                    selectedSeriesCategoryId = SeriesAllCategory
                    seriesSearchQuery = ""
                    selectedSeriesDetail = null
                    selectedSeriesSeasonId = null
                    selectedEpisode = null
                    playingEpisode = null
                    episodePlayerStartPosition = 0L
                    episodeProgressMap = emptyMap()
                    seriesDetailCache = emptyMap()
                    seriesErrorMessage = null
                    tvScreen = TvScreen.HOME
                    mobileScreen = MobileScreen.HOME
                    tvPreviewChannel = null
                    tvPreviewChannels = emptyList()
                    lastSelectedChannelId = null
                    selectedCategory = AllCategoryName
                    errorMessage = null
                    epgMessage = null
                }
                val moviesEnabled = sourceType == PlaylistSourceType.XTREAM
                val moviesSubtitle = if (moviesEnabled) {
                    if (movies.isEmpty()) "Catalogo VOD" else "${movies.size} peliculas"
                } else {
                    "Disponible con Xtream"
                }
                val seriesEnabled = sourceType == PlaylistSourceType.XTREAM
                val seriesSubtitle = if (seriesEnabled) {
                    if (seriesCatalog.isEmpty()) "Catalogo Series" else "${seriesCatalog.size} series"
                } else {
                    "Disponible con Xtream"
                }
                val accountSummary = xtreamAccountInfo?.expiresAtEpochSeconds
                    ?.let(::formatXtreamDate)
                    ?.let { "Cuenta activa · Expira: $it" }
                val openMoviesFromHome: () -> Unit = {
                    if (movies.isEmpty()) {
                        loadMovies()
                    } else {
                        selectedMovie = null
                        playingMovie = null
                        movieProgress = null
                        tvScreen = TvScreen.MOVIES
                        mobileScreen = MobileScreen.MOVIES
                    }
                }
                val openSeriesFromHome: () -> Unit = {
                    if (seriesCatalog.isEmpty()) {
                        loadSeries()
                    } else {
                        selectedSeriesDetail = null
                        selectedEpisode = null
                        playingEpisode = null
                        tvScreen = TvScreen.SERIES
                        mobileScreen = MobileScreen.SERIES
                    }
                }
                val toggleMovieFavorite: (Movie) -> Unit = { movieToToggle ->
                    coroutineScope.launch {
                        settingsRepository.toggleFavorite(movieToToggle.favoriteId)
                    }
                }
                val playMovie: (Movie, Boolean) -> Unit = { movieToPlay, resume ->
                    playingMovie = movieToPlay
                    moviePlayerStartPosition = if (resume) {
                        movieProgress?.takeIf { it.shouldResume() }?.positionMillis ?: 0L
                    } else {
                        0L
                    }
                    if (!resume) {
                        coroutineScope.launch {
                            settingsRepository.clearMovieProgress(movieToPlay.favoriteId)
                        }
                    }
                }
                val saveMovieProgress: (MovieProgress?) -> Unit = { progress ->
                    val movieId = playingMovie?.favoriteId
                    coroutineScope.launch {
                        if (progress != null) {
                            settingsRepository.saveMovieProgress(progress)
                        } else if (movieId != null) {
                            settingsRepository.clearMovieProgress(movieId)
                        }
                    }
                }
                val toggleSeriesFavorite: (Series) -> Unit = { seriesToToggle ->
                    coroutineScope.launch {
                        settingsRepository.toggleFavorite(seriesToToggle.favoriteId)
                    }
                }
                val selectedEpisodes: List<Episode> = selectedSeriesDetail
                    ?.let { detail -> selectedSeriesSeasonId?.let { detail.episodesBySeasonId[it] } ?: detail.episodesBySeasonId.values.firstOrNull() }
                    .orEmpty()
                val playEpisode: (Episode, Boolean) -> Unit = { episodeToPlay, resume ->
                    playingEpisode = episodeToPlay
                    episodePlayerStartPosition = if (resume) {
                        episodeProgressMap[episodeToPlay.progressId]?.takeIf { it.shouldResume() }?.positionMillis ?: 0L
                    } else {
                        0L
                    }
                    if (!resume) {
                        coroutineScope.launch {
                            settingsRepository.clearEpisodeProgress(episodeToPlay.progressId)
                        }
                    }
                }
                val saveEpisodeProgress: (EpisodeProgress?) -> Unit = { progress ->
                    val episodeId = playingEpisode?.progressId
                    coroutineScope.launch {
                        if (progress != null) {
                            settingsRepository.saveEpisodeProgress(progress)
                            episodeProgressMap = episodeProgressMap + (progress.episodeId to progress)
                        } else if (episodeId != null) {
                            settingsRepository.clearEpisodeProgress(episodeId)
                            episodeProgressMap = episodeProgressMap - episodeId
                        }
                    }
                }
                val playNextEpisode: () -> Unit = {
                    val current = playingEpisode
                    val nextEpisode = current?.let { episode ->
                        val index = selectedEpisodes.indexOfFirst { it.progressId == episode.progressId }
                        selectedEpisodes.getOrNull(index + 1)
                    }
                    if (nextEpisode != null) {
                        playingEpisode = nextEpisode
                        selectedEpisode = nextEpisode
                        episodePlayerStartPosition = 0L
                    }
                }
                val openAddPlaylist: () -> Unit = {
                    editingPlaylistConfig = null
                    currentPlaylistConfigId = null
                    playlistName = ""
                    playlistUrl = ""
                    epgUrl = ""
                    xtreamServer = ""
                    xtreamUsername = ""
                    xtreamPassword = ""
                    sourceType = PlaylistSourceType.M3U
                    showPlaylistConfigEditor = true
                    tvScreen = TvScreen.CONFIG
                    mobileScreen = MobileScreen.CONFIG
                }
                val openEditPlaylist: (PlaylistConfig) -> Unit = { config ->
                    applyPlaylistConfigToForm(config)
                    showPlaylistConfigEditor = true
                    tvScreen = TvScreen.CONFIG
                    mobileScreen = MobileScreen.CONFIG
                }
                val activatePlaylist: (PlaylistConfig) -> Unit = { config ->
                    coroutineScope.launch {
                        settingsRepository.activatePlaylistConfig(config.id)
                        applyPlaylistConfigToForm(config)
                        clearLoadedContent()
                        applyPlaylistConfigToForm(config.copy(isActive = true))
                        if (config.sourceType == PlaylistSourceType.XTREAM) {
                            loadXtream(XtreamConfig(config.server, config.username, config.password), "Conectando con Xtream...", saveConfig = false)
                        } else {
                            loadPlaylist(config.playlistUrl, "Cargando lista...", saveConfig = false)
                        }
                    }
                }
                val deletePlaylist: (PlaylistConfig) -> Unit = { config ->
                    coroutineScope.launch {
                        val nextConfig = settingsRepository.deletePlaylistConfig(config.id)
                        if (config.isActive) {
                            clearLoadedContent()
                            if (nextConfig != null) {
                                applyPlaylistConfigToForm(nextConfig)
                                if (nextConfig.sourceType == PlaylistSourceType.XTREAM) {
                                    loadXtream(
                                        XtreamConfig(nextConfig.server, nextConfig.username, nextConfig.password),
                                        "Conectando con Xtream...",
                                        saveConfig = false,
                                    )
                                } else {
                                    loadPlaylist(nextConfig.playlistUrl, "Cargando lista...", saveConfig = false)
                                }
                            } else {
                                tvScreen = TvScreen.CONFIG
                                mobileScreen = MobileScreen.CONFIG
                            }
                        }
                    }
                }

                if (deviceType == DeviceType.TV) {
                    val previewChannel = tvPreviewChannel
                    when {
                        playingEpisode != null -> EpisodePlayerScreen(
                            episode = playingEpisode!!,
                            startPositionMillis = episodePlayerStartPosition,
                            hasNextEpisode = selectedEpisodes.indexOfFirst { it.progressId == playingEpisode!!.progressId }
                                .let { it >= 0 && it < selectedEpisodes.lastIndex },
                            onSaveProgress = saveEpisodeProgress,
                            onPlayNext = playNextEpisode,
                            onBack = { playingEpisode = null },
                        )
                        selectedEpisode != null -> EpisodeResumeScreen(
                            episode = selectedEpisode!!,
                            progress = episodeProgressMap[selectedEpisode!!.progressId],
                            onPlay = { resume -> playEpisode(selectedEpisode!!, resume) },
                            onBack = { selectedEpisode = null },
                        )
                        selectedSeriesDetail != null && selectedSeriesSeasonId != null -> {
                            val season = selectedSeriesDetail!!.seasons.firstOrNull { it.id == selectedSeriesSeasonId }
                            if (season != null) {
                                SeriesEpisodesScreen(
                                    series = selectedSeriesDetail!!.series,
                                    season = season,
                                    episodes = selectedSeriesDetail!!.episodesBySeasonId[season.id].orEmpty(),
                                    episodeProgress = episodeProgressMap,
                                    isTv = true,
                                    onEpisodeSelected = { selectedEpisode = it },
                                    onBack = { selectedSeriesSeasonId = null },
                                )
                            }
                        }
                        selectedSeriesDetail != null -> SeriesDetailScreen(
                            detail = selectedSeriesDetail!!,
                            episodeProgress = episodeProgressMap,
                            isFavorite = selectedSeriesDetail!!.series.favoriteId in favoriteChannelIds,
                            isTv = true,
                            errorMessage = seriesErrorMessage,
                            onSeasonSelected = { selectedSeriesSeasonId = it.id },
                            onToggleFavorite = { toggleSeriesFavorite(selectedSeriesDetail!!.series) },
                            onBack = {
                                selectedSeriesDetail = null
                                selectedSeriesSeasonId = null
                                selectedEpisode = null
                            },
                        )
                        tvScreen == TvScreen.SERIES -> SeriesCatalogScreen(
                            series = seriesCatalog,
                            categories = seriesCategories,
                            favoriteIds = favoriteChannelIds,
                            selectedCategoryId = selectedSeriesCategoryId,
                            searchQuery = seriesSearchQuery,
                            isTv = true,
                            errorMessage = seriesErrorMessage,
                            continuingProgress = emptyList(),
                            onCategorySelected = { selectedSeriesCategoryId = it },
                            onSearchQueryChange = { seriesSearchQuery = it },
                            onSeriesSelected = ::openSeries,
                            onBack = { tvScreen = TvScreen.HOME },
                        )
                        playingMovie != null -> MoviePlayerScreen(
                            movie = playingMovie!!,
                            startPositionMillis = moviePlayerStartPosition,
                            onSaveProgress = saveMovieProgress,
                            onBack = { playingMovie = null },
                        )
                        selectedMovie != null -> MovieDetailScreen(
                            movie = selectedMovie!!,
                            progress = movieProgress,
                            isFavorite = selectedMovie!!.favoriteId in favoriteChannelIds,
                            isTv = true,
                            onPlay = { resume -> playMovie(selectedMovie!!, resume) },
                            onToggleFavorite = { toggleMovieFavorite(selectedMovie!!) },
                            onBack = { selectedMovie = null },
                        )
                        tvScreen == TvScreen.MOVIES -> MovieCatalogScreen(
                            movies = movies,
                            categories = movieCategories,
                            favoriteIds = favoriteChannelIds,
                            selectedCategoryId = selectedMovieCategoryId,
                            searchQuery = movieSearchQuery,
                            isTv = true,
                            errorMessage = movieErrorMessage,
                            onCategorySelected = { selectedMovieCategoryId = it },
                            onSearchQueryChange = { movieSearchQuery = it },
                            onMovieSelected = ::openMovie,
                            onBack = { tvScreen = TvScreen.HOME },
                        )
                        tvScreen == TvScreen.ACCOUNT -> AccountScreen(
                            sourceType = sourceType,
                            accountInfo = xtreamAccountInfo,
                            isTv = true,
                            onBack = { tvScreen = TvScreen.HOME },
                        )
                        tvScreen == TvScreen.PLAYLISTS -> PlaylistManagerScreen(
                            configs = playlistConfigs,
                            isTv = true,
                            onAdd = openAddPlaylist,
                            onEdit = openEditPlaylist,
                            onActivate = activatePlaylist,
                            onDelete = deletePlaylist,
                            onBack = { tvScreen = TvScreen.HOME },
                        )
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
                            moviesEnabled = moviesEnabled,
                            moviesSubtitle = moviesSubtitle,
                            seriesEnabled = seriesEnabled,
                            seriesSubtitle = seriesSubtitle,
                            accountSummary = accountSummary,
                            onOpenLiveTv = { tvScreen = TvScreen.LIVE_TV },
                            onOpenMovies = openMoviesFromHome,
                            onOpenSeries = openSeriesFromHome,
                            onOpenAccount = { tvScreen = TvScreen.ACCOUNT },
                            onChangePlaylist = { tvScreen = TvScreen.PLAYLISTS },
                        )
                        tvScreen == TvScreen.CONFIG -> PlaylistScreen(
                            playlistName = playlistName,
                            sourceType = sourceType,
                            playlistUrl = playlistUrl,
                            epgUrl = epgUrl,
                            xtreamServer = xtreamServer,
                            xtreamUsername = xtreamUsername,
                            xtreamPassword = xtreamPassword,
                            onPlaylistNameChange = { playlistName = it },
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
                            showPlaylistName = showPlaylistConfigEditor,
                            submitButtonText = if (showPlaylistConfigEditor) "Guardar y cargar" else null,
                            onBack = {
                                if (showPlaylistConfigEditor) {
                                    showPlaylistConfigEditor = false
                                    tvScreen = TvScreen.PLAYLISTS
                                } else {
                                    tvScreen = TvScreen.HOME
                                }
                            },
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
                    when {
                        playingEpisode != null -> EpisodePlayerScreen(
                            episode = playingEpisode!!,
                            startPositionMillis = episodePlayerStartPosition,
                            hasNextEpisode = selectedEpisodes.indexOfFirst { it.progressId == playingEpisode!!.progressId }
                                .let { it >= 0 && it < selectedEpisodes.lastIndex },
                            onSaveProgress = saveEpisodeProgress,
                            onPlayNext = playNextEpisode,
                            onBack = { playingEpisode = null },
                        )
                        selectedEpisode != null -> EpisodeResumeScreen(
                            episode = selectedEpisode!!,
                            progress = episodeProgressMap[selectedEpisode!!.progressId],
                            onPlay = { resume -> playEpisode(selectedEpisode!!, resume) },
                            onBack = { selectedEpisode = null },
                        )
                        selectedSeriesDetail != null && selectedSeriesSeasonId != null -> {
                            val season = selectedSeriesDetail!!.seasons.firstOrNull { it.id == selectedSeriesSeasonId }
                            if (season != null) {
                                SeriesEpisodesScreen(
                                    series = selectedSeriesDetail!!.series,
                                    season = season,
                                    episodes = selectedSeriesDetail!!.episodesBySeasonId[season.id].orEmpty(),
                                    episodeProgress = episodeProgressMap,
                                    isTv = false,
                                    onEpisodeSelected = { selectedEpisode = it },
                                    onBack = { selectedSeriesSeasonId = null },
                                )
                            }
                        }
                        selectedSeriesDetail != null -> SeriesDetailScreen(
                            detail = selectedSeriesDetail!!,
                            episodeProgress = episodeProgressMap,
                            isFavorite = selectedSeriesDetail!!.series.favoriteId in favoriteChannelIds,
                            isTv = false,
                            errorMessage = seriesErrorMessage,
                            onSeasonSelected = { selectedSeriesSeasonId = it.id },
                            onToggleFavorite = { toggleSeriesFavorite(selectedSeriesDetail!!.series) },
                            onBack = {
                                selectedSeriesDetail = null
                                selectedSeriesSeasonId = null
                                selectedEpisode = null
                            },
                        )
                        playingMovie != null -> MoviePlayerScreen(
                            movie = playingMovie!!,
                            startPositionMillis = moviePlayerStartPosition,
                            onSaveProgress = saveMovieProgress,
                            onBack = { playingMovie = null },
                        )
                        selectedMovie != null -> MovieDetailScreen(
                            movie = selectedMovie!!,
                            progress = movieProgress,
                            isFavorite = selectedMovie!!.favoriteId in favoriteChannelIds,
                            isTv = false,
                            onPlay = { resume -> playMovie(selectedMovie!!, resume) },
                            onToggleFavorite = { toggleMovieFavorite(selectedMovie!!) },
                            onBack = { selectedMovie = null },
                        )
                        else -> when (mobileScreen) {
                        MobileScreen.HOME -> MobileHomeScreen(
                            channelCount = channels.size,
                            moviesEnabled = moviesEnabled,
                            moviesSubtitle = moviesSubtitle,
                            seriesEnabled = seriesEnabled,
                            seriesSubtitle = seriesSubtitle,
                            accountSummary = accountSummary,
                            onOpenLiveTv = { mobileScreen = MobileScreen.LIVE_TV },
                            onOpenMovies = openMoviesFromHome,
                            onOpenSeries = openSeriesFromHome,
                            onOpenAccount = { mobileScreen = MobileScreen.ACCOUNT },
                            onOpenPlaylist = { mobileScreen = MobileScreen.PLAYLISTS },
                        )
                        MobileScreen.CONFIG -> PlaylistScreen(
                            playlistName = playlistName,
                            sourceType = sourceType,
                            playlistUrl = playlistUrl,
                            epgUrl = epgUrl,
                            xtreamServer = xtreamServer,
                            xtreamUsername = xtreamUsername,
                            xtreamPassword = xtreamPassword,
                            onPlaylistNameChange = { playlistName = it },
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
                            showPlaylistName = showPlaylistConfigEditor,
                            submitButtonText = if (showPlaylistConfigEditor) "Guardar y cargar" else null,
                            onBack = {
                                if (showPlaylistConfigEditor) {
                                    showPlaylistConfigEditor = false
                                    mobileScreen = MobileScreen.PLAYLISTS
                                } else {
                                    mobileScreen = MobileScreen.HOME
                                }
                            },
                        )
                        MobileScreen.ACCOUNT -> AccountScreen(
                            sourceType = sourceType,
                            accountInfo = xtreamAccountInfo,
                            isTv = false,
                            onBack = { mobileScreen = MobileScreen.HOME },
                        )
                        MobileScreen.PLAYLISTS -> PlaylistManagerScreen(
                            configs = playlistConfigs,
                            isTv = false,
                            onAdd = openAddPlaylist,
                            onEdit = openEditPlaylist,
                            onActivate = activatePlaylist,
                            onDelete = deletePlaylist,
                            onBack = { mobileScreen = MobileScreen.HOME },
                        )
                        MobileScreen.MOVIES -> MovieCatalogScreen(
                            movies = movies,
                            categories = movieCategories,
                            favoriteIds = favoriteChannelIds,
                            selectedCategoryId = selectedMovieCategoryId,
                            searchQuery = movieSearchQuery,
                            isTv = false,
                            errorMessage = movieErrorMessage,
                            onCategorySelected = { selectedMovieCategoryId = it },
                            onSearchQueryChange = { movieSearchQuery = it },
                            onMovieSelected = ::openMovie,
                            onBack = { mobileScreen = MobileScreen.HOME },
                        )
                        MobileScreen.SERIES -> SeriesCatalogScreen(
                            series = seriesCatalog,
                            categories = seriesCategories,
                            favoriteIds = favoriteChannelIds,
                            selectedCategoryId = selectedSeriesCategoryId,
                            searchQuery = seriesSearchQuery,
                            isTv = false,
                            errorMessage = seriesErrorMessage,
                            continuingProgress = emptyList(),
                            onCategorySelected = { selectedSeriesCategoryId = it },
                            onSearchQueryChange = { seriesSearchQuery = it },
                            onSeriesSelected = ::openSeries,
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
    MOVIES,
    SERIES,
    ACCOUNT,
    PLAYLISTS,
    CONFIG,
}

private enum class MobileScreen {
    HOME,
    LIVE_TV,
    MOVIES,
    SERIES,
    ACCOUNT,
    PLAYLISTS,
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
