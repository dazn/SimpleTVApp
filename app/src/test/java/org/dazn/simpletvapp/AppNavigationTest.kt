package org.dazn.simpletvapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationTest {

    private fun parseQueryParam(route: String, key: String): String {
        val query = route.substringAfter('?', "")
        return query.split("&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            ?: ""
    }

    private fun parsePathArg(route: String): String = parseQueryParam(route, "path")
    private fun parseFormatArg(route: String): String = parseQueryParam(route, "format")

    // Returns the decoded value, or null if the key is absent from the route.
    private fun parseOptionalQueryParam(route: String, key: String): String? {
        val query = route.substringAfter('?', "")
        return query.split("&")
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }

    @Test
    fun ampersandInPath() {
        val path = "Movies/AC&DC/song.mp3"
        val route = buildPlayerRoute(path, "mp3")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun emojiInPath() {
        val path = "🎬/film.mp4"
        val route = buildPlayerRoute(path, "mp4")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun questionMarkInPath() {
        val path = "Music/What?/track.flac"
        val route = buildPlayerRoute(path, "flac")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun hashInPath() {
        val path = "Music/#1 Hit.mp3"
        val route = buildPlayerRoute(path, "mp3")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun equalsInPath() {
        val path = "a=b/file.mp4"
        val route = buildPlayerRoute(path, "mp4")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun percentLiteralInPath() {
        val path = "Music/100% Pure/song.mp3"
        val route = buildPlayerRoute(path, "mp3")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun spaceInPath() {
        val path = "My Movies/film name.mp4"
        val route = buildPlayerRoute(path, "mp4")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun slashPreservation() {
        val path = "a/b/c.mp4"
        val route = buildPlayerRoute(path, "mp4")
        assertEquals(path, parsePathArg(route))
    }

    @Test
    fun simplePath() {
        val path = "movies/film.mp4"
        val route = buildPlayerRoute(path, "mp4")
        assertEquals(path, parsePathArg(route))
        assertEquals("mp4", parseFormatArg(route))
    }

    @Test
    fun emptyPathAndFormat() {
        val route = buildPlayerRoute("", "")
        assertEquals("", parsePathArg(route))
        assertEquals("", parseFormatArg(route))
    }

    @Test
    fun `space encoded as percent20 not plus`() {
        val route = buildPlayerRoute("My Movies/film name.mp4", "mp4")
        assert(!route.contains("+")) { "Route must use %20 not + for spaces (Uri.decode doesn't handle +)" }
        assert(route.contains("%20")) { "Route must contain %20 for spaces" }
    }

    @Test
    fun aspectRatioColonRoundtrips() {
        val route = buildPlayerRoute("films/movie.mp4", "mp4", displayAspectRatio = "16:9")
        assertEquals("16:9", parseOptionalQueryParam(route, "displayAspectRatio"))
    }

    @Test
    fun allOptionalParamsRoundtrip() {
        val route = buildPlayerRoute("f.mp4", "mp4", "16:9", "h264", "aac")
        assertEquals("16:9", parseOptionalQueryParam(route, "displayAspectRatio"))
        assertEquals("h264", parseOptionalQueryParam(route, "videoCodec"))
        assertEquals("aac", parseOptionalQueryParam(route, "audioCodec"))
    }

    @Test
    fun nullOptionalParamsAbsentFromRoute() {
        val route = buildPlayerRoute("f.mp4", "mp4")
        assertNull(parseOptionalQueryParam(route, "displayAspectRatio"))
        assertNull(parseOptionalQueryParam(route, "videoCodec"))
        assertNull(parseOptionalQueryParam(route, "audioCodec"))
    }
}
