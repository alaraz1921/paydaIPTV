package com.payda.iptv.data

data class Channel(
    val name: String,
    val streamUrl: String,
    val group: String? = null,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
)

fun Channel.stableFavoriteId(): String {
    return tvgId?.trim()?.takeIf { it.isNotEmpty() }
        ?: streamUrl.trim().takeIf { it.isNotEmpty() }
        ?: "${name.trim()}|${streamUrl.trim()}"
}
