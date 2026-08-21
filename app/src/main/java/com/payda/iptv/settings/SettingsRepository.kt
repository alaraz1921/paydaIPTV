package com.payda.iptv.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.payda.iptv.data.EpisodeProgress
import com.payda.iptv.data.MovieProgress
import com.payda.iptv.data.PlaylistConfig
import com.payda.iptv.data.PlaylistConfigStatus
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.data.XtreamConfig
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.payDaSettingsDataStore by preferencesDataStore(name = "payda_settings")

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.payDaSettingsDataStore

    val favoriteChannelIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[FavoriteChannelIdsKey].orEmpty()
    }

    val playlistConfigs: Flow<List<PlaylistConfig>> = dataStore.data.map { preferences ->
        preferences[PlaylistConfigsKey].orEmpty()
            .mapNotNull(::decodePlaylistConfig)
            .sortedWith(compareByDescending<PlaylistConfig> { it.isActive }.thenBy { it.displayName.lowercase() })
    }

    suspend fun getLastPlaylistUrl(): String? {
        return dataStore.data.first()[LastPlaylistUrlKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun getLastEpgUrl(): String? {
        return dataStore.data.first()[LastEpgUrlKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun getPlaylistSourceType(): PlaylistSourceType {
        val value = dataStore.data.first()[PlaylistSourceTypeKey]
        return runCatching { PlaylistSourceType.valueOf(value.orEmpty()) }
            .getOrDefault(PlaylistSourceType.M3U)
    }

    suspend fun getXtreamConfig(): XtreamConfig? {
        val preferences = dataStore.data.first()
        val server = preferences[XtreamServerKey]?.takeIf { it.isNotBlank() }
        val username = preferences[XtreamUsernameKey]?.takeIf { it.isNotBlank() }
        val password = preferences[XtreamPasswordKey]?.takeIf { it.isNotBlank() }
        return if (server != null && username != null && password != null) {
            XtreamConfig(server = server, username = username, password = password)
        } else {
            null
        }
    }

    suspend fun getPlaylistConfigs(): List<PlaylistConfig> {
        migrateLegacyConfigIfNeeded()
        return dataStore.data.first()[PlaylistConfigsKey].orEmpty()
            .mapNotNull(::decodePlaylistConfig)
            .sortedWith(compareByDescending<PlaylistConfig> { it.isActive }.thenBy { it.displayName.lowercase() })
    }

    suspend fun getActivePlaylistConfig(): PlaylistConfig? {
        return getPlaylistConfigs().firstOrNull { it.isActive }
    }

    suspend fun getMovieProgress(movieId: String): MovieProgress? {
        return dataStore.data.first()[MovieProgressKey]
            .orEmpty()
            .mapNotNull(::decodeMovieProgress)
            .firstOrNull { it.movieId == movieId }
    }

    suspend fun getEpisodeProgress(episodeId: String): EpisodeProgress? {
        return dataStore.data.first()[EpisodeProgressKey]
            .orEmpty()
            .mapNotNull(::decodeEpisodeProgress)
            .firstOrNull { it.episodeId == episodeId }
    }

    suspend fun getEpisodeProgressForIds(episodeIds: Set<String>): Map<String, EpisodeProgress> {
        if (episodeIds.isEmpty()) return emptyMap()
        return dataStore.data.first()[EpisodeProgressKey]
            .orEmpty()
            .mapNotNull(::decodeEpisodeProgress)
            .filter { it.episodeId in episodeIds }
            .associateBy { it.episodeId }
    }

    suspend fun saveLastPlaylistUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[LastPlaylistUrlKey] = url
        }
    }

    suspend fun savePlaylistSourceType(sourceType: PlaylistSourceType) {
        dataStore.edit { preferences ->
            preferences[PlaylistSourceTypeKey] = sourceType.name
        }
    }

    suspend fun saveXtreamConfig(config: XtreamConfig) {
        dataStore.edit { preferences ->
            preferences[XtreamServerKey] = config.server
            preferences[XtreamUsernameKey] = config.username
            preferences[XtreamPasswordKey] = config.password
        }
    }

    suspend fun upsertPlaylistConfig(config: PlaylistConfig, makeActive: Boolean = config.isActive) {
        dataStore.edit { preferences ->
            val existing = preferences[PlaylistConfigsKey].orEmpty()
                .mapNotNull(::decodePlaylistConfig)
                .filterNot { it.id == config.id }
            val updated = if (makeActive) {
                existing.map { it.copy(isActive = false) } + config.copy(isActive = true, lastUsedAtEpochMillis = System.currentTimeMillis())
            } else {
                existing + config
            }
            preferences[PlaylistConfigsKey] = updated.map(::encodePlaylistConfig).toSet()
            updated.firstOrNull { it.isActive }?.let { saveLegacyFields(preferences, it) }
        }
    }

    suspend fun activatePlaylistConfig(configId: String): PlaylistConfig? {
        var activeConfig: PlaylistConfig? = null
        dataStore.edit { preferences ->
            val now = System.currentTimeMillis()
            val configs = preferences[PlaylistConfigsKey].orEmpty()
                .mapNotNull(::decodePlaylistConfig)
                .map { config ->
                    if (config.id == configId) {
                        config.copy(isActive = true, lastUsedAtEpochMillis = now).also { activeConfig = it }
                    } else {
                        config.copy(isActive = false)
                    }
                }
            preferences[PlaylistConfigsKey] = configs.map(::encodePlaylistConfig).toSet()
            activeConfig?.let { saveLegacyFields(preferences, it) }
        }
        return activeConfig
    }

    suspend fun deletePlaylistConfig(configId: String): PlaylistConfig? {
        var nextActive: PlaylistConfig? = null
        dataStore.edit { preferences ->
            val configs = preferences[PlaylistConfigsKey].orEmpty()
                .mapNotNull(::decodePlaylistConfig)
            val removedActive = configs.firstOrNull { it.id == configId }?.isActive == true
            val remaining = configs.filterNot { it.id == configId }
            val updated = if (removedActive && remaining.isNotEmpty()) {
                val replacement = remaining.maxBy { it.lastUsedAtEpochMillis ?: it.createdAtEpochMillis }
                    .copy(isActive = true, lastUsedAtEpochMillis = System.currentTimeMillis())
                nextActive = replacement
                remaining.map { if (it.id == replacement.id) replacement else it.copy(isActive = false) }
            } else {
                remaining
            }
            preferences[PlaylistConfigsKey] = updated.map(::encodePlaylistConfig).toSet()
            updated.firstOrNull { it.isActive }?.let { saveLegacyFields(preferences, it) }
        }
        return nextActive
    }

    suspend fun migrateLegacyConfigIfNeeded() {
        dataStore.edit { preferences ->
            if (!preferences[PlaylistConfigsKey].isNullOrEmpty()) return@edit
            val sourceType = runCatching { PlaylistSourceType.valueOf(preferences[PlaylistSourceTypeKey].orEmpty()) }
                .getOrDefault(PlaylistSourceType.M3U)
            val now = System.currentTimeMillis()
            val legacyConfig = when (sourceType) {
                PlaylistSourceType.M3U -> preferences[LastPlaylistUrlKey]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { url ->
                        PlaylistConfig(
                            id = "playlist-$now",
                            displayName = defaultPlaylistName(sourceType, url),
                            sourceType = PlaylistSourceType.M3U,
                            playlistUrl = url,
                            epgUrl = preferences[LastEpgUrlKey].orEmpty(),
                            isActive = true,
                            createdAtEpochMillis = now,
                            lastUsedAtEpochMillis = now,
                            status = PlaylistConfigStatus.UNKNOWN,
                        )
                    }
                PlaylistSourceType.XTREAM -> {
                    val server = preferences[XtreamServerKey]
                    val username = preferences[XtreamUsernameKey]
                    val password = preferences[XtreamPasswordKey]
                    if (!server.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
                        PlaylistConfig(
                            id = "playlist-$now",
                            displayName = defaultPlaylistName(sourceType, server),
                            sourceType = PlaylistSourceType.XTREAM,
                            server = server,
                            username = username,
                            password = password,
                            epgUrl = preferences[LastEpgUrlKey].orEmpty(),
                            isActive = true,
                            createdAtEpochMillis = now,
                            lastUsedAtEpochMillis = now,
                            status = PlaylistConfigStatus.UNKNOWN,
                        )
                    } else {
                        null
                    }
                }
            }
            if (legacyConfig != null) {
                preferences[PlaylistConfigsKey] = setOf(encodePlaylistConfig(legacyConfig))
            }
        }
    }

    suspend fun saveLastEpgUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[LastEpgUrlKey] = url
        }
    }

    suspend fun clearLastEpgUrl() {
        dataStore.edit { preferences ->
            preferences.remove(LastEpgUrlKey)
        }
    }

    suspend fun toggleFavorite(channelId: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[FavoriteChannelIdsKey].orEmpty()
            preferences[FavoriteChannelIdsKey] = if (channelId in currentFavorites) {
                currentFavorites - channelId
            } else {
                currentFavorites + channelId
            }
        }
    }

    suspend fun saveMovieProgress(progress: MovieProgress) {
        dataStore.edit { preferences ->
            val encodedMovieId = encodeStoredId(progress.movieId)
            val currentProgress = preferences[MovieProgressKey].orEmpty()
                .filterNot { it.substringBefore("|") == encodedMovieId }
                .toSet()
            preferences[MovieProgressKey] = currentProgress + encodeMovieProgress(progress)
        }
    }

    suspend fun clearMovieProgress(movieId: String) {
        dataStore.edit { preferences ->
            val encodedMovieId = encodeStoredId(movieId)
            preferences[MovieProgressKey] = preferences[MovieProgressKey].orEmpty()
                .filterNot { it.substringBefore("|") == encodedMovieId }
                .toSet()
        }
    }

    suspend fun saveEpisodeProgress(progress: EpisodeProgress) {
        dataStore.edit { preferences ->
            val encodedEpisodeId = encodeStoredId(progress.episodeId)
            val currentProgress = preferences[EpisodeProgressKey].orEmpty()
                .filterNot { it.substringBefore("|") == encodedEpisodeId }
                .toSet()
            preferences[EpisodeProgressKey] = currentProgress + encodeEpisodeProgress(progress)
        }
    }

    suspend fun clearEpisodeProgress(episodeId: String) {
        dataStore.edit { preferences ->
            val encodedEpisodeId = encodeStoredId(episodeId)
            preferences[EpisodeProgressKey] = preferences[EpisodeProgressKey].orEmpty()
                .filterNot { it.substringBefore("|") == encodedEpisodeId }
                .toSet()
        }
    }

    private fun encodeMovieProgress(progress: MovieProgress): String {
        return "${encodeStoredId(progress.movieId)}|${progress.positionMillis}|${progress.durationMillis}"
    }

    private fun decodeMovieProgress(value: String): MovieProgress? {
        val parts = value.split("|")
        if (parts.size != 3) return null
        return MovieProgress(
            movieId = decodeStoredId(parts[0]),
            positionMillis = parts[1].toLongOrNull() ?: return null,
            durationMillis = parts[2].toLongOrNull() ?: return null,
        )
    }

    private fun encodeEpisodeProgress(progress: EpisodeProgress): String {
        return listOf(
            encodeStoredId(progress.episodeId),
            progress.positionMillis,
            progress.durationMillis,
            if (progress.watched) 1 else 0,
            progress.updatedAtEpochMillis,
        ).joinToString("|")
    }

    private fun decodeEpisodeProgress(value: String): EpisodeProgress? {
        val parts = value.split("|")
        if (parts.size != 5) return null
        return EpisodeProgress(
            episodeId = decodeStoredId(parts[0]),
            positionMillis = parts[1].toLongOrNull() ?: return null,
            durationMillis = parts[2].toLongOrNull() ?: return null,
            watched = parts[3] == "1",
            updatedAtEpochMillis = parts[4].toLongOrNull() ?: return null,
        )
    }

    private fun encodeStoredId(id: String): String {
        return URLEncoder.encode(id, Charsets.UTF_8.name())
    }

    private fun decodeStoredId(id: String): String {
        return URLDecoder.decode(id, Charsets.UTF_8.name())
    }

    private fun encodePlaylistConfig(config: PlaylistConfig): String {
        return JSONObject()
            .put("id", config.id)
            .put("displayName", config.displayName)
            .put("sourceType", config.sourceType.name)
            .put("playlistUrl", config.playlistUrl)
            .put("server", config.server)
            .put("username", config.username)
            .put("password", config.password)
            .put("epgUrl", config.epgUrl)
            .put("isActive", config.isActive)
            .put("createdAt", config.createdAtEpochMillis)
            .put("lastUsedAt", config.lastUsedAtEpochMillis ?: 0L)
            .put("status", config.status.name)
            .toString()
    }

    private fun decodePlaylistConfig(value: String): PlaylistConfig? {
        return runCatching {
            val json = JSONObject(value)
            PlaylistConfig(
                id = json.optString("id").takeIf { it.isNotBlank() } ?: return null,
                displayName = json.optString("displayName").takeIf { it.isNotBlank() } ?: "Mi playlist",
                sourceType = runCatching { PlaylistSourceType.valueOf(json.optString("sourceType")) }
                    .getOrDefault(PlaylistSourceType.M3U),
                playlistUrl = json.optString("playlistUrl"),
                server = json.optString("server"),
                username = json.optString("username"),
                password = json.optString("password"),
                epgUrl = json.optString("epgUrl"),
                isActive = json.optBoolean("isActive", false),
                createdAtEpochMillis = json.optLong("createdAt", System.currentTimeMillis()),
                lastUsedAtEpochMillis = json.optLong("lastUsedAt").takeIf { it > 0 },
                status = runCatching { PlaylistConfigStatus.valueOf(json.optString("status")) }
                    .getOrDefault(PlaylistConfigStatus.UNKNOWN),
            )
        }.getOrNull()
    }

    private fun saveLegacyFields(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        config: PlaylistConfig,
    ) {
        preferences[PlaylistSourceTypeKey] = config.sourceType.name
        if (config.epgUrl.isBlank()) {
            preferences.remove(LastEpgUrlKey)
        } else {
            preferences[LastEpgUrlKey] = config.epgUrl
        }
        when (config.sourceType) {
            PlaylistSourceType.M3U -> {
                preferences[LastPlaylistUrlKey] = config.playlistUrl
            }
            PlaylistSourceType.XTREAM -> {
                preferences[XtreamServerKey] = config.server
                preferences[XtreamUsernameKey] = config.username
                preferences[XtreamPasswordKey] = config.password
            }
        }
    }

    private fun defaultPlaylistName(sourceType: PlaylistSourceType, value: String): String {
        val host = runCatching { java.net.URL(value).host }.getOrNull()
        return host?.takeIf { it.isNotBlank() } ?: when (sourceType) {
            PlaylistSourceType.M3U -> "Mi playlist"
            PlaylistSourceType.XTREAM -> "Mi Xtream"
        }
    }

    private companion object {
        val LastPlaylistUrlKey = stringPreferencesKey("last_playlist_url")
        val LastEpgUrlKey = stringPreferencesKey("last_epg_url")
        val PlaylistSourceTypeKey = stringPreferencesKey("playlist_source_type")
        val XtreamServerKey = stringPreferencesKey("xtream_server")
        val XtreamUsernameKey = stringPreferencesKey("xtream_username")
        val XtreamPasswordKey = stringPreferencesKey("xtream_password")
        val FavoriteChannelIdsKey = stringSetPreferencesKey("favorite_channel_ids")
        val MovieProgressKey = stringSetPreferencesKey("movie_progress")
        val EpisodeProgressKey = stringSetPreferencesKey("episode_progress")
        val PlaylistConfigsKey = stringSetPreferencesKey("playlist_configs")
    }
}
