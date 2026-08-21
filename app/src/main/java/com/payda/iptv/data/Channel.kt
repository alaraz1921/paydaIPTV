package com.payda.iptv.data

data class Channel(
    val name: String,
    val streamUrl: String,
    val group: String? = null,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val favoriteId: String? = null,
)

fun Channel.stableFavoriteId(): String {
    favoriteId?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val normalizedStreamUrl = streamUrl.trim()
    val prefix = tvgId?.trim()?.takeIf { it.isNotEmpty() }
        ?: name.trim().lowercase()
    return "$prefix|$normalizedStreamUrl"
}

enum class PlaylistSourceType {
    M3U,
    XTREAM,
}
