package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val detectedLanguage: Language? = null,
    val isOffline: Boolean = false,
    val specialNotice: String? = null
)

class TranslationEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Automatic language detection based on Unicode script analysis and language-specific orthography.
     */
    fun detectLanguage(text: String): Language {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Language.SUPPORTED_LANGUAGES[0]

        // 1. Check for Bhojpuri dialect markers first if in Devanagari
        var devanagariCount = 0
        var chineseCount = 0
        var arabicScriptCount = 0
        var bengaliCount = 0
        var tamilCount = 0
        var cyrillicCount = 0
        var latinCount = 0

        for (ch in trimmed) {
            val codePoint = ch.code
            when (codePoint) {
                in 0x0900..0x097F -> devanagariCount++
                in 0x4E00..0x9FFF, in 0x3400..0x4DBF -> chineseCount++
                in 0x0600..0x06FF, in 0x0750..0x077F, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> arabicScriptCount++
                in 0x0980..0x09FF -> bengaliCount++
                in 0x0B80..0x0BFF -> tamilCount++
                in 0x0400..0x04FF -> cyrillicCount++
                in 0x0041..0x005A, in 0x0061..0x007A -> latinCount++
            }
        }

        val max = maxOf(devanagariCount, chineseCount, arabicScriptCount, bengaliCount, tamilCount, cyrillicCount, latinCount)

        return when {
            chineseCount == max && chineseCount > 0 -> Language.findByCode("zh")
            bengaliCount == max && bengaliCount > 0 -> Language.findByCode("bn")
            tamilCount == max && tamilCount > 0 -> Language.findByCode("ta")
            cyrillicCount == max && cyrillicCount > 0 -> Language.findByCode("ru")
            arabicScriptCount == max && arabicScriptCount > 0 -> {
                // Check Urdu specific letters: ٹ, ڈ, ڑ, ں, ے, ہ, پ, چ, ژ, گ
                val urduSpecific = Regex("[ٹڈڑںےہپچژگ]")
                if (urduSpecific.containsMatchIn(trimmed)) {
                    Language.findByCode("ur")
                } else {
                    Language.findByCode("ar")
                }
            }
            devanagariCount == max && devanagariCount > 0 -> {
                // Check Bhojpuri markers
                if (BhojpuriDialectEngine.isBhojpuriText(trimmed)) {
                    Language.findByCode("bho")
                } else {
                    // Check Nepali markers
                    val nepaliMarkers = listOf("छ", "छन्", "भयो", "गर्छ", "राम्रो", "तपाईं", "नेपाल")
                    if (nepaliMarkers.any { trimmed.contains(it) }) {
                        Language.findByCode("ne")
                    } else {
                        Language.findByCode("hi")
                    }
                }
            }
            else -> Language.findByCode("en")
        }
    }

    /**
     * Dual-engine translation: attempts online neural translation first; falls back cleanly to offline dictionary.
     */
    suspend fun translate(
        text: String,
        requestedSourceLang: Language,
        targetLang: Language
    ): TranslationResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLanguage = requestedSourceLang,
                targetLanguage = targetLang
            )
        }

        // Determine effective source language if set to Auto Detect
        val actualSourceLang = if (requestedSourceLang.code == "auto") {
            detectLanguage(trimmed)
        } else {
            requestedSourceLang
        }

        // If source and target are the same (and not Bhojpuri special)
        if (actualSourceLang.code == targetLang.code) {
            return@withContext TranslationResult(
                originalText = trimmed,
                translatedText = trimmed,
                sourceLanguage = actualSourceLang,
                targetLanguage = targetLang,
                detectedLanguage = if (requestedSourceLang.code == "auto") actualSourceLang else null
            )
        }

        // Handle Bhojpuri to/from Hindi directly
        if (actualSourceLang.code == "bho" && targetLang.code == "hi") {
            val converted = BhojpuriDialectEngine.convertBhojpuriToHindi(trimmed)
            return@withContext TranslationResult(
                originalText = trimmed,
                translatedText = converted,
                sourceLanguage = actualSourceLang,
                targetLanguage = targetLang,
                detectedLanguage = if (requestedSourceLang.code == "auto") actualSourceLang else null,
                specialNotice = "भोजपुरी से हिन्दी अनुवाद (आंचलिक व्याकरण सक्रिय)"
            )
        }

        if (actualSourceLang.code == "hi" && targetLang.code == "bho") {
            val converted = BhojpuriDialectEngine.convertHindiToBhojpuri(trimmed)
            return@withContext TranslationResult(
                originalText = trimmed,
                translatedText = converted,
                sourceLanguage = actualSourceLang,
                targetLanguage = targetLang,
                detectedLanguage = if (requestedSourceLang.code == "auto") actualSourceLang else null,
                specialNotice = "हिन्दी से भोजपुरी अनुवाद (ठेठ भोजपुरी शब्दावली)"
            )
        }

        // If source is Bhojpuri to third language (e.g. English, Chinese, etc.)
        val intermediateText = if (actualSourceLang.code == "bho") {
            BhojpuriDialectEngine.convertBhojpuriToHindi(trimmed)
        } else trimmed

        val effectiveSourceCode = if (actualSourceLang.code == "bho") "hi" else actualSourceLang.code
        val effectiveTargetCode = if (targetLang.code == "bho") "hi" else targetLang.code

        val isConnected = isNetworkAvailable()
        var translatedOutput: String? = null
        var isOfflineUsed = false

        if (isConnected) {
            try {
                translatedOutput = fetchOnlineTranslation(intermediateText, effectiveSourceCode, effectiveTargetCode)
            } catch (e: Exception) {
                Log.w("TranslationEngine", "Online translation error: ${e.message}, falling back to offline", e)
            }
        }

        if (translatedOutput.isNullOrBlank()) {
            // Offline fallback
            translatedOutput = translateOffline(intermediateText, effectiveSourceCode, effectiveTargetCode)
            isOfflineUsed = true
        }

        // If final target is Bhojpuri, convert the Hindi output to Bhojpuri
        val finalOutput = if (targetLang.code == "bho") {
            BhojpuriDialectEngine.convertHindiToBhojpuri(translatedOutput)
        } else {
            translatedOutput
        }

        val notice = when {
            targetLang.code == "bho" || actualSourceLang.code == "bho" ->
                "भोजपुरी विशेष अनुवाद: आंचलिक शब्दावली व व्याकरण प्रयुक्त"
            isOfflineUsed ->
                "इंटरनेट कनेक्शन उपलब्ध नहीं है (ऑफ़लाइन अनुवाद लागू किया गया)"
            else -> null
        }

        TranslationResult(
            originalText = trimmed,
            translatedText = finalOutput,
            sourceLanguage = actualSourceLang,
            targetLanguage = targetLang,
            detectedLanguage = if (requestedSourceLang.code == "auto") actualSourceLang else null,
            isOffline = isOfflineUsed,
            specialNotice = notice
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchOnlineTranslation(text: String, srcCode: String, tgtCode: String): String? {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$srcCode|$tgtCode"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SmartVoiceTranslator/1.0 (Android)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val responseData = json.optJSONObject("responseData") ?: return null
            val match = responseData.optString("translatedText")
            if (match.isNotBlank() && !match.equals("null", ignoreCase = true)) {
                return android.text.Html.fromHtml(match, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            }
        }
        return null
    }

    /**
     * Comprehensive multi-language offline phrasebook and dictionary for all 10 languages.
     */
    private fun translateOffline(text: String, srcCode: String, tgtCode: String): String {
        val clean = text.trim().lowercase()

        // Common conversational database across all 10 languages
        val offlineDictionary = mapOf(
            "hello" to mapOf(
                "hi" to "नमस्ते", "en" to "Hello", "zh" to "你好", "bho" to "प्रणाम",
                "ar" to "مرحباً", "ur" to "السلام علیکم", "bn" to "নমস্কার",
                "ne" to "नमस्ते", "ta" to "வணக்கம்", "ru" to "Привет"
            ),
            "how are you" to mapOf(
                "hi" to "आप कैसे हैं?", "en" to "How are you?", "zh" to "你好吗？", "bho" to "रउआ कइसन बानी?",
                "ar" to "كيف حالك؟", "ur" to "آپ کیسے ہیں؟", "bn" to "আপনি কেমন আছেন?",
                "ne" to "तपाईँलाई कस्तो छ?", "ta" to "நீங்கள் எப்படி இருக்கிறீர்கள்?", "ru" to "Как ваши дела?"
            ),
            "good morning" to mapOf(
                "hi" to "शुभ प्रभात", "en" to "Good morning", "zh" to "早上好", "bho" to "सुप्रभात / सबेरे के प्रणाम",
                "ar" to "صباح الخير", "ur" to "صبح بخیر", "bn" to "সুপ্রভাত",
                "ne" to "शुभ प्रभात", "ta" to "காலை வணக்கம்", "ru" to "Доброе утро"
            ),
            "good night" to mapOf(
                "hi" to "शुभ रात्रि", "en" to "Good night", "zh" to "晚安", "bho" to "शुभ रात्रि",
                "ar" to "تصبح على خير", "ur" to "شب بخیر", "bn" to "শুভ রাত্রি",
                "ne" to "शुभ रात्री", "ta" to "இனிய இரவு", "ru" to "Спокойной ночи"
            ),
            "thank you" to mapOf(
                "hi" to "धन्यवाद", "en" to "Thank you", "zh" to "谢谢", "bho" to "बहुते धन्यवाद",
                "ar" to "شكراً", "ur" to "شکریہ", "bn" to "ধন্যবাদ",
                "ne" to "धन्यवाद", "ta" to "நன்றி", "ru" to "Спасибо"
            ),
            "welcome" to mapOf(
                "hi" to "स्वागत है", "en" to "Welcome", "zh" to "欢迎", "bho" to "स्वागत बा",
                "ar" to "أهلاً وسهلاً", "ur" to "خوش آمدید", "bn" to "স্বাগতম",
                "ne" to "स्वागत छ", "ta" to "வரவேற்கிறோம்", "ru" to "Добро пожаловать"
            ),
            "yes" to mapOf(
                "hi" to "हाँ", "en" to "Yes", "zh" to "是的", "bho" to "हँ",
                "ar" to "نعم", "ur" to "جی ہاں", "bn" to "হ্যাঁ",
                "ne" to "हो", "ta" to "ஆம்", "ru" to "Да"
            ),
            "no" to mapOf(
                "hi" to "नहीं", "en" to "No", "zh" to "不", "bho" to "ना",
                "ar" to "لا", "ur" to "نہیں", "bn" to "না",
                "ne" to "होइन", "ta" to "இல்லை", "ru" to "Нет"
            ),
            "please" to mapOf(
                "hi" to "कृपया", "en" to "Please", "zh" to "请", "bho" to "कृपा कइके",
                "ar" to "من فضلك", "ur" to "براہ کرم", "bn" to "দয়া করে",
                "ne" to "कृपया", "ta" to "தயவுசெய்து", "ru" to "Пожалуйста"
            ),
            "sorry" to mapOf(
                "hi" to "माफ़ कीजिए", "en" to "Sorry", "zh" to "对不起", "bho" to "माफ करीं",
                "ar" to "آسف", "ur" to "معذرت", "bn" to "দুঃখিত",
                "ne" to "माफ गर्नुहोस्", "ta" to "மன்னிக்கவும்", "ru" to "Извините"
            ),
            "what is your name" to mapOf(
                "hi" to "आपका नाम क्या है?", "en" to "What is your name?", "zh" to "你叫什么名字？", "bho" to "राउर नाम का ह?",
                "ar" to "ما اسمك؟", "ur" to "آپ کا نام کیا ہے؟", "bn" to "আপনার নাম কি?",
                "ne" to "तपाईंको नाम के हो?", "ta" to "உங்கள் பெயர் என்ன?", "ru" to "Как вас зовут?"
            ),
            "where are you going" to mapOf(
                "hi" to "आप कहाँ जा रहे हैं?", "en" to "Where are you going?", "zh" to "你去哪里？", "bho" to "रउआ कहाँ जात बानी?",
                "ar" to "إلى أين أنت ذاهب؟", "ur" to "آپ کہاں جا رہے ہیں؟", "bn" to "আপনি কোথায় যাচ্ছেন?",
                "ne" to "तपाईं कहाँ जाँदै हुनुहुन्छ?", "ta" to "நீங்கள் எங்கே போகிறீர்கள்?", "ru" to "Куда вы идете?"
            ),
            "i am fine" to mapOf(
                "hi" to "मैं ठीक हूँ", "en" to "I am fine", "zh" to "我很好", "bho" to "हम ठीक बानी",
                "ar" to "أنا بخير", "ur" to "میں ٹھیک ہوں", "bn" to "আমি ভাল আছি",
                "ne" to "म सन्चै छु", "ta" to "நான் நலமாக இருக்கிறேன்", "ru" to "Я в порядке"
            ),
            "water" to mapOf(
                "hi" to "पानी", "en" to "Water", "zh" to "水", "bho" to "पानी",
                "ar" to "ماء", "ur" to "پانی", "bn" to "জল / পানি",
                "ne" to "पानी", "ta" to "தண்ணீர்", "ru" to "Вода"
            ),
            "food" to mapOf(
                "hi" to "खाना / भोजन", "en" to "Food", "zh" to "食物", "bho" to "खाना",
                "ar" to "طعام", "ur" to "کھانا", "bn" to "খাবার",
                "ne" to "खाना", "ta" to "உணவு", "ru" to "Еда"
            ),
            "friend" to mapOf(
                "hi" to "दोस्त / मित्र", "en" to "Friend", "zh" to "朋友", "bho" to "संगी / मितवा",
                "ar" to "صديق", "ur" to "دوست", "bn" to "বন্ধু",
                "ne" to "साथी", "ta" to "நண்பர்", "ru" to "Друг"
            )
        )

        // Find direct match
        for ((_, map) in offlineDictionary) {
            val srcVal = map[srcCode]?.lowercase()
            if (srcVal != null && (clean == srcVal || clean.replace(Regex("[?.!,]"), "") == srcVal.replace(Regex("[?.!,]"), ""))) {
                return map[tgtCode] ?: text
            }
        }

        // If English source
        val directKey = offlineDictionary[clean.replace(Regex("[?.!,]"), "")]
        if (directKey != null) {
            return directKey[tgtCode] ?: text
        }

        // Return original with offline notice indicator
        return text
    }
}
