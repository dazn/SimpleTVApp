package org.dazn.simpletvapp.presentation.photo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.dazn.simpletvapp.data.model.MediaItem
import org.dazn.simpletvapp.data.repository.MediaRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeMediaRepository(
    private val items: List<MediaItem> = emptyList(),
) : MediaRepository() {
    override suspend fun listDirectory(path: String): List<MediaItem> = items
}

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeItem(name: String, type: String = "file", contentType: String? = null) =
        MediaItem(path = "photos/$name", type = type, contentType = contentType)

    @Test
    fun onlyPhotosAreSurfaced() = runTest {
        val items = listOf(
            makeItem("dir1", type = "directory"),
            makeItem("video.mp4", contentType = "video/mp4"),
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.png", contentType = "image/png"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/a.jpg")
        advanceUntilIdle()
        // hasPrev false at first (index 0), hasNext true (index 0 of 2)
        assertFalse(vm.hasPrev.value)
        assertTrue(vm.hasNext.value)
    }

    @Test
    fun currentIndexIsSetToPhotoPath() = runTest {
        val items = listOf(
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.jpg", contentType = "image/jpeg"),
            makeItem("c.jpg", contentType = "image/jpeg"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/b.jpg")
        advanceUntilIdle()
        assertTrue(vm.hasPrev.value)
        assertTrue(vm.hasNext.value)
    }

    @Test
    fun goNextAdvancesIndex() = runTest {
        val items = listOf(
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.jpg", contentType = "image/jpeg"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/a.jpg")
        advanceUntilIdle()
        assertFalse(vm.hasPrev.value)
        assertTrue(vm.hasNext.value)
        vm.goNext()
        advanceUntilIdle()
        assertTrue(vm.hasPrev.value)
        assertFalse(vm.hasNext.value)
    }

    @Test
    fun goPrevDecrementsIndex() = runTest {
        val items = listOf(
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.jpg", contentType = "image/jpeg"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/b.jpg")
        advanceUntilIdle()
        assertTrue(vm.hasPrev.value)
        vm.goPrev()
        advanceUntilIdle()
        assertFalse(vm.hasPrev.value)
        assertTrue(vm.hasNext.value)
    }

    @Test
    fun goNextIsNoopAtLastPhoto() = runTest {
        val items = listOf(
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.jpg", contentType = "image/jpeg"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/b.jpg")
        advanceUntilIdle()
        assertFalse(vm.hasNext.value)
        vm.goNext()
        advanceUntilIdle()
        assertFalse(vm.hasNext.value)
        assertTrue(vm.hasPrev.value)
    }

    @Test
    fun goPrevIsNoopAtFirstPhoto() = runTest {
        val items = listOf(
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.jpg", contentType = "image/jpeg"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/a.jpg")
        advanceUntilIdle()
        assertFalse(vm.hasPrev.value)
        vm.goPrev()
        advanceUntilIdle()
        assertFalse(vm.hasPrev.value)
        assertTrue(vm.hasNext.value)
    }

    @Test
    fun unknownPhotoPathDefaultsToFirstPhoto() = runTest {
        val items = listOf(
            makeItem("a.jpg", contentType = "image/jpeg"),
            makeItem("b.jpg", contentType = "image/jpeg"),
        )
        val vm = PhotoViewModel(FakeMediaRepository(items), "photos/unknown.jpg")
        advanceUntilIdle()
        assertFalse(vm.hasPrev.value)
        assertTrue(vm.hasNext.value)
    }

    @Test
    fun emptyDirectoryResultsInNoNavigation() = runTest {
        val vm = PhotoViewModel(FakeMediaRepository(emptyList()), "photos/a.jpg")
        advanceUntilIdle()
        assertFalse(vm.hasPrev.value)
        assertFalse(vm.hasNext.value)
        assertEquals("", vm.currentPhotoUrl.value)
    }
}
