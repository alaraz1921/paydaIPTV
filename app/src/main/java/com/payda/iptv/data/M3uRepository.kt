package com.payda.iptv.data

import android.os.Build
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class M3uRepository(
    private val parser: M3uParser = M3uParser(),
    private val networkLogger: M3uNetworkLogger = AndroidM3uNetworkLogger,
    private val connectTimeoutMillis: Int = ConnectTimeoutMillis,
    private val readTimeoutMillis: Int = ReadTimeoutMillis,
    private val callTimeoutMillis: Long = CallTimeoutMillis,
) {
    suspend fun loadChannels(playlistUrl: String): List<Channel> = withContext(Dispatchers.IO) {
        loadPlaylist(playlistUrl).channels
    }

    suspend fun loadPlaylist(playlistUrl: String): M3uPlaylist = withContext(Dispatchers.IO) {
        val content = downloadPlaylist(playlistUrl)
        parser.parsePlaylist(content)
    }

    private fun downloadPlaylist(playlistUrl: String): String {
        val visitedUrls = mutableSetOf<String>()
        var currentUrl = URL(playlistUrl)
        val callDeadlineNanos = System.nanoTime() + callTimeoutMillis * NanosPerMillisecond

        repeat(MaxRedirects + 1) { redirectCount ->
            var stage = NetworkOperationStage.CONNECTING
            checkCallTimeout(currentUrl, callDeadlineNanos)
            val connection = currentUrl.openConnection()
            connection.connectTimeout = timeoutForDeadline(connectTimeoutMillis, callDeadlineNanos)
            connection.readTimeout = timeoutForDeadline(readTimeoutMillis, callDeadlineNanos)
            connection.setRequestProperty("User-Agent", UserAgent)

            if (connection !is HttpURLConnection) {
                stage = NetworkOperationStage.READING
                connection.readTimeout = timeoutForDeadline(readTimeoutMillis, callDeadlineNanos)
                return connection.getInputStream().bufferedReader().use { it.readText() }
            }

            connection.instanceFollowRedirects = false
            val safeUrl = sanitizeUrl(currentUrl)
            networkLogger.logRequest(safeUrl, currentUrl.protocol)

            try {
                val responseCode = connection.responseCode
                networkLogger.logResponse(safeUrl, responseCode)
                checkCallTimeout(currentUrl, callDeadlineNanos)

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

                stage = NetworkOperationStage.READING
                connection.readTimeout = timeoutForDeadline(readTimeoutMillis, callDeadlineNanos)
                return connection.inputStream.bufferedReader().use { it.readText() }
            } catch (error: PlaylistLoadException) {
                networkLogger.logError(
                    buildNetworkErrorDiagnostics(
                        url = currentUrl,
                        error = error,
                        stage = if (error is PlaylistCallTimeoutException) {
                            NetworkOperationStage.CALL_TIMEOUT
                        } else {
                            stage
                        },
                    ),
                )
                throw error
            } catch (error: Exception) {
                val mappedError = PlaylistLoadException(friendlyMessageForNetworkError(error), error)
                networkLogger.logError(
                    buildNetworkErrorDiagnostics(
                        url = currentUrl,
                        error = error,
                        stage = stage,
                    ),
                )
                throw mappedError
            } finally {
                connection.disconnect()
            }
        }

        throw PlaylistLoadException("La lista tiene demasiadas redirecciones.")
    }

    private fun timeoutForDeadline(
        configuredTimeoutMillis: Int,
        deadlineNanos: Long,
    ): Int {
        val remainingMillis = remainingCallMillis(deadlineNanos)
        if (remainingMillis <= 0) {
            return 1
        }
        return min(configuredTimeoutMillis.toLong(), remainingMillis).coerceAtLeast(1).toInt()
    }

    private fun checkCallTimeout(url: URL, deadlineNanos: Long) {
        if (remainingCallMillis(deadlineNanos) <= 0) {
            throw PlaylistCallTimeoutException(sanitizeUrl(url))
        }
    }

    private fun remainingCallMillis(deadlineNanos: Long): Long {
        return (deadlineNanos - System.nanoTime()) / NanosPerMillisecond
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

    private fun buildNetworkErrorDiagnostics(
        url: URL,
        error: Throwable,
        stage: NetworkOperationStage,
    ): NetworkErrorDiagnostics {
        return NetworkErrorDiagnostics(
            sanitizedUrl = sanitizeUrl(url),
            protocol = url.protocol.uppercase(),
            host = url.host,
            port = if (url.port != -1) url.port else url.defaultPort,
            stage = stage.name,
            timeoutKind = classifyTimeout(error, stage),
            androidVersion = "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
            sslProvider = runCatching {
                SSLContext.getDefault().provider.name
            }.getOrDefault("No disponible"),
            exceptionClass = error::class.java.name,
            exceptionType = error::class.java.simpleName,
            message = error.message.orEmpty(),
            rootCauseClass = rootCauseOf(error)::class.java.name,
            rootCauseMessage = rootCauseOf(error).message.orEmpty(),
            causeChain = error.causeChainDescription(),
            tlsFailureKind = classifyTlsFailure(error),
        )
    }

    private companion object {
        const val ConnectTimeoutMillis = 20_000
        const val ReadTimeoutMillis = 60_000
        const val CallTimeoutMillis = 90_000L
        const val NanosPerMillisecond = 1_000_000L
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

internal fun classifyTimeout(
    error: Throwable,
    stage: NetworkOperationStage,
): String? = when {
    error is PlaylistCallTimeoutException -> "CALL_TIMEOUT"
    stage == NetworkOperationStage.CALL_TIMEOUT -> "CALL_TIMEOUT"
    error !is SocketTimeoutException -> null
    stage == NetworkOperationStage.CONNECTING -> "CONNECT_TIMEOUT"
    stage == NetworkOperationStage.READING -> "READ_TIMEOUT"
    else -> "SOCKET_TIMEOUT"
}

internal fun classifyTlsFailure(error: Throwable): String? {
    if (error !is SSLException && error.causeChain().none { it is SSLException }) {
        return null
    }

    val causes = error.causeChain()
    val messages = causes.joinToString(" ") { it.message.orEmpty() }.lowercase()

    return when {
        causes.any { it is CertificateExpiredException } -> "CERTIFICATE_EXPIRED"
        causes.any { it is SSLPeerUnverifiedException } -> "HOSTNAME_NOT_VERIFIED"
        causes.any { it is CertPathValidatorException } -> "CERTIFICATE_NOT_TRUSTED"
        causes.any { it is CertificateException } -> "CERTIFICATE_ERROR"
        "protocol_version" in messages || "tlsv1" in messages -> "TLS_VERSION_INCOMPATIBLE"
        "handshake_failure" in messages -> "HANDSHAKE_FAILURE"
        "connection closed" in messages || "closed" in messages -> "CONNECTION_CLOSED_DURING_HANDSHAKE"
        causes.any { it is SocketTimeoutException } -> "TLS_TIMEOUT"
        error is SSLHandshakeException -> "SSL_HANDSHAKE_FAILED"
        else -> "SSL_ERROR"
    }
}

private fun Throwable.rootCauseOfSelf(): Throwable = cause?.rootCauseOfSelf() ?: this

private fun rootCauseOf(error: Throwable): Throwable = error.rootCauseOfSelf()

private fun Throwable.causeChain(): List<Throwable> {
    val causes = mutableListOf<Throwable>()
    var current: Throwable? = this
    while (current != null && current !in causes) {
        causes += current
        current = current.cause
    }
    return causes
}

private fun Throwable.causeChainDescription(): String {
    return causeChain().joinToString(" -> ") { cause ->
        "${cause::class.java.simpleName}: ${cause.message.orEmpty()}"
    }
}

open class PlaylistLoadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

private class PlaylistCallTimeoutException(
    sanitizedUrl: String,
) : PlaylistLoadException("La descarga de la lista ha agotado el tiempo total de espera: $sanitizedUrl")

interface M3uNetworkLogger {
    fun logRequest(url: String, protocol: String)
    fun logResponse(url: String, responseCode: Int)
    fun logRedirect(fromUrl: String, toUrl: String, responseCode: Int)
    fun logError(diagnostics: NetworkErrorDiagnostics)
}

internal enum class NetworkOperationStage {
    CONNECTING,
    READING,
    CALL_TIMEOUT,
}

data class NetworkErrorDiagnostics(
    val sanitizedUrl: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val stage: String,
    val timeoutKind: String?,
    val androidVersion: String,
    val sslProvider: String,
    val exceptionClass: String,
    val exceptionType: String,
    val message: String,
    val rootCauseClass: String,
    val rootCauseMessage: String,
    val causeChain: String,
    val tlsFailureKind: String?,
)

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

    override fun logError(diagnostics: NetworkErrorDiagnostics) {
        Log.e(
            Tag,
            """
            Playlist download failed
            Url: ${diagnostics.sanitizedUrl}
            Protocol: ${diagnostics.protocol}
            Host: ${diagnostics.host}
            Port: ${diagnostics.port}
            Stage: ${diagnostics.stage}
            Timeout kind: ${diagnostics.timeoutKind ?: "N/A"}
            Android: ${diagnostics.androidVersion}
            SSL provider: ${diagnostics.sslProvider}
            Exception: ${diagnostics.exceptionType}
            Exception class: ${diagnostics.exceptionClass}
            TLS failure kind: ${diagnostics.tlsFailureKind ?: "N/A"}
            Root cause: ${diagnostics.rootCauseClass}: ${diagnostics.rootCauseMessage}
            Cause chain: ${diagnostics.causeChain}
            Message: ${diagnostics.message}
            """.trimIndent(),
        )
    }

    private const val Tag = "M3uRepository"
}
