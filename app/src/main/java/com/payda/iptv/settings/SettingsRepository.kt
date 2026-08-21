package com.payda.iptv.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    suspend fun saveLastPlaylistUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[LastPlaylistUrlKey] = url
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

    private companion object {
        val LastPlaylistUrlKey = stringPreferencesKey("last_playlist_url")
        val LastEpgUrlKey = stringPreferencesKey("last_epg_url")
        val FavoriteChannelIdsKey = stringSetPreferencesKey("favorite_channel_ids")
    }
}
