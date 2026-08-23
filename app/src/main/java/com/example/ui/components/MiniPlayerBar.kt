package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.MediaPlayerEngine
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import kotlin.math.sin

@Composable
fun MiniPlayerBar(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by MediaPlayerEngine.playbackState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "MiniWave")
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveProgress"
    )

    AnimatedVisibility(
        visible = state.isMiniPlayerVisible && state.currentFile != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onExpand() },
            color = Color(0xEE161B22),
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MTCyan.copy(alpha = 0.4f))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Album disc / icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(MTCyan, ZAGold))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Playing",
                                tint = Color(0xFF0D1117),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${MediaPlayerEngine.formatDuration(state.currentPositionMs)} / ${MediaPlayerEngine.formatDuration(state.durationMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MTCyan
                                )
                            }
                        }
                    }

                    // Mini Waveform
                    Canvas(
                        modifier = Modifier
                            .width(50.dp)
                            .height(24.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        val bars = 6
                        val barW = 4.dp.toPx()
                        val sp = (size.width - (bars * barW)) / (bars - 1)
                        for (i in 0 until bars) {
                            val mult = if (state.isPlaying) {
                                (sin((i * 0.8f + waveAnim * 6.28f).toDouble()).toFloat() + 1f) / 2f
                            } else 0.2f
                            val h = (size.height * (0.2f + 0.8f * mult)).coerceIn(4f, size.height)
                            val x = i * (barW + sp)
                            val y = size.height - h
                            drawRoundRect(
                                color = MTCyan,
                                topLeft = Offset(x, y),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }

                    // Controls (Play/Pause, Close)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { MediaPlayerEngine.togglePlayPause() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { MediaPlayerEngine.closeMiniPlayer() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Player",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Bottom Progress Line
                if (state.durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (state.currentPositionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MTCyan,
                        trackColor = Color(0xFF21262D)
                    )
                }
            }
        }
    }
}
