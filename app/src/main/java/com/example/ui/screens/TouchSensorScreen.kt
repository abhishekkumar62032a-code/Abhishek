package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Language
import com.example.model.VoiceProfile
import com.example.touch.ParsedSentence
import com.example.touch.ReadingTarget
import com.example.touch.TouchSensorManager
import com.example.touch.TouchSensorMode
import com.example.ui.components.HeaderBar
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouchSensorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val touchState by viewModel.touchSensorManager.state.collectAsState()
    val isEnabled by viewModel.settingsRepo.touchSensorEnabled.collectAsState()
    val currentVoiceId by viewModel.settingsRepo.voiceId.collectAsState()
    val pitch by viewModel.settingsRepo.voicePitch.collectAsState()
    val speed by viewModel.settingsRepo.voiceSpeed.collectAsState()
    val sourceLangCode by viewModel.settingsRepo.sourceLang.collectAsState()
    val targetLangCode by viewModel.settingsRepo.targetLang.collectAsState()

    val sourceLang = remember(sourceLangCode) { Language.findByCode(sourceLangCode) }
    val targetLang = remember(targetLangCode) { Language.findByCode(targetLangCode) }
    val activeVoice = remember(currentVoiceId) { VoiceProfile.findById(currentVoiceId) }

    // Sample bilingual texts including Hindi, English, and Bhojpuri
    val samplePresetOriginal = remember {
        "नमस्ते! स्मार्ट वॉयस ट्रांसलेटर में आपका स्वागत है। आप जिस शब्द को छूएंगे, वही शब्द स्पष्ट आवाज़ में सुनाई देगा। यदि आप पूरा वाक्य सुनना चाहते हैं, तो वाक्य पर टच करें। यह प्रणाली सीखने और पठन के लिए अत्यंत उपयोगी है।"
    }
    val samplePresetTranslated = remember {
        "Hello! Welcome to Smart Voice Translator. Whichever word you touch will be spoken in a clear voice. If you want to listen to the entire sentence, tap on the sentence. This system is extremely useful for learning and reading."
    }

    var customTextOriginal by remember { mutableStateOf(samplePresetOriginal) }
    var customTextTranslated by remember { mutableStateOf(samplePresetTranslated) }

    var selectedSampleIdx by remember { mutableIntStateOf(0) }
    var readingTarget by remember { mutableStateOf(ReadingTarget.ORIGINAL) }

    val activeText = if (readingTarget == ReadingTarget.ORIGINAL) customTextOriginal else customTextTranslated
    val activeLang = if (readingTarget == ReadingTarget.ORIGINAL) {
        if (sourceLang.code == "auto") Language.findByCode("hi") else sourceLang
    } else targetLang

    val parsedSentences = remember(activeText) {
        TouchSensorManager.parseText(activeText)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.touchSensorManager.stopReading()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderBar(
            title = "टच सेंसर रीडर (Touch Sensor)",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Touch Sensor ON / OFF Master Switch Card
            Card(
                onClick = {
                    val newState = !isEnabled
                    viewModel.settingsRepo.setTouchSensorEnabled(newState)
                    viewModel.touchSensorManager.setEnabled(newState)
                    val msg = if (newState) "टच सेंसर चालू किया गया (Enabled)" else "टच सेंसर बंद किया गया (Disabled)"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("touch_sensor_master_toggle_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) Color(0xFF0F1A33) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isEnabled) Color(0xFF00E5FF) else Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isEnabled) Color(0xFF00E5FF).copy(alpha = 0.2f)
                                    else Color.Gray.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (isEnabled) Color(0xFF00E5FF) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "टच सेंसर रीडर (Touch Sensor)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnabled) "🟢 चालू है - शब्द या वाक्य टच करें" else "⚪ बंद है - चालू करने के लिए टैप करें",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isEnabled) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { newState ->
                            viewModel.settingsRepo.setTouchSensorEnabled(newState)
                            viewModel.touchSensorManager.setEnabled(newState)
                            val msg = if (newState) "टच सेंसर चालू किया गया (Enabled)" else "टच सेंसर बंद किया गया (Disabled)"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("touch_sensor_switch_button")
                    )
                }
            }

            // Mode Selector: SINGLE WORD vs FULL SENTENCE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "टच मोड चुनें (Touch Mode)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.touchSensorManager.setMode(TouchSensorMode.SINGLE_WORD) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("touch_mode_single_word_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (touchState.mode == TouchSensorMode.SINGLE_WORD)
                                    Color(0xFF00E5FF)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "🔤 सिंगल वर्ड टच",
                                color = if (touchState.mode == TouchSensorMode.SINGLE_WORD) Color(0xFF0A1128) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.touchSensorManager.setMode(TouchSensorMode.FULL_SENTENCE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("touch_mode_full_sentence_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (touchState.mode == TouchSensorMode.FULL_SENTENCE)
                                    Color(0xFFFF9E00)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "📜 फुल सेंटेंस टच",
                                color = if (touchState.mode == TouchSensorMode.FULL_SENTENCE) Color(0xFF0A1128) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Target Text Toggle: Original vs Translated
            TabRow(
                selectedTabIndex = if (readingTarget == ReadingTarget.ORIGINAL) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = readingTarget == ReadingTarget.ORIGINAL,
                    onClick = {
                        readingTarget = ReadingTarget.ORIGINAL
                        viewModel.touchSensorManager.setReadingTarget(ReadingTarget.ORIGINAL)
                    },
                    text = { Text("मूल पाठ (${activeLang.nativeName})", fontSize = 13.sp) },
                    modifier = Modifier.testTag("touch_tab_original")
                )
                Tab(
                    selected = readingTarget == ReadingTarget.TRANSLATED,
                    onClick = {
                        readingTarget = ReadingTarget.TRANSLATED
                        viewModel.touchSensorManager.setReadingTarget(ReadingTarget.TRANSLATED)
                    },
                    text = { Text("अनुवादित पाठ (${targetLang.nativeName})", fontSize = 13.sp) },
                    modifier = Modifier.testTag("touch_tab_translated")
                )
            }

            // Interactive Text Reading Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1A33)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (touchState.mode == TouchSensorMode.SINGLE_WORD)
                                    "टच करें: किसी भी शब्द पर टैप करें"
                                else
                                    "टच करें: वाक्य पर टैप करें",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E5FF)
                            )
                        }

                        Text(
                            text = "आवाज़: ${activeVoice.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Render Sentence & Word Chips
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parsedSentences.forEachIndexed { sIndex, sentence ->
                            val isSentenceActive = touchState.activeSentenceIndex == sIndex

                            val sentenceBgColor by animateColorAsState(
                                targetValue = if (isSentenceActive) Color(0xFF1E3A8A) else Color.Transparent,
                                animationSpec = tween(250),
                                label = "sentenceBg"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(sentenceBgColor)
                                    .padding(if (isSentenceActive) 6.dp else 0.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sentence.words.forEachIndexed { wIndex, word ->
                                        val isWordActive = isSentenceActive && touchState.activeWordIndex == wIndex

                                        val wordBgColor by animateColorAsState(
                                            targetValue = when {
                                                isWordActive -> Color(0xFF00E5FF)
                                                isSentenceActive -> Color(0xFF3B82F6).copy(alpha = 0.35f)
                                                else -> Color(0xFF1E293B)
                                            },
                                            animationSpec = tween(200),
                                            label = "wordBg"
                                        )

                                        val wordTextColor by animateColorAsState(
                                            targetValue = when {
                                                isWordActive -> Color(0xFF0A1128)
                                                isSentenceActive -> Color.White
                                                else -> Color(0xFFE2E8F0)
                                            },
                                            animationSpec = tween(200),
                                            label = "wordText"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(wordBgColor)
                                                .clickable {
                                                    viewModel.touchSensorManager.handleWordTouch(
                                                        sentenceIndex = sIndex,
                                                        wordIndex = wIndex,
                                                        parsedSentence = sentence,
                                                        language = activeLang,
                                                        voiceProfile = activeVoice,
                                                        pitch = pitch,
                                                        speed = speed
                                                    )
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                                .testTag("touch_word_${sIndex}_${wIndex}")
                                        ) {
                                            Text(
                                                text = word.text,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isWordActive || isSentenceActive) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 16.sp
                                                ),
                                                color = wordTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Status Bar
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 ${touchState.statusMessage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Continuous Reading Controls (Read Full Page, Pause, Resume, Stop)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "पूर्ण पृष्ठ पठन (Read Full Page)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Read Full Page
                        Button(
                            onClick = {
                                viewModel.touchSensorManager.startFullPageReading(
                                    sentences = parsedSentences,
                                    language = activeLang,
                                    voiceProfile = activeVoice,
                                    pitch = pitch,
                                    speed = speed
                                )
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("touch_read_full_page_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("पूरा पढ़ें", fontSize = 12.sp)
                        }

                        // Pause / Resume
                        if (touchState.isContinuousReading && !touchState.isPaused) {
                            Button(
                                onClick = { viewModel.touchSensorManager.pauseReading() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("touch_pause_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9E00))
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("विराम", fontSize = 12.sp, color = Color.Black)
                            }
                        } else if (touchState.isContinuousReading && touchState.isPaused) {
                            Button(
                                onClick = { viewModel.touchSensorManager.resumeReading(activeLang, activeVoice, pitch, speed) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("touch_resume_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("चालू", fontSize = 12.sp)
                            }
                        }

                        // Stop
                        Button(
                            onClick = { viewModel.touchSensorManager.stopReading() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("touch_stop_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("रोकें", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Custom Text Input Expander
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "कस्टम टेक्स्ट डालें (Try Your Own Text):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = if (readingTarget == ReadingTarget.ORIGINAL) customTextOriginal else customTextTranslated,
                        onValueChange = {
                            if (readingTarget == ReadingTarget.ORIGINAL) customTextOriginal = it else customTextTranslated = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("touch_custom_text_input"),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
