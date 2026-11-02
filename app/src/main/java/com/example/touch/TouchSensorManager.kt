package com.example.touch

import com.example.audio.TtsManager
import com.example.model.Language
import com.example.model.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TouchSensorMode {
    SINGLE_WORD,
    FULL_SENTENCE
}

enum class ReadingTarget {
    ORIGINAL,
    TRANSLATED
}

data class ParsedWord(
    val wordIndexInSentence: Int,
    val text: String,
    val cleanText: String
)

data class ParsedSentence(
    val sentenceIndex: Int,
    val fullText: String,
    val words: List<ParsedWord>
)

data class TouchSensorState(
    val isEnabled: Boolean = true,
    val mode: TouchSensorMode = TouchSensorMode.SINGLE_WORD,
    val readingTarget: ReadingTarget = ReadingTarget.ORIGINAL,
    val activeSentenceIndex: Int? = null,
    val activeWordIndex: Int? = null,
    val isContinuousReading: Boolean = false,
    val isPaused: Boolean = false,
    val statusMessage: String = "शब्द या वाक्य पर टच करें"
)

class TouchSensorManager(private val ttsManager: TtsManager) {

    private val _state = MutableStateFlow(TouchSensorState())
    val state: StateFlow<TouchSensorState> = _state.asStateFlow()

    private var continuousJob: Job? = null
    private var queuedSentences: List<ParsedSentence> = emptyList()
    private var currentSentenceQueueIndex = 0

    fun setMode(mode: TouchSensorMode) {
        _state.value = _state.value.copy(
            mode = mode,
            statusMessage = if (mode == TouchSensorMode.SINGLE_WORD)
                "सिंगल वर्ड मोड: किसी भी शब्द पर टच करके सुनें"
            else
                "फुल सेंटेंस मोड: वाक्य पर टच करके पूरा वाक्य सुनें"
        )
    }

    fun setReadingTarget(target: ReadingTarget) {
        stopReading()
        _state.value = _state.value.copy(readingTarget = target)
    }

    fun setEnabled(enabled: Boolean) {
        if (!enabled) stopReading()
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun handleWordTouch(
        sentenceIndex: Int,
        wordIndex: Int,
        parsedSentence: ParsedSentence,
        language: Language,
        voiceProfile: VoiceProfile,
        pitch: Float,
        speed: Float
    ) {
        if (!_state.value.isEnabled) return

        stopContinuousInternal()

        if (_state.value.mode == TouchSensorMode.SINGLE_WORD) {
            val word = parsedSentence.words.getOrNull(wordIndex) ?: return
            _state.value = _state.value.copy(
                activeSentenceIndex = sentenceIndex,
                activeWordIndex = wordIndex,
                statusMessage = "शब्द बोला जा रहा है: '${word.cleanText}'"
            )

            ttsManager.speak(
                text = word.cleanText,
                language = language,
                voiceProfile = voiceProfile,
                userPitchMultiplier = pitch,
                userSpeedMultiplier = speed,
                onDone = {
                    _state.value = _state.value.copy(activeWordIndex = null)
                }
            )
        } else {
            // FULL_SENTENCE mode
            _state.value = _state.value.copy(
                activeSentenceIndex = sentenceIndex,
                activeWordIndex = null,
                statusMessage = "पूरा वाक्य बोला जा रहा है (${sentenceIndex + 1}/${queuedSentences.size.coerceAtLeast(1)})"
            )

            ttsManager.speak(
                text = parsedSentence.fullText,
                language = language,
                voiceProfile = voiceProfile,
                userPitchMultiplier = pitch,
                userSpeedMultiplier = speed,
                onDone = {
                    _state.value = _state.value.copy(activeSentenceIndex = null)
                }
            )
        }
    }

    fun startFullPageReading(
        sentences: List<ParsedSentence>,
        language: Language,
        voiceProfile: VoiceProfile,
        pitch: Float,
        speed: Float
    ) {
        if (sentences.isEmpty() || !_state.value.isEnabled) return

        stopReading()
        queuedSentences = sentences
        currentSentenceQueueIndex = 0

        _state.value = _state.value.copy(
            isContinuousReading = true,
            isPaused = false,
            statusMessage = "पूर्ण पृष्ठ पठन प्रारंभ..."
        )

        readNextInQueue(language, voiceProfile, pitch, speed)
    }

    private fun readNextInQueue(language: Language, voiceProfile: VoiceProfile, pitch: Float, speed: Float) {
        if (!_state.value.isContinuousReading || _state.value.isPaused) return

        if (currentSentenceQueueIndex >= queuedSentences.size) {
            stopReading()
            _state.value = _state.value.copy(statusMessage = "पृष्ठ पठन पूर्ण हुआ")
            return
        }

        val sentence = queuedSentences[currentSentenceQueueIndex]
        _state.value = _state.value.copy(
            activeSentenceIndex = sentence.sentenceIndex,
            activeWordIndex = null,
            statusMessage = "पढ़ा जा रहा है: वाक्य ${currentSentenceQueueIndex + 1}/${queuedSentences.size}"
        )

        ttsManager.speak(
            text = sentence.fullText,
            language = language,
            voiceProfile = voiceProfile,
            userPitchMultiplier = pitch,
            userSpeedMultiplier = speed,
            onDone = {
                if (_state.value.isContinuousReading && !_state.value.isPaused) {
                    currentSentenceQueueIndex++
                    readNextInQueue(language, voiceProfile, pitch, speed)
                }
            }
        )
    }

    fun pauseReading() {
        if (_state.value.isContinuousReading) {
            ttsManager.stop()
            _state.value = _state.value.copy(isPaused = true, statusMessage = "पठन विरामित (Paused)")
        }
    }

    fun resumeReading(language: Language, voiceProfile: VoiceProfile, pitch: Float, speed: Float) {
        if (_state.value.isContinuousReading && _state.value.isPaused) {
            _state.value = _state.value.copy(isPaused = false, statusMessage = "पठन पुनः चालू")
            readNextInQueue(language, voiceProfile, pitch, speed)
        }
    }

    fun stopReading() {
        stopContinuousInternal()
        ttsManager.stop()
        _state.value = _state.value.copy(
            activeSentenceIndex = null,
            activeWordIndex = null,
            isContinuousReading = false,
            isPaused = false,
            statusMessage = "शब्द या वाक्य पर टच करें"
        )
    }

    private fun stopContinuousInternal() {
        continuousJob?.cancel()
        continuousJob = null
    }

    companion object {
        fun parseText(text: String): List<ParsedSentence> {
            if (text.isBlank()) return emptyList()

            // Split into sentences by punctuation across multiple scripts (?, !, ।, ।, ., \n)
            val rawSentences = text.split(Regex("(?<=[.?!।\n])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return rawSentences.mapIndexed { sIndex, sText ->
                val rawWords = sText.split(Regex("\\s+"))
                val words = rawWords.mapIndexed { wIndex, wText ->
                    val clean = wText.replace(Regex("[?.!,!।\"'()«»]"), "").trim()
                    ParsedWord(
                        wordIndexInSentence = wIndex,
                        text = wText,
                        cleanText = if (clean.isNotEmpty()) clean else wText
                    )
                }
                ParsedSentence(
                    sentenceIndex = sIndex,
                    fullText = sText,
                    words = words
                )
            }
        }
    }
}
