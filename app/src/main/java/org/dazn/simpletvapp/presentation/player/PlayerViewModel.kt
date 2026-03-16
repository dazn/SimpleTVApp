package org.dazn.simpletvapp.presentation.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.dazn.simpletvapp.BuildConfig
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Controls are visible when explicitly requested OR when in trick-play (speed != 1f).
 * Extracted as a top-level internal function for unit testability.
 */
internal fun controlsVisible(requested: Boolean, playbackSpeed: Float): Boolean =
    requested || playbackSpeed != 1f

/**
 * Parses a display aspect ratio string like "16:9" into a Float.
 * Returns null if the input is null, malformed, or has a zero/negative height.
 * Extracted as a top-level internal function for unit testability.
 */
internal fun parseAspectRatio(dar: String?): Float? {
    val parts = dar?.split(":") ?: return null
    val w = parts.getOrNull(0)?.toFloatOrNull() ?: return null
    val h = parts.getOrNull(1)?.toFloatOrNull()?.takeIf { it > 0 } ?: return null
    return w / h
}

@UnstableApi
class PlayerViewModel(
    application: Application,
    displayAspectRatio: String? = null,
    private val videoCodec: String? = null,
    private val audioCodec: String? = null,
) : AndroidViewModel(application) {

    val aspectRatio: Float? = parseAspectRatio(displayAspectRatio)

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _playbackEnded = MutableStateFlow(false)
    val playbackEnded: StateFlow<Boolean> = _playbackEnded.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    val showControls: StateFlow<Boolean> =
        combine(_showControls, _playbackSpeed) { show, speed ->
            controlsVisible(requested = show, playbackSpeed = speed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val trickPlaySteps = listOf(15, 45, 130)
    private val trickPlayIntervalMs = 1000L
    private var trickPlayJob: Job? = null

    val player: ExoPlayer = run {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                mapOf("Authorization" to "Bearer ${BuildConfig.API_KEY}")
            )
        ExoPlayer.Builder(application)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) _duration.value = player.duration.coerceAtLeast(0L)
            if (state == Player.STATE_ENDED) _playbackEnded.value = true
        }
        override fun onPlayerError(error: PlaybackException) {
            _playbackError.value = when {
                error.errorCode in 3003..3004 || error.errorCode in 4001..4003 -> {
                    val codecInfo = listOfNotNull(videoCodec, audioCodec).joinToString(", ")
                    val detail = if (codecInfo.isNotBlank()) "Codec: $codecInfo" else "Unknown format"
                    "This file could not be played. $detail"
                }
                error.errorCode in 2000..2999 -> {
                    "Could not load file (network error ${error.errorCode})."
                }
                else -> {
                    "Playback failed (error ${error.errorCode})."
                }
            }
        }
    }

    init {
        player.addListener(playerListener)

        viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                delay(500)
            }
        }
    }

    fun setShowControls(show: Boolean) {
        _showControls.value = show
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun startTrickPlay(deltaSeconds: Int) {
        trickPlayJob?.cancel()
        player.pause()
        player.setPlaybackSpeed(1f)
        _playbackSpeed.value = deltaSeconds.toFloat()
        trickPlayJob = viewModelScope.launch {
            while (true) {
                val duration = _duration.value
                val newPos = (player.currentPosition + deltaSeconds * trickPlayIntervalMs)
                    .coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
                player.seekTo(newPos)
                if (newPos == 0L || (duration > 0 && newPos >= duration)) {
                    resetSpeed()
                    break
                }
                delay(trickPlayIntervalMs)
            }
        }
    }

    fun cycleForwardSpeed() {
        val current = _playbackSpeed.value.toInt()
        val idx = trickPlaySteps.indexOf(current)
        if (idx == trickPlaySteps.size - 1) {
            resetSpeed()
        } else {
            startTrickPlay(trickPlaySteps[(idx + 1).coerceAtLeast(0)])
        }
    }

    fun cycleRewindSpeed() {
        val current = _playbackSpeed.value.toInt()
        val idx = trickPlaySteps.indexOf(-current)
        if (idx == trickPlaySteps.size - 1) {
            startTrickPlay(-trickPlaySteps[0])
        } else if (current < 0) {
            startTrickPlay(-trickPlaySteps[idx + 1])
        } else {
            startTrickPlay(-trickPlaySteps[0])
        }
    }

    fun resetSpeed() {
        trickPlayJob?.cancel()
        trickPlayJob = null
        player.setPlaybackSpeed(1f)
        _playbackSpeed.value = 1f
        player.play()
    }

    fun dismissError() {
        _playbackError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        trickPlayJob?.cancel()
        player.removeListener(playerListener)
        player.release()
    }

}

@UnstableApi
class PlayerViewModelFactory(
    private val application: Application,
    private val displayAspectRatio: String?,
    private val videoCodec: String?,
    private val audioCodec: String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerViewModel(application, displayAspectRatio, videoCodec, audioCodec) as T
    }
}
