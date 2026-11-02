package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.BhojpuriDialectEngine
import com.example.model.Language
import com.example.model.TranslationRecord
import com.example.model.VoiceProfile
import com.example.util.HistoryExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Voice Translator", appName)
  }

  @Test
  fun `verify ten languages and ten voices`() {
    assertEquals(10, Language.SUPPORTED_LANGUAGES.size)
    assertEquals(10, VoiceProfile.TEN_VOICES.size)
    val femaleCount = VoiceProfile.TEN_VOICES.count { it.gender == com.example.model.VoiceGender.FEMALE }
    val maleCount = VoiceProfile.TEN_VOICES.count { it.gender == com.example.model.VoiceGender.MALE }
    assertEquals(5, femaleCount)
    assertEquals(5, maleCount)
  }

  @Test
  fun `verify bhojpuri dialect transformation`() {
    val hindiText = "नमस्ते, आप कैसे हैं?"
    val bhojpuri = BhojpuriDialectEngine.convertHindiToBhojpuri(hindiText)
    assertTrue(bhojpuri.contains("बानी") || bhojpuri.contains("रउआ") || bhojpuri.contains("प्रणाम"))
  }

  @Test
  fun `verify csv and json history export`() {
    val sampleRecords = listOf(
      TranslationRecord(
        id = 101L,
        timestamp = 1793554000000L,
        sourceLangCode = "hi",
        targetLangCode = "en",
        originalText = "नमस्ते, आप कैसे हैं?",
        translatedText = "Hello, how are you?",
        audioFilePath = "/data/user/0/rec_1.m4a",
        category = "RECORDING",
        durationMs = 2500L
      ),
      TranslationRecord(
        id = 102L,
        timestamp = 1793554100000L,
        sourceLangCode = "en",
        targetLangCode = "bho",
        originalText = "Welcome to our home",
        translatedText = "हमार घरे रउआ सब के स्वागत बा",
        category = "CHAT"
      )
    )

    // Test CSV
    val csv = HistoryExportHelper.convertToCsv(sampleRecords)
    assertTrue(csv.contains("id,timestamp,formatted_date,category,source_language,target_language"))
    assertTrue(csv.contains("नमस्ते, आप कैसे हैं?"))
    assertTrue(csv.contains("Hello, how are you?"))
    assertTrue(csv.contains("RECORDING"))

    // Test JSON
    val json = HistoryExportHelper.convertToJson(sampleRecords)
    assertTrue(json.contains("\"app_name\": \"Smart Voice Translator\""))
    assertTrue(json.contains("\"total_records\": 2"))
    assertTrue(json.contains("\"original_text\": \"नमस्ते, आप कैसे हैं?\""))
    assertTrue(json.contains("\"target_language\": \"bho\""))
  }
}
