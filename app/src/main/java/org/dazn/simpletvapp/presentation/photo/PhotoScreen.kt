package org.dazn.simpletvapp.presentation.photo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Size

@Composable
fun PhotoScreen(
    viewModel: PhotoViewModel,
    imageLoader: ImageLoader,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val context      = LocalContext.current
    val targetUrl    by viewModel.currentPhotoUrl.collectAsStateWithLifecycle()
    val preloadUrls  by viewModel.preloadUrls.collectAsStateWithLifecycle()
    var displayedUrl by remember { mutableStateOf("") }
    val preloadDisposables = remember { mutableMapOf<String, Disposable>() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Swap displayedUrl only once the target is in cache
    LaunchedEffect(targetUrl) {
        if (targetUrl.isEmpty()) return@LaunchedEffect
        imageLoader.execute(
            ImageRequest.Builder(context)
                .data(targetUrl)
                .size(Size.ORIGINAL)
                .build()
        )
        displayedUrl = targetUrl
    }

    // Preload adjacent photos at low priority; only dispose URLs no longer needed
    LaunchedEffect(targetUrl, preloadUrls) {
        val urlsToKeep = preloadUrls.toSet() + targetUrl
        preloadDisposables.keys
            .filter { it !in urlsToKeep }
            .forEach { url -> preloadDisposables.remove(url)?.dispose() }
        preloadUrls.filter { it !in preloadDisposables }.forEach { url ->
            preloadDisposables[url] = imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .build()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft  -> { viewModel.goPrev(); true }
                        Key.DirectionRight -> { viewModel.goNext(); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        // Visible image (only swaps when new image is ready)
        if (displayedUrl.isEmpty()) {
            CircularProgressIndicator(color = Color.White)
        } else {
            AsyncImage(
                model = displayedUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
