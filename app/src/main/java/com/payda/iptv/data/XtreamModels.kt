package com.payda.iptv.data

data class XtreamConfig(
    val server: String,
    val username: String,
    val password: String,
)

data class XtreamCategory(
    val id: String,
    val name: String,
)

data class XtreamAccountInfo(
    val username: String? = null,
    val status: String?,
    val expiresAtEpochSeconds: Long?,
    val createdAtEpochSeconds: Long? = null,
    val isTrial: Boolean? = null,
    val maxConnections: Int?,
    val activeConnections: Int?,
    val server: String? = null,
)

data class XtreamLiveData(
    val channels: List<Channel>,
    val categories: List<XtreamCategory>,
    val accountInfo: XtreamAccountInfo?,
)
