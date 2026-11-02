package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechRecognizerState {
    object Idle : SpeechRecognizerState()
    object ReadyForSpeech : SpeechRecognizerState()
    data class Listening(val rmsDb: Float) : SpeechRecognizerState()
    data class PartialResult(val text: String) : SpeechRecognizerState()
    data class FinalResult(val text: String) : SpeechRecognizerState()
    data class Error(val errorMessage: String, val isOfflineNotice: Boolean = false) : SpeechRecognizerState()
}

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechRecognizerState>(SpeechRecognizerState.Idle)
    val state: StateFlow<SpeechRecognizerState> = _state.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(language: Language) {
        stopListening()

        if (!isAvailable()) {
            _state.value = SpeechRecognizerState.Error(
                "इस डिवाइस पर स्पीच रिकग्निशन सेवा उपलब्ध नहीं है। कृपया Google ऐप या वॉइस इनपुट सक्षम करें。"
            )
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.speechRecognizerTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.speechRecognizerTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language.speechRecognizerTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = SpeechRecognizerState.ReadyForSpeech
                }

                override fun onBeginningOfSpeech() {
                    _state.value = SpeechRecognizerState.Listening(0f)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (_state.value is SpeechRecognizerState.Listening || _state.value is SpeechRecognizerState.PartialResult) {
                        _state.value = SpeechRecognizerState.Listening(rmsdB)
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "ऑडियो रिकॉर्डिंग त्रुटि"
                        SpeechRecognizer.ERROR_CLIENT -> "क्लाइंट त्रुटि"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक्रोफोन अनुमति आवश्यक है"
                        SpeechRecognizer.ERROR_NETWORK -> "नेटवर्क त्रुटि - कृपया इंटरनेट कनेक्शन जांचें"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "नेटवर्क टाइमआउट"
                        SpeechRecognizer.ERROR_NO_MATCH -> "कोई आवाज़ नहीं पहचानी जा सकी, कृपया दोबारा बोलें"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "स्पीच रिकग्नाइज़र व्यस्त है"
                        SpeechRecognizer.ERROR_SERVER -> "सर्वर त्रुटि"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "आवाज़ सुनाई नहीं दी (टाइमआउट)"
                        else -> "अज्ञात त्रुटि कोड: $error"
                    }
                    _state.value = SpeechRecognizerState.Error(
                        errorMessage = message,
                        isOfflineNotice = (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_SERVER)
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestText = matches?.firstOrNull() ?: ""
                    if (bestText.isNotBlank()) {
                        _state.value = SpeechRecognizerState.FinalResult(bestText)
                    } else {
                        _state.value = SpeechRecognizerState.Idle
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _state.value = SpeechRecognizerState.PartialResult(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognizerHelper", "Failed to start speech recognition: ${e.message}", e)
            _state.value = SpeechRecognizerState.Error("स्पीच रिकग्निशन शुरू करने में विफल: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechRecognizerHelper", "Error stopping speech recognizer", e)
        }
        speechRecognizer = null
        if (_state.value is SpeechRecognizerState.Listening || _state.value is SpeechRecognizerState.ReadyForSpeech) {
            _state.value = SpeechRecognizerState.Idle
        }
    }

    fun reset() {
        stopListening()
        _state.value = SpeechRecognizerState.Idle
    }
}
