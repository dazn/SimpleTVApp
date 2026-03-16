package org.dazn.simpletvapp.presentation.browse

import org.dazn.simpletvapp.data.model.MediaItem
import org.dazn.simpletvapp.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMediaRepository(
    private val items: List<MediaItem> = emptyList(),
    private val shouldThrow: Boolean = false
) : MediaRepository() {
    var lastLoadedPath: String? = null

    override suspend fun listDirectory(path: String): List<MediaItem> {
        lastLoadedPath = path
        if (shouldThrow) throw RuntimeException("network error")
        return items
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() {
        val fakeRepo = FakeMediaRepository()
        val vm = BrowseViewModel(fakeRepo)
        assertTrue(vm.uiState.value is BrowseUiState.Loading)
    }

    @Test
    fun loadsRootOnInit() = runTest {
        val fakeRepo = FakeMediaRepository()
        val vm = BrowseViewModel(fakeRepo)
        advanceUntilIdle()
        assertEquals("", fakeRepo.lastLoadedPath)
    }

    @Test
    fun stateBecomesSuccessOnLoad() = runTest {
        val items = listOf(MediaItem(path = "Movies", type = "directory"))
        val fakeRepo = FakeMediaRepository(items)
        val vm = BrowseViewModel(fakeRepo)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is BrowseUiState.Success)
        assertEquals(items, (state as BrowseUiState.Success).items)
    }

    @Test
    fun stateBecomesErrorOnFailure() = runTest {
        val fakeRepo = FakeMediaRepository(shouldThrow = true)
        val vm = BrowseViewModel(fakeRepo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is BrowseUiState.Error)
    }

    @Test
    fun navigateUpStripsLastSegment() = runTest {
        val fakeRepo = FakeMediaRepository()
        val vm = BrowseViewModel(fakeRepo)
        advanceUntilIdle()
        vm.loadPath("foo/bar")
        advanceUntilIdle()
        vm.navigateUp()
        advanceUntilIdle()
        assertEquals("foo", fakeRepo.lastLoadedPath)
    }

    @Test
    fun navigateUpAtRootIsNoop() = runTest {
        val fakeRepo = FakeMediaRepository()
        val vm = BrowseViewModel(fakeRepo)
        advanceUntilIdle()
        fakeRepo.lastLoadedPath = null
        vm.navigateUp()
        advanceUntilIdle()
        assertEquals(null, fakeRepo.lastLoadedPath)
    }

    @Test
    fun getLastFocusedItemPathReturnsNullForUnvisitedPath() {
        val vm = BrowseViewModel(FakeMediaRepository())
        assertEquals(null, vm.getLastFocusedItemPath("Movies"))
    }

    @Test
    fun saveFocusedItemIsReturnedByGet() {
        val vm = BrowseViewModel(FakeMediaRepository())
        vm.saveFocusedItem("Movies", "Movies/Inception.mp4")
        assertEquals("Movies/Inception.mp4", vm.getLastFocusedItemPath("Movies"))
    }

    @Test
    fun focusMemoryIsIndependentPerPath() {
        val vm = BrowseViewModel(FakeMediaRepository())
        vm.saveFocusedItem("Movies", "Movies/Inception.mp4")
        vm.saveFocusedItem("Shows", "Shows/Breaking Bad")
        assertEquals("Movies/Inception.mp4", vm.getLastFocusedItemPath("Movies"))
        assertEquals("Shows/Breaking Bad", vm.getLastFocusedItemPath("Shows"))
    }

    @Test
    fun saveFocusedItemOverwritesPreviousValue() {
        val vm = BrowseViewModel(FakeMediaRepository())
        vm.saveFocusedItem("Movies", "Movies/Inception.mp4")
        vm.saveFocusedItem("Movies", "Movies/Interstellar.mp4")
        assertEquals("Movies/Interstellar.mp4", vm.getLastFocusedItemPath("Movies"))
    }
}
