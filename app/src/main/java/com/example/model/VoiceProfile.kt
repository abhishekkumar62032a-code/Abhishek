package com.example.model

enum class VoiceGender {
    FEMALE, MALE
}

data class VoiceProfile(
    val id: String,
    val name: String,
    val hindiTitle: String,
    val gender: VoiceGender,
    val basePitch: Float,
    val baseSpeed: Float,
    val toneDescription: String,
    val sampleTextHindi: String = "नमस्कार, यह मेरी आवाज़ का नमूना है। स्मार्ट वॉयस ट्रांसलेटर में आपका स्वागत है।",
    val sampleTextEnglish: String = "Hello, this is a sample of my voice. Welcome to Smart Voice Translator."
) {
    companion object {
        val TEN_VOICES: List<VoiceProfile> = listOf(
            // 5 Female Voices
            VoiceProfile(
                id = "female_1",
                name = "Female 1",
                hindiTitle = "महिला 1 (सुरीली व शांत)",
                gender = VoiceGender.FEMALE,
                basePitch = 1.25f,
                baseSpeed = 1.0f,
                toneDescription = "शांत, मधुर और स्वाभाविक आवाज़"
            ),
            VoiceProfile(
                id = "female_2",
                name = "Female 2",
                hindiTitle = "महिला 2 (स्पष्ट व पेशेवर)",
                gender = VoiceGender.FEMALE,
                basePitch = 1.15f,
                baseSpeed = 1.05f,
                toneDescription = "पेशेवर उद्घोषिका जैसी स्पष्ट आवाज़"
            ),
            VoiceProfile(
                id = "female_3",
                name = "Female 3",
                hindiTitle = "महिला 3 (उत्साही व तेज़)",
                gender = VoiceGender.FEMALE,
                basePitch = 1.35f,
                baseSpeed = 1.12f,
                toneDescription = "ऊर्जावान और बातचीत के अनुकूल आवाज़"
            ),
            VoiceProfile(
                id = "female_4",
                name = "Female 4",
                hindiTitle = "महिला 4 (गंभीर व सौम्य)",
                gender = VoiceGender.FEMALE,
                basePitch = 0.95f,
                baseSpeed = 0.95f,
                toneDescription = "गहरी, परिपक्व और सौम्य आवाज़"
            ),
            VoiceProfile(
                id = "female_5",
                name = "Female 5",
                hindiTitle = "महिला 5 (मधुर कथावाचक)",
                gender = VoiceGender.FEMALE,
                basePitch = 1.20f,
                baseSpeed = 0.90f,
                toneDescription = "किताबें और लंबे लेख पढ़ने के लिए आदर्श"
            ),

            // 5 Male Voices
            VoiceProfile(
                id = "male_1",
                name = "Male 1",
                hindiTitle = "पुरुष 1 (सहज व दोस्ताना)",
                gender = VoiceGender.MALE,
                basePitch = 0.90f,
                baseSpeed = 1.0f,
                toneDescription = "सामान्य दोस्ताना और सहज बातचीत"
            ),
            VoiceProfile(
                id = "male_2",
                name = "Male 2",
                hindiTitle = "पुरुष 2 (गहरी बेस व उद्घोषक)",
                gender = VoiceGender.MALE,
                basePitch = 0.72f,
                baseSpeed = 0.95f,
                toneDescription = "रेडियो व समाचार उद्घोषक जैसी गहरी आवाज़"
            ),
            VoiceProfile(
                id = "male_3",
                name = "Male 3",
                hindiTitle = "पुरुष 3 (स्पष्ट व आधुनिक)",
                gender = VoiceGender.MALE,
                basePitch = 1.02f,
                baseSpeed = 1.10f,
                toneDescription = "तेज़ और स्पष्ट तकनीकी प्रस्तुति शैली"
            ),
            VoiceProfile(
                id = "male_4",
                name = "Male 4",
                hindiTitle = "पुरुष 4 (मजबूत व प्रभावशाली)",
                gender = VoiceGender.MALE,
                basePitch = 0.82f,
                baseSpeed = 1.0f,
                toneDescription = "दृढ़ और आधिकारिक अंदाज़"
            ),
            VoiceProfile(
                id = "male_5",
                name = "Male 5",
                hindiTitle = "पुरुष 5 (अनौपचारिक व युवा)",
                gender = VoiceGender.MALE,
                basePitch = 0.96f,
                baseSpeed = 1.08f,
                toneDescription = "युवा और अनौपचारिक वार्तालाप"
            )
        )

        fun findById(id: String): VoiceProfile {
            return TEN_VOICES.find { it.id == id } ?: TEN_VOICES[0]
        }
    }
}
