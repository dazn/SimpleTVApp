package org.dazn.simpletvapp.data.network

import org.dazn.simpletvapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaApiClientTest {

    @Test
    fun encodesPathSegments() {
        val encoded = encodePath("foo bar/baz qux")
        assertEquals("foo%20bar/baz%20qux", encoded)
    }

    @Test
    fun encodesSpecialChars() {
        val encoded = encodePath("music/AC&DC")
        assertEquals("music/AC%26DC", encoded)
    }

    @Test
    fun encodesEmptyPath() {
        val encoded = encodePath("")
        assertEquals("", encoded)
    }

    @Test
    fun buildsStreamUrlForFile() {
        val url = buildStreamUrl("videos/movie.mp4")
        assertEquals("${BuildConfig.BASE_URL}/objects/videos/movie.mp4", url)
    }

    @Test
    fun buildsStreamUrlForRootPath() {
        val url = buildStreamUrl("")
        assertEquals("${BuildConfig.BASE_URL}/objects/", url)
    }

    @Test
    fun encodesEmojiInPath() {
        val encoded = encodePath("🎬/film.mp4")
        // Emoji is multi-byte UTF-8; slashes are preserved, emoji is percent-encoded
        assert(!encoded.contains("🎬")) { "Emoji should be percent-encoded" }
        assert(encoded.endsWith("/film.mp4")) { "Slash and filename should be preserved" }
    }

    @Test
    fun encodesQuestionMark() {
        val encoded = encodePath("Music/What?/track.flac")
        assertEquals("Music/What%3F/track.flac", encoded)
    }

    @Test
    fun encodesHash() {
        val encoded = encodePath("Music/#1 Hit.mp3")
        assertEquals("Music/%231%20Hit.mp3", encoded)
    }

    @Test
    fun encodesPercentLiteral() {
        val encoded = encodePath("100% Pure/song.mp3")
        assertEquals("100%25%20Pure/song.mp3", encoded)
    }

    @Test
    fun encodesUnicodeJapanese() {
        val encoded = encodePath("映画/film.mp4")
        assert(!encoded.contains("映画")) { "Japanese characters should be percent-encoded" }
        assert(encoded.endsWith("/film.mp4")) { "Slash and filename should be preserved" }
    }

    @Test
    fun `encodePath encodes literal plus as %2B`() {
        val result = encodePath("music/AC+DC.mp3")
        assertEquals("music/AC%2BDC.mp3", result)
    }

    @Test
    fun parsesMediaItemsWithEncodedPaths() {
        val json = """
            [
              {"path":"Bag+Raiders+-+Shooting+Stars+%5BO-MQC_G9jTU%5D.mp3","type":"file","size":1234,"content_type":"audio/mpeg","streamFormat":"mp3"}
            ]
        """.trimIndent()
        val items = parseMediaItems(json)
        assertEquals(1, items.size)
        assertEquals("Bag Raiders - Shooting Stars [O-MQC_G9jTU].mp3", items[0].path)
        assertEquals("Bag Raiders - Shooting Stars [O-MQC_G9jTU].mp3", items[0].name)
    }

    @Test
    fun parsesDirectoryListing() {
        val json = """
            [
              {"path":"Movies","type":"directory","size":null},
              {"path":"clip.mp4","type":"file","size":1048576,"content_type":"video/mp4","streamFormat":"mp4"}
            ]
        """.trimIndent()
        val items = parseMediaItems(json)
        assertEquals(2, items.size)
        assertEquals("Movies", items[0].name)
        assertEquals("directory", items[0].type)
        assertEquals(null, items[0].streamFormat)
        assertEquals("clip.mp4", items[1].name)
        assertEquals("file", items[1].type)
        assertEquals("mp4", items[1].streamFormat)
    }
}
