package org.dazn.simpletvapp.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlin.math.abs

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerControls(
    filename: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPlaying) "⏸" else "▶",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Text(
                text = " $filename",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (playbackSpeed != 1f) {
                val speedLabel = if (playbackSpeed < 0) {
                    "-${abs(playbackSpeed).toInt()}×"
                } else {
                    "${playbackSpeed.toInt()}×"
                }
                Text(
                    text = "  $speedLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFAA00),
                )
            }
            Text(
                text = "  ${formatTime(currentPosition)} / ${formatTime(duration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
        val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Color.White)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
