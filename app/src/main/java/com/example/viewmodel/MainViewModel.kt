package com.example.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartTranslatorApplication
import com.example.audio.AudioPlayerManager
import com.example.audio.SpeechRecognizerHelper
import com.example.audio.TtsManager
import com.example.audio.VoiceRecorderManager
import com.example.engine.TranslationEngine
import com.example.engine.TranslationResult
import com.example.model.Language
import com.example.model.TranslationRecord
import com.example.model.VoiceProfile
import com.example.service.FloatingTranslatorService
import com.example.service.ScreenTranslatorAccessibilityService
import com.example.touch.ParsedSentence
import com.example.touch.ReadingTarget
import com.example.touch.TouchSensorManager
import com.example.touch.TouchSensorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    HOME,
    VOICE_RECORDING,
    CHAT_TRANSLATOR,
    FLOATING_TRANSLATOR,
    LANGUAGE_SELECTION,
    VOICE_SELECTION,
    TOUCH_SENSOR,
    TRANSLATION_HISTORY,
    SETTINGS
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val translatedText: String,
    val sourceLang: Language,
    val targetLang: Language,
    val isBhojpuriNotice: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SmartTranslatorApplication
    val settingsRepo = app.settingsRepository
    val dao = app.database.translationDao()

    val translationEngine = TranslationEngine(application)
    val ttsManager = TtsManager(application)
    val voiceRecorderManager = VoiceRecorderManager(application)
    val audioPlayerManager = AudioPlayerManager()
    val speechRecognizerHelper = SpeechRecognizerHelper(application)
    val touchSensorManager = TouchSensorManager(ttsManager)

    // Navigation State
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // All Records from Database
    val allHistoryRecords = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Chat Conversation
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Translation Loading State
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    // Active Result for Voice Recording or General
    private val _lastTranslationResult = MutableStateFlow<TranslationResult?>(null)
    val lastTranslationResult: StateFlow<TranslationResult?> = _lastTranslationResult.asStateFlow()

    // Floating overlay service state
    private val _isFloatingOverlayActive = MutableStateFlow(false)
    val isFloatingOverlayActive: StateFlow<Boolean> = _isFloatingOverlayActive.asStateFlow()

    init {
        // Populate initial welcome chat item
        _chatMessages.value = listOf(
            ChatMessage(
                text = "नमस्ते, स्मार्ट वॉयस ट्रांसलेटर में आपका स्वागत है!",
                translatedText = "Hello, welcome to Smart Voice Translator!",
                sourceLang = Language.findByCode("hi"),
                targetLang = Language.findByCode("en")
            )
        )
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        return if (_currentScreen.value != Screen.HOME) {
            _currentScreen.value = Screen.HOME
            true
        } else {
            false
        }
    }

    fun setSourceLanguage(code: String) {
        settingsRepo.setSourceLang(code)
    }

    fun setTargetLanguage(code: String) {
        settingsRepo.setTargetLang(code)
        ttsManager.updateTelemetryForLanguage(Language.findByCode(code))
    }

    fun swapLanguages() {
        val currentSrc = settingsRepo.sourceLang.value
        val currentTgt = settingsRepo.targetLang.value
        if (currentSrc != "auto") {
            settingsRepo.setSourceLang(currentTgt)
            settingsRepo.setTargetLang(currentSrc)
            ttsManager.updateTelemetryForLanguage(Language.findByCode(currentSrc))
        }
    }

    fun translateChatText(input: String, sourceLang: Language, targetLang: Language) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _isTranslating.value = true
            val result = translationEngine.translate(input, sourceLang, targetLang)
            val msg = ChatMessage(
                text = input,
                translatedText = result.translatedText,
                sourceLang = result.detectedLanguage ?: sourceLang,
                targetLang = targetLang,
                isBhojpuriNotice = result.specialNotice
            )
            _chatMessages.value = _chatMessages.value + msg

            // Save to database
            dao.insertRecord(
                TranslationRecord(
                    sourceLangCode = result.detectedLanguage?.code ?: sourceLang.code,
                    targetLangCode = targetLang.code,
                    originalText = input,
                    translatedText = result.translatedText,
                    category = "CHAT",
                    notes = result.specialNotice
                )
            )

            _isTranslating.value = false
        }
    }

    fun translateRecordedText(text: String, sourceLang: Language, targetLang: Language, audioFile: File?, durationMs: Long) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isTranslating.value = true
            val result = translationEngine.translate(text, sourceLang, targetLang)
            _lastTranslationResult.value = result

            dao.insertRecord(
                TranslationRecord(
                    sourceLangCode = result.detectedLanguage?.code ?: sourceLang.code,
                    targetLangCode = targetLang.code,
                    originalText = text,
                    translatedText = result.translatedText,
                    audioFilePath = audioFile?.absolutePath,
                    category = "RECORDING",
                    durationMs = durationMs,
                    notes = result.specialNotice
                )
            )

            _isTranslating.value = false
        }
    }

    fun speak(text: String, language: Language) {
        val voiceId = settingsRepo.voiceId.value
        val voice = VoiceProfile.findById(voiceId)
        val pitch = settingsRepo.voicePitch.value
        val speed = settingsRepo.voiceSpeed.value
        ttsManager.speak(text, language, voice, pitch, speed)
    }

    fun toggleFloatingService(): Boolean {
        val context = getApplication<Application>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return false
        }

        val active = !_isFloatingOverlayActive.value
        if (active) {
            FloatingTranslatorService.start(context)
        } else {
            FloatingTranslatorService.stop(context)
        }
        _isFloatingOverlayActive.value = active
        return true
    }

    fun deleteHistoryRecord(record: TranslationRecord) {
        viewModelScope.launch {
            dao.deleteRecord(record)
            record.audioFilePath?.let {
                val file = File(it)
                if (file.exists()) file.delete()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        audioPlayerManager.stop()
        voiceRecorderManager.cancelRecording()
        speechRecognizerHelper.stopListening()
    }
}
