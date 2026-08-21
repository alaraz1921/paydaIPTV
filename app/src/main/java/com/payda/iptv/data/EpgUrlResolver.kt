package com.payda.iptv.data

import java.net.URL

internal fun resolveEpgUrl(
    manualUrl: String,
    manualConfigured: Boolean,
    detectedUrl: String?,
): EpgUrlResolution {
    val trimmedManualUrl = manualUrl.trim()
    val usableManualUrl = trimmedManualUrl
        .takeIf { manualConfigured && isUsableEpgUrl(it) }
    val usableDetectedUrl = detectedUrl
        ?.trim()
        ?.takeIf(::isUsableEpgUrl)

    return when {
        usableManualUrl != null -> EpgUrlResolution(
            url = usableManualUrl,
            source = EpgUrlSource.MANUAL,
            ignoredManualUrl = false,
        )
        usableDetectedUrl != null -> EpgUrlResolution(
            url = usableDetectedUrl,
            source = EpgUrlSource.AUTO,
            ignoredManualUrl = false,
        )
        else -> EpgUrlResolution(
            url = null,
            source = null,
            ignoredManualUrl = trimmedManualUrl.isNotBlank() && manualConfigured,
        )
    }
}

internal fun isUsableEpgUrl(value: String): Boolean {
    val trimmedValue = value.trim()
    if (trimmedValue.isBlank()) return false
    if (trimmedValue == "http://" || trimmedValue == "https://") return false
    if (trimmedValue.startsWith("<") && trimmedValue.endsWith(">")) return false
    if (trimmedValue.contains("placeholder", ignoreCase = true)) return false
    if (trimmedValue.contains("example.com", ignoreCase = true)) return false

    return runCatching {
        val url = URL(trimmedValue)
        url.protocol == "http" || url.protocol == "https"
    }.getOrDefault(false)
}

internal data class EpgUrlResolution(
    val url: String?,
    val source: EpgUrlSource?,
    val ignoredManualUrl: Boolean,
)

internal enum class EpgUrlSource {
    MANUAL,
    AUTO,
}
