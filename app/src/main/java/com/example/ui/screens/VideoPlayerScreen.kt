package com.example.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.FileEngine
import com.example.model.VideoPlayerState
import com.example.ui.theme.MTCyan
import com.example.ui.theme.ZAGold
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoPlayerScreen(
    state: VideoPlayerState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(state.isPlaying) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(state.isLooping) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Progress update loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPosition = vv.currentPosition
                    duration = vv.duration
                }
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Video View Component
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.fromFile(state.file))
                    setOnPreparedListener { mp ->
                        duration = mp.duration
                        mp.isLooping = isLooping
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        if (!isLooping) {
                            isPlaying = false
                        }
                    }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top App Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color(0xAA0D1117)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                videoViewRef?.stopPlayback()
                                onClose()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MTCyan
                                )
                            }
                            Column {
                                Text(
                                    text = state.file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${FileEngine.formatFileSize(state.file.length())} • Built-in Video Player",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MTCyan
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(onClick = { showSpeedMenu = true }) {
                                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = ZAGold)
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x") },
                                            onClick = {
                                                playbackSpeed = speed
                                                showSpeedMenu = false
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    try {
                                                        // Apply speed if possible
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Center Play/Pause & Quick Skip
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(target)
                                currentPosition = target
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(MTCyan, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF0D1117),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                val target = (vv.currentPosition + 10000).coerceAtMost(duration)
                                vv.seekTo(target)
                                currentPosition = target
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                    }
                }

                // Bottom Scrubber Bar & Timers
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xDD161B22)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZAGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8B949E)
                            )
                        }

                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = { frac ->
                                val target = (frac * duration).toInt()
                                currentPosition = target
                                videoViewRef?.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MTCyan,
                                activeTrackColor = MTCyan,
                                inactiveTrackColor = Color(0xFF30363D)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    val hours = min / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, min % 60, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
