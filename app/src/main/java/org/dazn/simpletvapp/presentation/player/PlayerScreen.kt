package org.dazn.simpletvapp.presentation.player

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.dazn.simpletvapp.data.network.buildStreamUrl
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@UnstableApi
@Composable
fun PlayerScreen(
    path: String,
    displayAspectRatio: String? = null,
    videoCodec: String? = null,
    audioCodec: String? = null,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(
            application = LocalContext.current.applicationContext as Application,
            displayAspectRatio = displayAspectRatio,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
        )
    )
) {
    val showControls by viewModel.showControls.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val playbackEnded by viewModel.playbackEnded.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val goBackFocusRequester = remember { FocusRequester() }

    DisposableEffect(path) {
        val streamUrl = buildStreamUrl(path)
        val mediaItem = MediaItem.fromUri(streamUrl)
        viewModel.player.setMediaItem(mediaItem)
        viewModel.player.prepare()
        viewModel.player.play()
        onDispose {
            viewModel.player.stop()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(playbackError) {
        if (playbackError != null) goBackFocusRequester.requestFocus()
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(2_000)
            viewModel.setShowControls(false)
        }
    }

    LaunchedEffect(playbackEnded) {
        if (playbackEnded) {
            delay(1500)
            onBack()
        }
    }

    BackHandler {
        if (showControls) viewModel.setShowControls(false)
        else onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            if (playbackSpeed != 1f) {
                                viewModel.resetSpeed()
                            } else {
                                viewModel.togglePlayPause()
                            }
                            viewModel.setShowControls(true)
                            true
                        }
                        Key.DirectionLeft -> {
                            viewModel.player.seekTo(maxOf(0, viewModel.player.currentPosition - 10_000))
                            viewModel.setShowControls(true)
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.player.seekTo(viewModel.player.currentPosition + 10_000)
                            viewModel.setShowControls(true)
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.setShowControls(true)
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.setShowControls(false)
                            true
                        }
                        Key.MediaFastForward -> {
                            viewModel.cycleForwardSpeed()
                            viewModel.setShowControls(true)
                            true
                        }
                        Key.MediaRewind -> {
                            viewModel.cycleRewindSpeed()
                            viewModel.setShowControls(true)
                            true
                        }
                        Key.MediaPlayPause -> {
                            viewModel.togglePlayPause()
                            true
                        }
                        Key.MediaPlay -> {
                            viewModel.player.play()
                            true
                        }
                        Key.MediaPause -> {
                            viewModel.player.pause()
                            true
                        }
                        Key.MediaStop -> {
                            viewModel.player.stop()
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val aspectRatio = viewModel.aspectRatio
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PlayerSurface(
                player = viewModel.player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = if (aspectRatio != null)
                    Modifier.aspectRatio(aspectRatio)
                else
                    Modifier.fillMaxSize()
            )
        }

        if (showControls) {
            PlayerControls(
                filename = path.substringAfterLast("/"),
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = playbackError ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.dismissError()
                            onBack()
                        },
                        modifier = Modifier.focusRequester(goBackFocusRequester)
                    ) {
                        Text("Go back")
                    }
                }
            }
        }
    }
}
