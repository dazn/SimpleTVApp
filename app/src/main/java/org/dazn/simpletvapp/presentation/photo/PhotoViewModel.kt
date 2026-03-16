package org.dazn.simpletvapp.presentation.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.dazn.simpletvapp.data.network.buildStreamUrl
import org.dazn.simpletvapp.data.repository.MediaRepository

class PhotoViewModel(
    private val repository: MediaRepository,
    val photoPath: String,
) : ViewModel() {

    private val _photos = MutableStateFlow<List<String>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)

    val currentPhotoUrl: StateFlow<String> = combine(_photos, _currentIndex) { photos, idx ->
        if (photos.isEmpty()) "" else buildStreamUrl(photos[idx])
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val preloadUrls: StateFlow<List<String>> = combine(_photos, _currentIndex) { photos, idx ->
        buildList {
            if (idx > 0) add(buildStreamUrl(photos[idx - 1]))
            if (idx < photos.lastIndex) add(buildStreamUrl(photos[idx + 1]))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasPrev: StateFlow<Boolean> = _currentIndex
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hasNext: StateFlow<Boolean> = combine(_photos, _currentIndex) { photos, idx ->
        idx < photos.lastIndex
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            val dirPath = photoPath.substringBeforeLast("/")
            val items = try { repository.listDirectory(dirPath) } catch (_: Exception) { emptyList() }
            val sorted = items.filter { it.isPhoto }.sortedBy { it.name }
            _photos.value = sorted.map { it.path }
            _currentIndex.value = sorted.indexOfFirst { it.path == photoPath }.coerceAtLeast(0)
        }
    }

    fun goNext() {
        if (_currentIndex.value < _photos.value.lastIndex) _currentIndex.value++
    }

    fun goPrev() {
        if (_currentIndex.value > 0) _currentIndex.value--
    }
}

class PhotoViewModelFactory(
    private val repository: MediaRepository,
    private val photoPath: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PhotoViewModel(repository, photoPath) as T
    }
}
