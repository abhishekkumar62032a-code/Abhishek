package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.model.Language
import com.example.model.VoiceGender
import com.example.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class TtsTelemetry(
    val isInitialized: Boolean = false,
    val defaultEngine: String = "Android System TTS",
    val installedVoicesCount: Int = 0,
    val matchedLanguageVoicesCount: Int = 0,
    val currentVoiceName: String = "सिस्टम डिफ़ॉल्ट",
    val isOfflineCapable: Boolean = true,
    val statusMessage: String = "TTS प्रारंभ हो रहा है..."
)

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _telemetry = MutableStateFlow(TtsTelemetry())
    val telemetry: StateFlow<TtsTelemetry> = _telemetry.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onUtteranceDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts?.defaultEngine ?: "Google TTS Engine"
            val allVoices = tts?.voices ?: emptySet()
            val totalCount = allVoices.size

            _telemetry.value = TtsTelemetry(
                isInitialized = true,
                defaultEngine = engine,
                installedVoicesCount = totalCount,
                matchedLanguageVoicesCount = totalCount,
                statusMessage = "TTS सफलतापूर्वक सक्रिय है। कुल $totalCount सिस्टम वॉइस खोजी गईं।"
            )

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onUtteranceDoneCallback?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onUtteranceDoneCallback?.invoke()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onUtteranceDoneCallback?.invoke()
                }
            })
        } else {
            _telemetry.value = TtsTelemetry(
                isInitialized = false,
                statusMessage = "TTS प्रारंभ करने में विफल। कृपया सिस्टम सेटिंग्स में TTS जांचें।"
            )
        }
    }

    fun updateTelemetryForLanguage(language: Language) {
        val ttsInstance = tts ?: return
        try {
            val allVoices = ttsInstance.voices ?: emptySet()
            val matched = allVoices.filter { voice ->
                voice.locale.language.equals(language.locale.language, ignoreCase = true)
            }

            val statusMsg = if (matched.isNotEmpty()) {
                "${language.displayName} के लिए डिवाइस पर ${matched.size} वास्तविक सिस्टम वॉइस मौजूद हैं।"
            } else {
                "${language.displayName} के लिए मानक संश्लेषण सक्रिय है। पिच और टोन मॉड्यूलेशन द्वारा 10 विशिष्ट प्रोफाइल बनाई गई हैं।"
            }

            _telemetry.value = _telemetry.value.copy(
                matchedLanguageVoicesCount = matched.size,
                statusMessage = statusMsg
            )
        } catch (e: Exception) {
            Log.e("TtsManager", "Error querying voices: ${e.message}")
        }
    }

    fun speak(
        text: String,
        language: Language,
        voiceProfile: VoiceProfile,
        userPitchMultiplier: Float = 1.0f,
        userSpeedMultiplier: Float = 1.0f,
        onDone: (() -> Unit)? = null
    ) {
        val ttsInstance = tts ?: return
        if (text.isBlank()) return

        stop()
        this.onUtteranceDoneCallback = onDone

        try {
            // Apply language locale (fallback to Hindi for Bhojpuri if Bhojpuri TTS not present)
            val effectiveLocale = if (language.isBhojpuriSpecial) {
                Locale("hi", "IN")
            } else {
                language.locale
            }

            val result = ttsInstance.setLanguage(effectiveLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsInstance.language = Locale.ENGLISH
            }

            // Map installed system voice matching gender if possible
            val allVoices = ttsInstance.voices
            if (!allVoices.isNullOrEmpty()) {
                val matched = allVoices.filter { it.locale.language.equals(effectiveLocale.language, ignoreCase = true) }
                if (matched.isNotEmpty()) {
                    // Try to match gender by voice name attributes or index
                    val candidate = when (voiceProfile.gender) {
                        VoiceGender.FEMALE -> matched.find { it.name.contains("female", ignoreCase = true) || it.name.contains("fem", ignoreCase = true) } ?: matched.first()
                        VoiceGender.MALE -> matched.find { it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true) } ?: matched.last()
                    }
                    try {
                        ttsInstance.voice = candidate
                        _telemetry.value = _telemetry.value.copy(currentVoiceName = candidate.name)
                    } catch (e: Exception) {
                        // ignore voice switch exception
                    }
                }
            }

            // Calculate acoustic modulation
            val finalPitch = (voiceProfile.basePitch * userPitchMultiplier).coerceIn(0.5f, 2.0f)
            val finalSpeed = (voiceProfile.baseSpeed * userSpeedMultiplier).coerceIn(0.5f, 2.0f)

            ttsInstance.setPitch(finalPitch)
            ttsInstance.setSpeechRate(finalSpeed)

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString())
            ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
        } catch (e: Exception) {
            Log.e("TtsManager", "Error in speak: ${e.message}", e)
            _isSpeaking.value = false
            onDone?.invoke()
        }
    }

    fun playSample(
        voiceProfile: VoiceProfile,
        language: Language,
        userPitchMultiplier: Float = 1.0f,
        userSpeedMultiplier: Float = 1.0f
    ) {
        val sample = if (language.code == "en") {
            voiceProfile.sampleTextEnglish
        } else if (language.code == "bho") {
            "प्रणाम! हमार आवाज़ कइसन लागल? ई ${voiceProfile.hindiTitle} ह।"
        } else {
            "नमस्कार! यह मेरी आवाज़ है। मैं ${voiceProfile.hindiTitle} हूँ।"
        }
        speak(sample, language, voiceProfile, userPitchMultiplier, userSpeedMultiplier)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error stopping TTS", e)
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
