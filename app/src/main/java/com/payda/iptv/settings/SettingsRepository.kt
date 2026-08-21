package com.payda.iptv.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.payda.iptv.data.MovieProgress
import com.payda.iptv.data.PlaylistSourceType
import com.payda.iptv.data.XtreamConfig
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.payDaSettingsDataStore by preferencesDataStore(name = "payda_settings")

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.payDaSettingsDataStore

    val favoriteChannelIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[FavoriteChannelIdsKey].orEmpty()
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

    suspend fun getMovieProgress(movieId: String): MovieProgress? {
        return dataStore.data.first()[MovieProgressKey]
            .orEmpty()
            .mapNotNull(::decodeMovieProgress)
            .firstOrNull { it.movieId == movieId }
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
            val encodedMovieId = encodeMovieId(progress.movieId)
            val currentProgress = preferences[MovieProgressKey].orEmpty()
                .filterNot { it.substringBefore("|") == encodedMovieId }
                .toSet()
            preferences[MovieProgressKey] = currentProgress + encodeMovieProgress(progress)
        }
    }

    suspend fun clearMovieProgress(movieId: String) {
        dataStore.edit { preferences ->
            val encodedMovieId = encodeMovieId(movieId)
            preferences[MovieProgressKey] = preferences[MovieProgressKey].orEmpty()
                .filterNot { it.substringBefore("|") == encodedMovieId }
                .toSet()
        }
    }

    private fun encodeMovieProgress(progress: MovieProgress): String {
        return "${encodeMovieId(progress.movieId)}|${progress.positionMillis}|${progress.durationMillis}"
    }

    private fun decodeMovieProgress(value: String): MovieProgress? {
        val parts = value.split("|")
        if (parts.size != 3) return null
        return MovieProgress(
            movieId = decodeMovieId(parts[0]),
            positionMillis = parts[1].toLongOrNull() ?: return null,
            durationMillis = parts[2].toLongOrNull() ?: return null,
        )
    }

    private fun encodeMovieId(movieId: String): String {
        return URLEncoder.encode(movieId, Charsets.UTF_8.name())
    }

    private fun decodeMovieId(movieId: String): String {
        return URLDecoder.decode(movieId, Charsets.UTF_8.name())
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
    }
}
