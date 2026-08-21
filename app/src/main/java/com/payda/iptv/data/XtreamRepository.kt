package com.payda.iptv.data

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.min

class XtreamRepository {
    suspend fun loadLiveData(config: XtreamConfig): XtreamLiveData = withContext(Dispatchers.IO) {
        val normalizedConfig = config.copy(server = normalizeXtreamServer(config.server))
        val accountJson = requestObject(normalizedConfig)
        val accountInfo = parseAndValidateAccount(accountJson)
        val categories = requestArray(normalizedConfig, "get_live_categories")
            .let(::parseCategories)
        val categoryNames = categories.associate { it.id to it.name }
        val channels = requestArray(normalizedConfig, "get_live_streams")
            .let { parseLiveStreams(normalizedConfig, categoryNames, it) }

        XtreamLiveData(
            channels = channels,
            categories = categories,
            accountInfo = accountInfo,
        )
    }

    suspend fun loadMovieCatalog(config: XtreamConfig): MovieCatalog = withContext(Dispatchers.IO) {
        val normalizedConfig = config.copy(server = normalizeXtreamServer(config.server))
        val categories = requestArray(normalizedConfig, "get_vod_categories")
            .let(::parseMovieCategories)
        val categoryNames = categories.associate { it.id to it.name }
        val movies = requestArray(normalizedConfig, "get_vod_streams")
            .let { parseVodStreams(normalizedConfig, categoryNames, it) }

        MovieCatalog(
            movies = movies,
            categories = categories,
        )
    }

    suspend fun loadMovieInfo(config: XtreamConfig, movie: Movie): Movie = withContext(Dispatchers.IO) {
        val normalizedConfig = config.copy(server = normalizeXtreamServer(config.server))
        val root = requestObject(normalizedConfig, "get_vod_info&vod_id=${urlEncode(movie.id)}")
        val info = root.optJSONObject("info") ?: root
        movie.copy(
            plot = info.optString("plot").takeIf { it.isNotBlank() } ?: movie.plot,
            cast = info.optString("cast").takeIf { it.isNotBlank() } ?: movie.cast,
            director = info.optString("director").takeIf { it.isNotBlank() } ?: movie.director,
            genre = info.optString("genre").takeIf { it.isNotBlank() } ?: movie.genre,
            releaseDate = info.optString("releasedate").takeIf { it.isNotBlank() }
                ?: info.optString("releaseDate").takeIf { it.isNotBlank() }
                ?: movie.releaseDate,
            duration = info.optString("duration").takeIf { it.isNotBlank() } ?: movie.duration,
            rating = info.optFlexibleString("rating")?.takeIf { it.isNotBlank() } ?: movie.rating,
            posterUrl = info.optString("movie_image").takeIf { it.isNotBlank() }
                ?: info.optString("cover_big").takeIf { it.isNotBlank() }
                ?: movie.posterUrl,
        )
    }

    private fun parseAndValidateAccount(root: JSONObject): XtreamAccountInfo {
        val userInfo = root.optJSONObject("user_info")
            ?: throw XtreamLoadException("Respuesta del servidor no valida.")
        val auth = userInfo.optFlexibleString("auth")
        val status = userInfo.optString("status", "")
        val expiresAt = userInfo.optFlexibleLong("exp_date")

        if (auth == "0") {
            throw XtreamLoadException("Credenciales incorrectas.")
        }
        if (status.equals("Expired", ignoreCase = true) ||
            (expiresAt != null && expiresAt > 0 && expiresAt < System.currentTimeMillis() / 1_000L)
        ) {
            throw XtreamLoadException("Cuenta expirada.")
        }
        if (
            status.equals("Disabled", ignoreCase = true) ||
            status.equals("Banned", ignoreCase = true) ||
            status.equals("Inactive", ignoreCase = true)
        ) {
            throw XtreamLoadException("Cuenta deshabilitada.")
        }
        if (!status.equals("Active", ignoreCase = true) && auth != "1") {
            throw XtreamLoadException("Credenciales incorrectas.")
        }

        return XtreamAccountInfo(
            username = userInfo.optFlexibleString("username"),
            status = status.takeIf { it.isNotBlank() },
            expiresAtEpochSeconds = expiresAt,
            createdAtEpochSeconds = userInfo.optFlexibleLong("created_at"),
            isTrial = userInfo.optFlexibleString("is_trial")?.let { it == "1" || it.equals("true", ignoreCase = true) },
            maxConnections = userInfo.optFlexibleInt("max_connections"),
            activeConnections = userInfo.optFlexibleInt("active_cons"),
            server = root.optJSONObject("server_info")?.optFlexibleString("url")
                ?: root.optJSONObject("server_info")?.optFlexibleString("server_protocol")
                    ?.let { protocol ->
                        val host = root.optJSONObject("server_info")?.optFlexibleString("url")
                        val port = root.optJSONObject("server_info")?.optFlexibleString("port")
                        if (host.isNullOrBlank()) null else "$protocol://$host${port?.let { ":$it" }.orEmpty()}"
                    },
        )
    }

    private fun parseCategories(array: JSONArray): List<XtreamCategory> {
        return buildList {
            for (index in 0 until array.length()) {
                val category = array.optJSONObject(index) ?: continue
                val id = category.optString("category_id").takeIf { it.isNotBlank() } ?: continue
                add(
                    XtreamCategory(
                        id = id,
                        name = category.optString("category_name").takeIf { it.isNotBlank() }
                            ?: "Categoria $id",
                    ),
                )
            }
        }
    }

    private fun parseMovieCategories(array: JSONArray): List<MovieCategory> {
        return buildList {
            for (index in 0 until array.length()) {
                val category = array.optJSONObject(index) ?: continue
                val id = category.optString("category_id").takeIf { it.isNotBlank() } ?: continue
                add(
                    MovieCategory(
                        id = id,
                        name = category.optString("category_name").takeIf { it.isNotBlank() }
                            ?: "Categoria $id",
                    ),
                )
            }
        }
    }

    private fun parseLiveStreams(
        config: XtreamConfig,
        categoryNames: Map<String, String>,
        array: JSONArray,
    ): List<Channel> {
        return buildList {
            for (index in 0 until array.length()) {
                val stream = array.optJSONObject(index) ?: continue
                val streamId = stream.optFlexibleString("stream_id")?.takeIf { it.isNotBlank() } ?: continue
                val name = stream.optString("name").takeIf { it.isNotBlank() } ?: "Canal $streamId"
                val categoryId = stream.optFlexibleString("category_id")
                val extension = stream.optString("container_extension")
                    .takeIf { it.isNotBlank() }
                    ?: "ts"
                val streamUrl = buildLiveStreamUrl(config, streamId, extension)
                add(
                    Channel(
                        name = name,
                        streamUrl = streamUrl,
                        group = categoryId?.let { categoryNames[it] },
                        logoUrl = stream.optString("stream_icon").takeIf { it.isNotBlank() },
                        tvgId = stream.optString("epg_channel_id").takeIf { it.isNotBlank() },
                        tvgName = name,
                        favoriteId = "xtream|${config.server}|$streamId",
                    ),
                )
            }
        }
    }

    private fun parseVodStreams(
        config: XtreamConfig,
        categoryNames: Map<String, String>,
        array: JSONArray,
    ): List<Movie> {
        return buildList {
            for (index in 0 until array.length()) {
                val stream = array.optJSONObject(index) ?: continue
                val streamId = stream.optFlexibleString("stream_id")?.takeIf { it.isNotBlank() } ?: continue
                val name = stream.optString("name").takeIf { it.isNotBlank() } ?: "Pelicula $streamId"
                val categoryId = stream.optFlexibleString("category_id")
                val extension = stream.optString("container_extension")
                    .takeIf { it.isNotBlank() }
                    ?: "mp4"
                val streamUrl = buildMovieStreamUrl(config, streamId, extension)
                add(
                    Movie(
                        id = streamId,
                        name = name,
                        streamUrl = streamUrl,
                        posterUrl = stream.optString("stream_icon").takeIf { it.isNotBlank() },
                        categoryId = categoryId,
                        categoryName = categoryId?.let { categoryNames[it] },
                        containerExtension = extension,
                        rating = stream.optFlexibleString("rating_5based")?.takeIf { it.isNotBlank() }
                            ?: stream.optFlexibleString("rating")?.takeIf { it.isNotBlank() },
                        year = stream.optFlexibleString("year")?.takeIf { it.isNotBlank() },
                        plot = stream.optString("plot").takeIf { it.isNotBlank() },
                        favoriteId = "xtream|movie|${config.server}|$streamId",
                    ),
                )
            }
        }
    }

    private fun requestObject(config: XtreamConfig, action: String? = null): JSONObject {
        val body = download(buildPlayerApiUrl(config, action))
        return try {
            JSONObject(body)
        } catch (error: JSONException) {
            throw XtreamLoadException("Respuesta del servidor no valida.", error)
        }
    }

    private fun requestArray(config: XtreamConfig, action: String): JSONArray {
        val body = download(buildPlayerApiUrl(config, action))
        return try {
            JSONArray(body)
        } catch (error: JSONException) {
            throw XtreamLoadException("Respuesta del servidor no valida.", error)
        }
    }

    private fun buildPlayerApiUrl(config: XtreamConfig, action: String? = null): String {
        val base = "${config.server}/player_api.php?username=${urlEncode(config.username)}&password=${urlEncode(config.password)}"
        return if (action.isNullOrBlank()) base else "$base&action=$action"
    }

    private fun buildLiveStreamUrl(
        config: XtreamConfig,
        streamId: String,
        extension: String,
    ): String {
        return "${config.server}/live/${urlEncode(config.username)}/${urlEncode(config.password)}/$streamId.$extension"
    }

    private fun buildMovieStreamUrl(
        config: XtreamConfig,
        streamId: String,
        extension: String,
    ): String {
        return "${config.server}/movie/${urlEncode(config.username)}/${urlEncode(config.password)}/$streamId.$extension"
    }

    private fun download(requestUrl: String): String {
        val url = URL(requestUrl)
        val deadlineNanos = System.nanoTime() + CallTimeoutMillis * NanosPerMillisecond
        val connection = url.openConnection()
        connection.connectTimeout = timeoutForDeadline(ConnectTimeoutMillis, deadlineNanos)
        connection.readTimeout = timeoutForDeadline(ReadTimeoutMillis, deadlineNanos)
        connection.setRequestProperty("User-Agent", "PayDaIPTV/0.1 (Android)")
        Log.d(Tag, "Request Xtream url=${sanitizeXtreamUrl(url)}")

        return try {
            if (connection is HttpURLConnection) {
                val responseCode = connection.responseCode
                Log.d(Tag, "Response Xtream url=${sanitizeXtreamUrl(url)} httpCode=$responseCode")
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw XtreamLoadException("Credenciales incorrectas.")
                }
                if (responseCode !in 200..299) {
                    throw XtreamLoadException("El servidor respondio con codigo HTTP $responseCode.")
                }
                connection.readTimeout = timeoutForDeadline(ReadTimeoutMillis, deadlineNanos)
            }
            connection.getInputStream().bufferedReader().use { it.readText() }
        } catch (error: XtreamLoadException) {
            throw error
        } catch (error: UnknownHostException) {
            throw XtreamLoadException("No se ha podido encontrar el servidor.", error)
        } catch (error: SocketTimeoutException) {
            throw XtreamLoadException("El servidor esta tardando demasiado en responder.", error)
        } catch (error: IOException) {
            throw XtreamLoadException("Servidor no accesible.", error)
        } finally {
            if (connection is HttpURLConnection) {
                connection.disconnect()
            }
        }
    }

    private fun timeoutForDeadline(configuredTimeoutMillis: Int, deadlineNanos: Long): Int {
        val remainingMillis = (deadlineNanos - System.nanoTime()) / NanosPerMillisecond
        return min(configuredTimeoutMillis.toLong(), remainingMillis)
            .coerceAtLeast(1)
            .toInt()
    }

    private fun sanitizeXtreamUrl(url: URL): String {
        return "${url.protocol}://${url.host}${if (url.port != -1) ":${url.port}" else ""}${url.path}"
    }

    private companion object {
        const val ConnectTimeoutMillis = 20_000
        const val ReadTimeoutMillis = 60_000
        const val CallTimeoutMillis = 90_000L
        const val NanosPerMillisecond = 1_000_000L
        const val Tag = "XtreamRepository"
    }
}

fun normalizeXtreamServer(server: String): String {
    val trimmedServer = server.trim().trimEnd('/')
    val url = runCatching { URL(trimmedServer) }
        .getOrElse { throw XtreamLoadException("Servidor Xtream invalido.", it) }
    if (url.protocol != "http" && url.protocol != "https") {
        throw XtreamLoadException("Servidor Xtream invalido.")
    }
    return buildString {
        append(url.protocol)
        append("://")
        append(url.host)
        if (url.port != -1) {
            append(":")
            append(url.port)
        }
    }
}

private fun urlEncode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

private fun JSONObject.optFlexibleString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return opt(key)?.toString()
}

private fun JSONObject.optFlexibleLong(key: String): Long? {
    return optFlexibleString(key)?.toLongOrNull()
}

private fun JSONObject.optFlexibleInt(key: String): Int? {
    return optFlexibleString(key)?.toIntOrNull()
}

class XtreamLoadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)
