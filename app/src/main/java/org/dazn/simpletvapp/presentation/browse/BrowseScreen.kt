package org.dazn.simpletvapp.presentation.browse

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import compose.icons.TablerIcons
import compose.icons.tablericons.File
import compose.icons.tablericons.FileMusic
import compose.icons.tablericons.Folder
import compose.icons.tablericons.Movie
import compose.icons.tablericons.Photo
import org.dazn.simpletvapp.data.model.MediaItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseScreen(
    onNavigateToPlayer: (path: String, format: String, displayAspectRatio: String?, videoCodec: String?, audioCodec: String?) -> Unit,
    onNavigateToPhoto: (String) -> Unit,
    onExitApp: () -> Unit,
    viewModel: BrowseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()

    BackHandler {
        if (currentPath.isEmpty()) onExitApp() else viewModel.navigateUp()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(32.dp)) {
        Text(
            text = if (currentPath.isEmpty()) "/" else "/ " + currentPath.replace("/", " / "),
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFCCCCCC),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val state = uiState) {
            is BrowseUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…", color = Color(0xFFCCCCCC))
                }
            }
            is BrowseUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = Color(0xFFCCCCCC))
                }
            }
            is BrowseUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No content found in this folder.", color = Color(0xFFCCCCCC))
                    }
                } else {
                    val listState = rememberLazyListState()
                    val lastFocused = viewModel.getLastFocusedItemPath(currentPath)
                    val targetItemPath = state.items.find { it.path == lastFocused }?.path
                        ?: state.items.firstOrNull()?.path

                    val targetIndex = state.items.indexOfFirst { it.path == targetItemPath }.coerceAtLeast(0)
                    var isScrollComplete by remember(targetItemPath) { mutableStateOf(targetIndex == 0) }

                    LaunchedEffect(targetItemPath, state.items) {
                        val idx = state.items.indexOfFirst { it.path == targetItemPath }
                        if (idx >= 0) listState.scrollToItem(idx)
                        isScrollComplete = true
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.alpha(if (isScrollComplete) 1f else 0f)
                        ) {
                            items(state.items) { item ->
                                MediaRow(
                                    item = item,
                                    shouldBeFocused = item.path == targetItemPath,
                                    onFocused = { viewModel.saveFocusedItem(currentPath, item.path) },
                                    onClick = {
                                        if (item.type == "directory") {
                                            viewModel.loadPath(item.path)
                                        } else if (item.isPhoto) {
                                            onNavigateToPhoto(item.path)
                                        } else {
                                            val videoStream = item.ffprobe_response?.streams?.firstOrNull { it.codec_type == "video" }
                                            val audioStream = item.ffprobe_response?.streams?.firstOrNull { it.codec_type == "audio" }
                                            val videoCodec = listOfNotNull(videoStream?.codec_name, videoStream?.profile)
                                                .joinToString("/").ifEmpty { null }
                                            onNavigateToPlayer(
                                                item.path,
                                                item.streamFormat ?: "",
                                                videoStream?.display_aspect_ratio,
                                                videoCodec,
                                                audioStream?.codec_name,
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        if (!isScrollComplete) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Loading…", color = Color(0xFFCCCCCC))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaRow(
    item: MediaItem,
    shouldBeFocused: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(shouldBeFocused) {
        if (shouldBeFocused) focusRequester.requestFocus()
    }

    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0xFF2A2A2A),
            pressedContainerColor = Color(0xFF3A3A3A)
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemIcon(item)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFCCCCCC))
        }
    }
}

@Composable
private fun ItemIcon(item: MediaItem) {
    val vector = when {
        item.type == "directory" -> TablerIcons.Folder
        item.isPhoto -> TablerIcons.Photo
        item.streamFormat in listOf("mp4", "mkv", "hls", "dash") -> TablerIcons.Movie
        item.streamFormat in listOf("mp3", "mka", "mks") -> TablerIcons.FileMusic
        else -> TablerIcons.File
    }
    Icon(
        imageVector = vector,
        contentDescription = null,
        tint = Color(0xFFCCCCCC),
        modifier = Modifier.size(24.dp)
    )
}
