package com.payda.iptv.data

class M3uParser {
    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingChannel: PendingChannel? = null

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        pendingChannel = parseExtInf(line)
                    }
                    line.startsWith("#") -> Unit
                    pendingChannel != null -> {
                        channels += pendingChannel!!.toChannel(streamUrl = line)
                        pendingChannel = null
                    }
                }
            }

        return channels
    }

    private fun parseExtInf(line: String): PendingChannel {
        val payload = line.substringAfter(":", missingDelimiterValue = "")
        val commaIndex = findInfoSeparator(payload)
        val info = if (commaIndex >= 0) payload.substring(0, commaIndex) else payload
        val displayName = if (commaIndex >= 0) payload.substring(commaIndex + 1).trim() else ""
        val attributes = parseAttributes(info)
        val tvgName = attributes["tvg-name"]?.takeIf { it.isNotBlank() }
        val name = displayName.takeIf { it.isNotBlank() }
            ?: tvgName
            ?: "Canal sin nombre"

        return PendingChannel(
            name = name,
            group = attributes["group-title"]?.takeIf { it.isNotBlank() },
            logoUrl = attributes["tvg-logo"]?.takeIf { it.isNotBlank() },
            tvgId = attributes["tvg-id"]?.takeIf { it.isNotBlank() },
            tvgName = tvgName,
        )
    }

    private fun findInfoSeparator(value: String): Int {
        var insideQuotes = false
        value.forEachIndexed { index, character ->
            when (character) {
                '"' -> insideQuotes = !insideQuotes
                ',' -> if (!insideQuotes) return index
            }
        }
        return -1
    }

    private fun parseAttributes(info: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        AttributeRegex.findAll(info).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            attributes[key] = value
        }
        return attributes
    }

    private data class PendingChannel(
        val name: String,
        val group: String?,
        val logoUrl: String?,
        val tvgId: String?,
        val tvgName: String?,
    ) {
        fun toChannel(streamUrl: String): Channel = Channel(
            name = name,
            streamUrl = streamUrl,
            group = group,
            logoUrl = logoUrl,
            tvgId = tvgId,
            tvgName = tvgName,
        )
    }

    private companion object {
        val AttributeRegex = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")
    }
}
