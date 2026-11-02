package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
import java.io.IOException

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val durationMs: Long, val amplitude: Float) : RecordingState()
    data class Paused(val durationMs: Long) : RecordingState()
    data class Completed(val audioFile: File, val durationMs: Long) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var recordingJob: Job? = null
    private var recordedDurationMs: Long = 0L
    private var isPaused: Boolean = false

    @Suppress("DEPRECATION")
    fun startRecording(): Boolean {
        try {
            stopRecordingInternal()

            val dir = File(context.filesDir, "recordings")
            if (!dir.exists()) dir.mkdirs()

            val outputFile = File(dir, "rec_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            recordedDurationMs = 0L
            isPaused = false

            startTicker()
            return true
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Failed to start recording: ${e.message}", e)
            _recordingState.value = RecordingState.Error("रिकॉर्डिंग शुरू करने में त्रुटि: ${e.localizedMessage}")
            return false
        }
    }

    fun pauseRecording(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                isPaused = true
                _recordingState.value = RecordingState.Paused(recordedDurationMs)
                return true
            } catch (e: Exception) {
                Log.e("VoiceRecorderManager", "Error pausing recorder", e)
            }
        }
        return false
    }

    fun resumeRecording(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                isPaused = false
                return true
            } catch (e: Exception) {
                Log.e("VoiceRecorderManager", "Error resuming recorder", e)
            }
        }
        return false
    }

    fun stopRecording(): File? {
        val file = currentOutputFile
        val duration = recordedDurationMs
        stopRecordingInternal()
        if (file != null && file.exists() && file.length() > 0) {
            _recordingState.value = RecordingState.Completed(file, duration)
            return file
        } else {
            _recordingState.value = RecordingState.Idle
            return null
        }
    }

    fun cancelRecording() {
        stopRecordingInternal()
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
        _recordingState.value = RecordingState.Idle
    }

    private fun stopRecordingInternal() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    // Ignore stop fail if immediately stopped
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Error stopping recorder", e)
        }
        recorder = null
    }

    private fun startTicker() {
        recordingJob?.cancel()
        recordingJob = CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                if (!isPaused) {
                    recordedDurationMs += 100L
                    val rawAmp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalizedAmp = (rawAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                    _recordingState.value = RecordingState.Recording(recordedDurationMs, normalizedAmp)
                }
                delay(100)
            }
        }
    }
}
