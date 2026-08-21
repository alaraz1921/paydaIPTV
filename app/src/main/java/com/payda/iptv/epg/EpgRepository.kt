package com.payda.iptv.epg

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class EpgRepository(
    private val parser: XmlTvParser = XmlTvParser(),
) {
    suspend fun loadEpg(epgUrl: String): EpgData = withContext(Dispatchers.IO) {
        if (epgUrl.isBlank()) {
            throw EpgLoadException("Introduce una URL EPG valida.")
        }

        val content = downloadEpg(epgUrl)
        val epgData = try {
            parser.parse(content)
        } catch (error: XmlTvParseException) {
            throw EpgLoadException(error.message ?: "XMLTV invalido.", error)
        }
        if (epgData.programmesByChannelId.isEmpty()) {
            throw EpgLoadException("El XMLTV no contiene programas.")
        }
        epgData
    }

    private fun downloadEpg(epgUrl: String): String {
        val url = runCatching { URL(epgUrl) }
            .getOrElse { throw EpgLoadException("URL EPG invalida.", it) }
        val deadlineNanos = System.nanoTime() + CallTimeoutMillis * NanosPerMillisecond
        val connection = url.openConnection()
        connection.connectTimeout = timeoutForDeadline(ConnectTimeoutMillis, deadlineNanos)
        connection.readTimeout = timeoutForDeadline(ReadTimeoutMillis, deadlineNanos)
        connection.setRequestProperty("User-Agent", "PayDaIPTV/0.1 (Android)")

        return try {
            if (connection is HttpURLConnection) {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw EpgLoadException("El servidor EPG respondio con codigo HTTP $responseCode.")
                }
                connection.readTimeout = timeoutForDeadline(ReadTimeoutMillis, deadlineNanos)
            }
            connection.getInputStream().bufferedReader().use { it.readText() }
        } catch (error: SocketTimeoutException) {
            throw EpgLoadException("El servidor EPG esta tardando demasiado en responder.", error)
        } catch (error: UnknownHostException) {
            throw EpgLoadException("No se pudo encontrar el servidor de la EPG.", error)
        } catch (error: XmlTvParseException) {
            throw EpgLoadException(error.message ?: "XMLTV invalido.", error)
        } catch (error: EpgLoadException) {
            throw error
        } catch (error: IOException) {
            throw EpgLoadException("No se pudo descargar la EPG.", error)
        } finally {
            if (connection is HttpURLConnection) {
                connection.disconnect()
            }
        }
    }

    private fun timeoutForDeadline(
        configuredTimeoutMillis: Int,
        deadlineNanos: Long,
    ): Int {
        val remainingMillis = (deadlineNanos - System.nanoTime()) / NanosPerMillisecond
        return min(configuredTimeoutMillis.toLong(), remainingMillis)
            .coerceAtLeast(1)
            .toInt()
    }

    private companion object {
        const val ConnectTimeoutMillis = 20_000
        const val ReadTimeoutMillis = 60_000
        const val CallTimeoutMillis = 90_000L
        const val NanosPerMillisecond = 1_000_000L
    }
}

class EpgLoadException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
