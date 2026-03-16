package org.dazn.simpletvapp.data.network

import org.dazn.simpletvapp.BuildConfig
import org.dazn.simpletvapp.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

private val BASE_URL = BuildConfig.BASE_URL
private val API_KEY = BuildConfig.API_KEY

private val json = Json { ignoreUnknownKeys = true }

private fun decodeMediaPath(path: String): String {
    if (path.isEmpty()) return ""
    return path.split("/").joinToString("/") { seg ->
        try { URLDecoder.decode(seg, "UTF-8") } catch (_: IllegalArgumentException) { seg }
    }
}

fun parseMediaItems(jsonString: String): List<org.dazn.simpletvapp.data.model.MediaItem> =
    json.decodeFromString<List<org.dazn.simpletvapp.data.model.MediaItem>>(jsonString)
        .map { item -> item.copy(path = decodeMediaPath(item.path)) }

fun encodePath(path: String): String {
    if (path.isEmpty()) return ""
    return path.split("/").joinToString("/") { seg ->
        URLEncoder.encode(seg, "UTF-8").replace("+", "%20")
    }
}

fun buildStreamUrl(path: String): String {
    val encoded = encodePath(path)
    return if (encoded.isEmpty()) "$BASE_URL/objects/" else "$BASE_URL/objects/$encoded"
}

suspend fun listDirectory(path: String): List<MediaItem> = withContext(Dispatchers.IO) {
    val encoded = encodePath(path)
    val urlStr = if (encoded.isEmpty()) "$BASE_URL/objects/" else "$BASE_URL/objects/$encoded/"
    val url = URL(urlStr)
    val conn = url.openConnection() as HttpURLConnection
    try {
        conn.setRequestProperty("Authorization", "Bearer $API_KEY")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        val responseText = conn.inputStream.bufferedReader().readText()
        parseMediaItems(responseText)
    } finally {
        conn.disconnect()
    }
}
