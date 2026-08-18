package com.payda.iptv.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class M3uRepositoryTest {
    private lateinit var server: HttpServer
    private lateinit var baseUrl: String

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        baseUrl = "http://127.0.0.1:${server.address.port}"
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun loadsValidHttpPlaylist() = runBlocking {
        server.createContext("/valid.m3u") { exchange ->
            exchange.respond(
                """
                #EXTM3U
                #EXTINF:-1 group-title="Test",HTTP Channel
                http://127.0.0.1/video.m3u8
                """.trimIndent(),
            )
        }

        val channels = repository().loadChannels("$baseUrl/valid.m3u")

        assertEquals(1, channels.size)
        assertEquals("HTTP Channel", channels[0].name)
        assertEquals("Test", channels[0].group)
    }

    @Test
    fun followsHttpRedirect() = runBlocking {
        server.createContext("/redirect.m3u") { exchange ->
            exchange.responseHeaders.add("Location", "/valid-after-redirect.m3u")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/valid-after-redirect.m3u") { exchange ->
            exchange.respond(
                """
                #EXTM3U
                #EXTINF:-1,Redirect Channel
                http://127.0.0.1/redirect-video.m3u8
                """.trimIndent(),
            )
        }

        val channels = repository().loadChannels("$baseUrl/redirect.m3u")

        assertEquals(1, channels.size)
        assertEquals("Redirect Channel", channels[0].name)
    }

    @Test
    fun returnsFriendlyErrorForHttpStatusError() = runBlocking {
        server.createContext("/missing.m3u") { exchange ->
            exchange.respond("Not found", responseCode = 404)
        }

        val result = runCatching {
            repository().loadChannels("$baseUrl/missing.m3u")
        }

        assertTrue(result.isFailure)
        assertEquals("El servidor respondio con codigo HTTP 404.", result.exceptionOrNull()?.message)
    }

    @Test
    fun returnsFailureWhenUrlDoesNotRespond() = runBlocking {
        val result = runCatching {
            repository().loadChannels("http://127.0.0.1:9/not-found.m3u")
        }

        assertTrue(result.isFailure)
        assertEquals("No se ha podido conectar con el servidor.", result.exceptionOrNull()?.message)
    }

    @Test
    fun returnsFriendlyErrorForTimeout() = runBlocking {
        server.createContext("/slow.m3u") { exchange ->
            Thread.sleep(500)
            exchange.respond("#EXTM3U")
        }

        val result = runCatching {
            repository(timeoutMillis = 100).loadChannels("$baseUrl/slow.m3u")
        }

        assertTrue(result.isFailure)
        assertEquals(
            "La conexion con el servidor ha agotado el tiempo de espera.",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun mapsTlsAndDnsErrorsToFriendlyMessages() {
        assertEquals(
            "No se ha podido establecer una conexion segura con el servidor.",
            friendlyMessageForNetworkError(SSLHandshakeException("Handshake failed")),
        )
        assertEquals(
            "El certificado del servidor no es valido.",
            friendlyMessageForNetworkError(SSLPeerUnverifiedException("Hostname mismatch")),
        )
        assertEquals(
            "No se ha podido encontrar el servidor.",
            friendlyMessageForNetworkError(UnknownHostException("missing.example")),
        )
        assertEquals(
            "La conexion con el servidor ha agotado el tiempo de espera.",
            friendlyMessageForNetworkError(SocketTimeoutException("timeout")),
        )
    }

    private fun repository(timeoutMillis: Int = 1_000): M3uRepository = M3uRepository(
        networkLogger = NoOpNetworkLogger,
        timeoutMillis = timeoutMillis,
    )

    private fun HttpExchange.respond(body: String, responseCode: Int = 200) {
        val bytes = body.toByteArray()
        sendResponseHeaders(responseCode, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    private object NoOpNetworkLogger : M3uNetworkLogger {
        override fun logRequest(url: String, protocol: String) = Unit
        override fun logResponse(url: String, responseCode: Int) = Unit
        override fun logRedirect(fromUrl: String, toUrl: String, responseCode: Int) = Unit
        override fun logError(url: String, error: Throwable) = Unit
    }
}
