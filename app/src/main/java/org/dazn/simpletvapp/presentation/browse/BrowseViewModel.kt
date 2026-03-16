package org.dazn.simpletvapp.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.dazn.simpletvapp.data.model.MediaItem
import org.dazn.simpletvapp.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface BrowseUiState {
    data object Loading : BrowseUiState
    data class Success(val items: List<MediaItem>) : BrowseUiState
    data class Error(val message: String) : BrowseUiState
}

class BrowseViewModel(
    private val repository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    init {
        loadPath("")
    }

    fun loadPath(path: String) {
        _currentPath.value = path
        _uiState.value = BrowseUiState.Loading
        viewModelScope.launch {
            try {
                val items = repository.listDirectory(path)
                _uiState.value = BrowseUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val lastFocusedByPath = mutableMapOf<String, String>()

    fun saveFocusedItem(directoryPath: String, itemPath: String) {
        lastFocusedByPath[directoryPath] = itemPath
    }

    fun getLastFocusedItemPath(directoryPath: String): String? = lastFocusedByPath[directoryPath]

    fun navigateUp() {
        val path = _currentPath.value
        if (path.isEmpty()) return
        val parent = path.substringBeforeLast("/", "").let {
            if (it == path) "" else it
        }
        loadPath(parent)
    }
}
