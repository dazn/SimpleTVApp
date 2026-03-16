package org.dazn.simpletvapp.data.repository

import org.dazn.simpletvapp.data.model.MediaItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaRepositoryTest {

    private fun makeRepo(items: List<MediaItem>) =
        MediaRepository(networkFetcher = { items })

    @Test
    fun sortsDirectoriesBeforeFiles() = runTest {
        val items = listOf(
            MediaItem(path = "zoo.mp4", type = "file", streamFormat = "mp4"),
            MediaItem(path = "Aardvark", type = "directory"),
            MediaItem(path = "alpha.mp3", type = "file", streamFormat = "mp3"),
            MediaItem(path = "Beta", type = "directory")
        )
        val repo = makeRepo(items)
        val result = repo.listDirectory("")
        assertEquals("directory", result[0].type)
        assertEquals("directory", result[1].type)
        assertEquals("file", result[2].type)
        assertEquals("file", result[3].type)
    }

    @Test
    fun sortsByNameWithinEachGroup() = runTest {
        val items = listOf(
            MediaItem(path = "Zebra", type = "directory"),
            MediaItem(path = "zoo.mp4", type = "file", streamFormat = "mp4"),
            MediaItem(path = "Apple", type = "directory"),
            MediaItem(path = "alpha.mp3", type = "file", streamFormat = "mp3")
        )
        val repo = makeRepo(items)
        val result = repo.listDirectory("")
        assertEquals("Apple", result[0].name)
        assertEquals("Zebra", result[1].name)
        assertEquals("alpha.mp3", result[2].name)
        assertEquals("zoo.mp4", result[3].name)
    }
}
