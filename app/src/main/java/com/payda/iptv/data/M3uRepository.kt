package com.payda.iptv.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M3uRepository(
    private val parser: M3uParser = M3uParser(),
) {
    suspend fun loadChannels(playlistUrl: String): List<Channel> = withContext(Dispatchers.IO) {
        val content = downloadPlaylist(playlistUrl)
        parser.parse(content)
    }

    private fun downloadPlaylist(playlistUrl: String): String {
        val connection = URL(playlistUrl).openConnection()
        connection.connectTimeout = TimeoutMillis
        connection.readTimeout = TimeoutMillis
        connection.setRequestProperty("User-Agent", "PayDaIPTV/1.0")

        return try {
            if (connection is HttpURLConnection) {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("El servidor respondio con codigo HTTP $responseCode")
                }
            }

            connection.getInputStream().bufferedReader().use { it.readText() }
        } finally {
            if (connection is HttpURLConnection) {
                connection.disconnect()
            }
        }
    }

    private companion object {
        const val TimeoutMillis = 15_000
    }
}
