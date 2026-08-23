package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileEngine
import com.example.engine.MediaPlayerEngine
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import java.io.File
import kotlin.math.sin

@Composable
fun AudioPlayerScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by MediaPlayerEngine.playbackState.collectAsState()
    var showSpeedMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylSpin"
    )

    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveVisualizer"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF07090E),
                        Color(0xFF020408)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MTCyan
                    )
                }

                Text(
                    text = "MTZ AUDIO ENGINE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MTCyan,
                    letterSpacing = 2.sp
                )

                Box {
                    IconButton(onClick = { showSpeedMenu = true }) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = ZAGold)
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { sp ->
                            DropdownMenuItem(
                                text = { Text("${sp}x Speed") },
                                onClick = {
                                    MediaPlayerEngine.setPlaybackSpeed(sp)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vinyl / Audio Art Disc
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161B22))
                    .rotate(if (playbackState.isPlaying) rotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = Color(0xFF21262D),
                        radius = size.minDimension / 2 - 8,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF30363D),
                        radius = size.minDimension / 2 - 28,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF21262D),
                        radius = size.minDimension / 2 - 50,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }

                // Center Label
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MTCyan, ZAGold))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = Color(0xFF0D1117),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Real-Time Equalizer Waveform
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 24.dp)
            ) {
                val barCount = 28
                val barWidth = (size.width / barCount) * 0.6f
                val spacing = size.width / barCount

                for (i in 0 until barCount) {
                    val multiplier = if (playbackState.isPlaying) {
                        (sin((i * 0.45f + waveAnim * 6.28f).toDouble()).toFloat() + 1f) / 2f
                    } else 0.1f

                    val barHeight = (size.height * (0.15f + 0.85f * multiplier)).coerceIn(4f, size.height)
                    val x = i * spacing + (spacing - barWidth) / 2
                    val y = size.height - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                MTCyan,
                                ZAGold
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }

            // Track Title & Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = playbackState.title.ifEmpty { "No Audio Selected" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playbackState.currentFile?.let { "${it.extension.uppercase()} • ${FileEngine.formatFileSize(it.length())}" } ?: "MTZ Hi-Res Player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MTCyan
                )
            }

            // Progress Slider & Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (playbackState.durationMs > 0) {
                        playbackState.currentPositionMs.toFloat() / playbackState.durationMs
                    } else 0f,
                    onValueChange = { frac ->
                        val targetMs = (frac * playbackState.durationMs).toInt()
                        MediaPlayerEngine.seekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MTCyan,
                        activeTrackColor = MTCyan,
                        inactiveTrackColor = Color(0xFF30363D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = MediaPlayerEngine.formatDuration(playbackState.currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E)
                    )
                    Text(
                        text = "${playbackState.playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZAGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = MediaPlayerEngine.formatDuration(playbackState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E)
                    )
                }
            }

            // Player Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { MediaPlayerEngine.toggleLoop() }) {
                    Icon(
                        imageVector = if (playbackState.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Loop",
                        tint = if (playbackState.isLooping) ZAGold else Color(0xFF8B949E)
                    )
                }

                // Large Central Play/Pause Button
                IconButton(
                    onClick = { MediaPlayerEngine.togglePlayPause() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(MTCyan, ZAGold)),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF07090E),
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = {
                    playbackState.currentFile?.let { f ->
                        MediaPlayerEngine.seekTo(0)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Reset Track",
                        tint = MTCyan
                    )
                }
            }
        }
    }
}
