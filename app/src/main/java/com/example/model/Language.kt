package com.example.model

import java.util.Locale

data class Language(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val locale: Locale,
    val ttsLanguageTag: String,
    val speechRecognizerTag: String,
    val flagEmoji: String,
    val sampleGreeting: String,
    val isBhojpuriSpecial: Boolean = false
) {
    val displayName: String get() = "$nativeName ($englishName)"

    companion object {
        val AUTO_DETECT = Language(
            code = "auto",
            englishName = "Auto Detect",
            nativeName = "स्वतः पहचान",
            locale = Locale.getDefault(),
            ttsLanguageTag = "en-US",
            speechRecognizerTag = "en-US",
            flagEmoji = "✨",
            sampleGreeting = "ऑटोमैटिक भाषा पहचान"
        )

        val SUPPORTED_LANGUAGES: List<Language> = listOf(
            Language(
                code = "hi",
                englishName = "Hindi",
                nativeName = "हिन्दी",
                locale = Locale("hi", "IN"),
                ttsLanguageTag = "hi-IN",
                speechRecognizerTag = "hi-IN",
                flagEmoji = "🇮🇳",
                sampleGreeting = "नमस्ते, आप कैसे हैं?"
            ),
            Language(
                code = "zh",
                englishName = "Chinese Mandarin",
                nativeName = "中文 (普通话)",
                locale = Locale.SIMPLIFIED_CHINESE,
                ttsLanguageTag = "zh-CN",
                speechRecognizerTag = "zh-CN",
                flagEmoji = "🇨🇳",
                sampleGreeting = "你好，很高兴见到你！"
            ),
            Language(
                code = "bho",
                englishName = "Bhojpuri",
                nativeName = "भोजपुरी",
                locale = Locale("hi", "IN"), // Fallback TTS/STT locale with specialized Bhojpuri engine
                ttsLanguageTag = "hi-IN",
                speechRecognizerTag = "hi-IN",
                flagEmoji = "🌾",
                sampleGreeting = "प्रणाम! रउआ कइसन बानी? सब ठीक बा?",
                isBhojpuriSpecial = true
            ),
            Language(
                code = "en",
                englishName = "English",
                nativeName = "English",
                locale = Locale.US,
                ttsLanguageTag = "en-US",
                speechRecognizerTag = "en-US",
                flagEmoji = "🇺🇸",
                sampleGreeting = "Hello! How are you doing today?"
            ),
            Language(
                code = "ar",
                englishName = "Arabic",
                nativeName = "العربية",
                locale = Locale("ar", "SA"),
                ttsLanguageTag = "ar-SA",
                speechRecognizerTag = "ar-SA",
                flagEmoji = "🇸🇦",
                sampleGreeting = "مرحباً! كيف حالك اليوم؟"
            ),
            Language(
                code = "ur",
                englishName = "Urdu",
                nativeName = "اردو",
                locale = Locale("ur", "PK"),
                ttsLanguageTag = "ur-PK",
                speechRecognizerTag = "ur-PK",
                flagEmoji = "🇵🇰",
                sampleGreeting = "السلام علیکم! آپ کیسے ہیں؟"
            ),
            Language(
                code = "bn",
                englishName = "Bengali",
                nativeName = "বাংলা",
                locale = Locale("bn", "BD"),
                ttsLanguageTag = "bn-BD",
                speechRecognizerTag = "bn-BD",
                flagEmoji = "🇧🇩",
                sampleGreeting = "নমস্কার! আপনি কেমন আছেন?"
            ),
            Language(
                code = "ne",
                englishName = "Nepali",
                nativeName = "नेपाली",
                locale = Locale("ne", "NP"),
                ttsLanguageTag = "ne-NP",
                speechRecognizerTag = "ne-NP",
                flagEmoji = "🇳🇵",
                sampleGreeting = "नमस्ते! तपाईँलाई कस्तो छ?"
            ),
            Language(
                code = "ta",
                englishName = "Tamil",
                nativeName = "தமிழ்",
                locale = Locale("ta", "IN"),
                ttsLanguageTag = "ta-IN",
                speechRecognizerTag = "ta-IN",
                flagEmoji = "🇮🇳",
                sampleGreeting = "வணக்கம்! நீங்கள் எப்படி இருக்கிறீர்கள்?"
            ),
            Language(
                code = "ru",
                englishName = "Russian",
                nativeName = "Русский",
                locale = Locale("ru", "RU"),
                ttsLanguageTag = "ru-RU",
                speechRecognizerTag = "ru-RU",
                flagEmoji = "🇷🇺",
                sampleGreeting = "Привет! Как ваши дела?"
            )
        )

        fun findByCode(code: String): Language {
            if (code.equals("auto", ignoreCase = true)) return AUTO_DETECT
            return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
                ?: SUPPORTED_LANGUAGES[0]
        }
    }
}
