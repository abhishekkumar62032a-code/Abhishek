package com.example.audio

import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentFilePath: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun play(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerManager", "File not found: $filePath")
            return
        }

        // If same file is paused, just resume
        if (_playbackState.value.currentFilePath == filePath && mediaPlayer != null) {
            if (!_playbackState.value.isPlaying) {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressPolling()
                return
            }
        }

        stop()

        try {
            val player = MediaPlayer()
            player.setDataSource(filePath)
            player.prepare()
            val totalDuration = player.duration

            player.setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = 0
                )
                stopProgressPolling()
            }

            player.start()
            mediaPlayer = player
            _playbackState.value = PlaybackState(
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = totalDuration,
                currentFilePath = filePath
            )
            startProgressPolling()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio: ${e.message}", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    stopProgressPolling()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to pause audio", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to seek audio", e)
        }
    }

    fun stop() {
        stopProgressPolling()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = player.currentPosition,
                                durationMs = player.duration
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient errors
                }
                delay(100)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }
}
