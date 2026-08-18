package com.payda.iptv.data

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M3uRepository(
    private val parser: M3uParser = M3uParser(),
    private val networkLogger: M3uNetworkLogger = AndroidM3uNetworkLogger,
    private val timeoutMillis: Int = TimeoutMillis,
) {
    suspend fun loadChannels(playlistUrl: String): List<Channel> = withContext(Dispatchers.IO) {
        val content = downloadPlaylist(playlistUrl)
        parser.parse(content)
    }

    private fun downloadPlaylist(playlistUrl: String): String {
        val visitedUrls = mutableSetOf<String>()
        var currentUrl = URL(playlistUrl)

        repeat(MaxRedirects + 1) { redirectCount ->
            val connection = currentUrl.openConnection()
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("User-Agent", UserAgent)

            if (connection !is HttpURLConnection) {
                return connection.getInputStream().bufferedReader().use { it.readText() }
            }

            connection.instanceFollowRedirects = false
            val safeUrl = sanitizeUrl(currentUrl)
            networkLogger.logRequest(safeUrl, currentUrl.protocol)

            try {
                val responseCode = connection.responseCode
                networkLogger.logResponse(safeUrl, responseCode)

                if (responseCode in RedirectCodes) {
                    val location = connection.getHeaderField("Location")
                    val nextUrl = location?.let { URL(currentUrl, it) }
                    networkLogger.logRedirect(safeUrl, sanitizeUrl(nextUrl), responseCode)

                    if (nextUrl == null) {
                        throw PlaylistLoadException("La lista redirige a una URL no valida.")
                    }
                    if (!visitedUrls.add(nextUrl.toExternalForm())) {
                        throw PlaylistLoadException("La lista tiene una redireccion circular.")
                    }
                    if (redirectCount == MaxRedirects) {
                        throw PlaylistLoadException("La lista tiene demasiadas redirecciones.")
                    }

                    currentUrl = nextUrl
                    return@repeat
                }

                if (responseCode !in 200..299) {
                    throw PlaylistLoadException("El servidor respondio con codigo HTTP $responseCode.")
                }

                return connection.inputStream.bufferedReader().use { it.readText() }
            } catch (error: PlaylistLoadException) {
                networkLogger.logError(safeUrl, error)
                throw error
            } catch (error: Exception) {
                val mappedError = PlaylistLoadException(friendlyMessageForNetworkError(error), error)
                networkLogger.logError(safeUrl, error)
                throw mappedError
            } finally {
                connection.disconnect()
            }
        }

        throw PlaylistLoadException("La lista tiene demasiadas redirecciones.")
    }

    private fun sanitizeUrl(url: URL?): String {
        if (url == null) return "URL no disponible"
        val base = StringBuilder()
            .append(url.protocol)
            .append("://")
            .append(url.host)
        if (url.port != -1) {
            base.append(":").append(url.port)
        }
        base.append(url.path)

        val query = url.query
        if (!query.isNullOrBlank()) {
            val safeQuery = query.split("&").joinToString("&") { parameter ->
                val key = parameter.substringBefore("=", missingDelimiterValue = parameter)
                if (SensitiveQueryKeys.contains(key.lowercase())) {
                    "$key=***"
                } else {
                    parameter
                }
            }
            base.append("?").append(safeQuery)
        }

        return base.toString()
    }

    private companion object {
        const val TimeoutMillis = 15_000
        const val MaxRedirects = 5
        const val UserAgent = "PayDaIPTV/0.1 (Android)"
        val RedirectCodes = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308,
        )
        val SensitiveQueryKeys = setOf(
            "username",
            "password",
            "user",
            "pass",
            "token",
            "key",
            "api_key",
            "apikey",
            "auth",
            "signature",
        )
    }
}

internal fun friendlyMessageForNetworkError(error: Exception): String = when (error) {
    is SSLPeerUnverifiedException -> "El certificado del servidor no es valido."
    is SSLHandshakeException -> "No se ha podido establecer una conexion segura con el servidor."
    is SSLProtocolException -> "El servidor no admite una conexion TLS compatible."
    is SSLException -> "No se ha podido establecer una conexion segura con el servidor."
    is UnknownHostException -> "No se ha podido encontrar el servidor."
    is SocketTimeoutException -> "La conexion con el servidor ha agotado el tiempo de espera."
    is java.net.ConnectException -> "No se ha podido conectar con el servidor."
    is IOException -> "No se ha podido descargar la lista M3U."
    else -> "No se ha podido cargar la lista M3U."
}

class PlaylistLoadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

interface M3uNetworkLogger {
    fun logRequest(url: String, protocol: String)
    fun logResponse(url: String, responseCode: Int)
    fun logRedirect(fromUrl: String, toUrl: String, responseCode: Int)
    fun logError(url: String, error: Throwable)
}

private object AndroidM3uNetworkLogger : M3uNetworkLogger {
    override fun logRequest(url: String, protocol: String) {
        Log.d(Tag, "Request playlist url=$url protocol=${protocol.uppercase()}")
    }

    override fun logResponse(url: String, responseCode: Int) {
        Log.d(Tag, "Response playlist url=$url httpCode=$responseCode")
    }

    override fun logRedirect(fromUrl: String, toUrl: String, responseCode: Int) {
        Log.d(Tag, "Redirect playlist code=$responseCode from=$fromUrl to=$toUrl")
    }

    override fun logError(url: String, error: Throwable) {
        Log.e(
            Tag,
            "Playlist download failed url=$url type=${error::class.java.name} message=${error.message}",
            error,
        )
    }

    private const val Tag = "M3uRepository"
}
