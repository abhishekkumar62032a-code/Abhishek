package com.example.engine

object BhojpuriDialectEngine {

    val sampleBhojpuriPhrases = listOf(
        "प्रणाम! रउआ कइसन बानी?" to "नमस्ते! आप कैसे हैं? (Hello! How are you?)",
        "सब ठीक-ठाक बा, रउआ बताईं।" to "सब ठीक है, आप बताइए। (Everything is fine, you tell.)",
        "राउर नाम का ह?" to "आपका नाम क्या है? (What is your name?)",
        "हमार नाम आनन्द बा।" to "मेरा नाम आनंद है। (My name is Anand.)",
        "ईहाँ आवऽ आ बइठऽ।" to "यहाँ आओ और बैठो। (Come here and sit down.)",
        "का हालचाल बा भाई?" to "क्या हालचाल है भाई? (How are things, brother?)",
        "रउआ कहाँ जात बानी?" to "आप कहाँ जा रहे हैं? (Where are you going?)",
        "हम खाना खा लेहलीं।" to "मैंने खाना खा लिया है। (I have eaten food.)"
    )

    private val hindiToBhojpuriWords = mapOf(
        "आप" to "रउआ",
        "आपका" to "राउर",
        "आपके" to "राउर",
        "आपकी" to "राउर",
        "तुम" to "तूँ",
        "तुम्हारा" to "तोहार",
        "तुम्हारी" to "तोहार",
        "तुम्हारे" to "तोहार",
        "मैं" to "हम",
        "मेरा" to "हमार",
        "मेरी" to "हमार",
        "मेरे" to "हमार",
        "हम" to "हमनी",
        "हमारा" to "हमनी के",
        "क्या" to "का",
        "क्यों" to "काहे",
        "कैसे" to "कइसन",
        "कैसा" to "कइसन",
        "कैसी" to "कइसन",
        "कहाँ" to "कहाँ",
        "यहाँ" to "ईहाँ",
        "वहाँ" to "ऊहाँ",
        "कब" to "कहिले",
        "है" to "बा",
        "हैं" to "बानी",
        "हूँ" to "बानी",
        "था" to "रहल",
        "थी" to "रहलीं",
        "थे" to "रहलन",
        "नहीं" to "ना",
        "अच्छा" to "नीक",
        "अच्छी" to "नीक",
        "बहुत" to "बहुते",
        "थोड़ा" to "तनिका",
        "पानी" to "पानी",
        "खाना" to "भोजन / खाना",
        "घर" to "घरे",
        "जाना" to "जाइल",
        "आना" to "आइल",
        "करना" to "करल",
        "बात" to "बतकही / बात",
        "लड़का" to "बबुआ / लइका",
        "लड़की" to "बबुनी / लइकी",
        "दोस्त" to "संगी / मितवा",
        "भाई" to "भैया",
        "नमस्ते" to "प्रणाम",
        "धन्यवाद" to "धन्यवाद",
        "हाँ" to "हँ"
    )

    private val bhojpuriToHindiWords = mapOf(
        "रउआ" to "आप",
        "राउर" to "आपका",
        "तूँ" to "तुम",
        "तोहार" to "तुम्हारा",
        "हमनी" to "हम",
        "हमार" to "मेरा",
        "का" to "क्या",
        "काहे" to "क्यों",
        "कइसन" to "कैसे",
        "ईहाँ" to "यहाँ",
        "ऊहाँ" to "वहाँ",
        "कहिले" to "कब",
        "बा" to "है",
        "बानी" to "हैं",
        "बाटे" to "है",
        "रहल" to "था",
        "रहलीं" to "थी",
        "रहलन" to "थे",
        "ना" to "नहीं",
        "नीक" to "अच्छा",
        "बहुते" to "बहुत",
        "तनिका" to "थोड़ा",
        "घरे" to "घर",
        "जाइल" to "जाना",
        "आइल" to "आना",
        "करल" to "करना",
        "लइका" to "लड़का",
        "लइकी" to "लड़की",
        "संगी" to "दोस्त",
        "मितवा" to "मित्र",
        "प्रणाम" to "नमस्ते",
        "बबुआ" to "बेटा / लड़का",
        "बबुनी" to "बेटी / लड़की"
    )

    fun isBhojpuriText(text: String): Boolean {
        val bhojpuriMarkers = listOf(
            "बा", "बानी", "कइसन", "काहे", "हमनी", "रउआ", "तोहार", "हमार", "राउर",
            "गइल", "आइल", "होई", "तहार", "केहू", "काल्ह", "ईहाँ", "ऊहाँ", "अइसन",
            "तनिका", "नीक", "लइका", "लइकी", "बाटे", "संगी"
        )
        val tokens = text.split(Regex("[\\s,?.!।]+"))
        var matches = 0
        for (token in tokens) {
            if (bhojpuriMarkers.contains(token.trim())) {
                matches++
            }
        }
        return matches >= 1 || (tokens.size <= 3 && matches >= 1)
    }

    fun convertHindiToBhojpuri(hindiText: String): String {
        // Handle common greeting / full sentence patterns first
        var res = hindiText.trim()
        val commonPatterns = listOf(
            Regex("नमस्ते[!,.]?\\s*आप कैसे हैं[?]?", RegexOption.IGNORE_CASE) to "प्रणाम! रउआ कइसन बानी?",
            Regex("आप कैसे हैं[?]?", RegexOption.IGNORE_CASE) to "रउआ कइसन बानी?",
            Regex("सब ठीक है[.]?", RegexOption.IGNORE_CASE) to "सब ठीक-ठाक बा।",
            Regex("आपका क्या नाम है[?]?", RegexOption.IGNORE_CASE) to "राउर का नाम ह?",
            Regex("आपका नाम क्या है[?]?", RegexOption.IGNORE_CASE) to "राउर नाम का ह?",
            Regex("मेरा नाम (.+) है", RegexOption.IGNORE_CASE) to "हमार नाम $1 बा",
            Regex("कहाँ जा रहे हैं[?]?", RegexOption.IGNORE_CASE) to "कहाँ जात बानी?",
            Regex("मैं ठीक हूँ[.]?", RegexOption.IGNORE_CASE) to "हम ठीक बानी।",
            Regex("आप क्या कर रहे हैं[?]?", RegexOption.IGNORE_CASE) to "रउआ का करत बानी?",
            Regex("यहाँ आओ", RegexOption.IGNORE_CASE) to "ईहाँ आवऽ",
            Regex("वहाँ जाओ", RegexOption.IGNORE_CASE) to "ऊहाँ जाऽ"
        )

        for ((regex, replacement) in commonPatterns) {
            if (res.matches(regex)) {
                return res.replace(regex, replacement)
            }
        }

        // Token by token translation with morphology rules
        val words = res.split(" ")
        val translated = words.map { word ->
            val clean = word.replace(Regex("[?,.!।\"]"), "")
            val punct = word.filter { it in "?,.!।\"" }
            val match = hindiToBhojpuriWords[clean] ?: clean
            match + punct
        }
        return translated.joinToString(" ")
    }

    fun convertBhojpuriToHindi(bhojpuriText: String): String {
        var res = bhojpuriText.trim()
        val commonPatterns = listOf(
            Regex("प्रणाम[!,.]?\\s*रउआ कइसन बानी[?]?", RegexOption.IGNORE_CASE) to "नमस्ते! आप कैसे हैं?",
            Regex("रउआ कइसन बानी[?]?", RegexOption.IGNORE_CASE) to "आप कैसे हैं?",
            Regex("सब ठीक-ठाक बा[.]?", RegexOption.IGNORE_CASE) to "सब ठीक है।",
            Regex("राउर का नाम ह[?]?", RegexOption.IGNORE_CASE) to "आपका क्या नाम है?",
            Regex("राउर नाम का ह[?]?", RegexOption.IGNORE_CASE) to "आपका नाम क्या है?",
            Regex("हमार नाम (.+) बा", RegexOption.IGNORE_CASE) to "मेरा नाम $1 है",
            Regex("कहाँ जात बानी[?]?", RegexOption.IGNORE_CASE) to "कहाँ जा रहे हैं?",
            Regex("हम ठीक बानी[.]?", RegexOption.IGNORE_CASE) to "मैं ठीक हूँ।",
            Regex("रउआ का करत बानी[?]?", RegexOption.IGNORE_CASE) to "आप क्या कर रहे हैं?",
            Regex("ईहाँ आवऽ", RegexOption.IGNORE_CASE) to "यहाँ आओ",
            Regex("ऊहाँ जाऽ", RegexOption.IGNORE_CASE) to "वहाँ जाओ"
        )

        for ((regex, replacement) in commonPatterns) {
            if (res.matches(regex)) {
                return res.replace(regex, replacement)
            }
        }

        val words = res.split(" ")
        val translated = words.map { word ->
            val clean = word.replace(Regex("[?,.!।\"]"), "")
            val punct = word.filter { it in "?,.!।\"" }
            val match = bhojpuriToHindiWords[clean] ?: clean
            match + punct
        }
        return translated.joinToString(" ")
    }
}
