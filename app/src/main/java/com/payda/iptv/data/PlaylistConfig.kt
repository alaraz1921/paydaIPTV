package com.payda.iptv.data

data class PlaylistConfig(
    val id: String,
    val displayName: String,
    val sourceType: PlaylistSourceType,
    val playlistUrl: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val isActive: Boolean = false,
    val createdAtEpochMillis: Long,
    val lastUsedAtEpochMillis: Long? = null,
    val status: PlaylistConfigStatus = PlaylistConfigStatus.UNKNOWN,
)

enum class PlaylistConfigStatus {
    UNKNOWN,
    ACTIVE,
    EXPIRED,
    AVAILABLE,
    ERROR,
}

fun PlaylistConfig.summary(): String {
    return when (sourceType) {
        PlaylistSourceType.M3U -> runCatching { java.net.URL(playlistUrl).host }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: playlistUrl
        PlaylistSourceType.XTREAM -> runCatching { java.net.URL(server).host }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: server
    }
}
