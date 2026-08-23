package com.example.engine

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentFile: File? = null,
    val title: String = "",
    val artist: String = "MTZ Built-in Player",
    val durationMs: Int = 0,
    val currentPositionMs: Int = 0,
    val isLooping: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isMiniPlayerVisible: Boolean = false
)

object MediaPlayerEngine {
    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    fun playAudio(context: Context, file: File) {
        stopAudio()
        try {
            val player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()
                setOnCompletionListener {
                    if (!_playbackState.value.isLooping) {
                        _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0) }
                    }
                }
            }
            mediaPlayer = player

            _playbackState.update {
                it.copy(
                    isPlaying = true,
                    currentFile = file,
                    title = file.nameWithoutExtension,
                    durationMs = player.duration,
                    currentPositionMs = 0,
                    isMiniPlayerVisible = true
                )
            }

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playbackState.update { it.copy(isPlaying = false) }
        } else {
            player.start()
            _playbackState.update { it.copy(isPlaying = true) }
            startProgressTracker()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun toggleLoop() {
        val newLoop = !_playbackState.value.isLooping
        mediaPlayer?.isLooping = newLoop
        _playbackState.update { it.copy(isLooping = newLoop) }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                }
            }
            _playbackState.update { it.copy(playbackSpeed = speed) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeMiniPlayer() {
        stopAudio()
        _playbackState.update { it.copy(isMiniPlayerVisible = false, currentFile = null) }
    }

    fun stopAudio() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0) }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                _playbackState.update { it.copy(currentPositionMs = current) }
                delay(250)
            }
        }
    }

    fun formatDuration(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
