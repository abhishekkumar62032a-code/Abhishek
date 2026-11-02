package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.RecordingState
import com.example.audio.SpeechRecognizerState
import com.example.model.Language
import com.example.ui.components.HeaderBar
import com.example.viewmodel.MainViewModel
import java.io.File

@Composable
fun VoiceRecordingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by viewModel.voiceRecorderManager.recordingState.collectAsState()
    val speechState by viewModel.speechRecognizerHelper.state.collectAsState()
    val playbackState by viewModel.audioPlayerManager.playbackState.collectAsState()
    val sourceLangCode by viewModel.settingsRepo.sourceLang.collectAsState()
    val targetLangCode by viewModel.settingsRepo.targetLang.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val lastResult by viewModel.lastTranslationResult.collectAsState()

    val sourceLang = remember(sourceLangCode) { Language.findByCode(sourceLangCode) }
    val targetLang = remember(targetLangCode) { Language.findByCode(targetLangCode) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var recognizedText by remember { mutableStateOf("") }
    var completedAudioFile by remember { mutableStateOf<File?>(null) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            android.widget.Toast.makeText(context, "माइक की अनुमति मिल गई! रिकॉर्डिंग शुरू हो रही है...", android.widget.Toast.LENGTH_SHORT).show()
            recognizedText = ""
            saveStatusMsg = null
            completedAudioFile = null
            viewModel.audioPlayerManager.stop()
            val started = viewModel.voiceRecorderManager.startRecording()
            if (started) {
                val listenLang = if (sourceLang.code == "auto") Language.findByCode("hi") else sourceLang
                viewModel.speechRecognizerHelper.startListening(listenLang)
            }
        } else {
            android.widget.Toast.makeText(context, "ऑडियो रिकॉर्ड करने के लिए माइक अनुमति आवश्यक है", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Sync speech recognition results
    LaunchedEffect(speechState) {
        when (val s = speechState) {
            is SpeechRecognizerState.PartialResult -> {
                recognizedText = s.text
            }
            is SpeechRecognizerState.FinalResult -> {
                recognizedText = s.text
            }
            else -> {}
        }
    }

    // When recording completes
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Completed) {
            val comp = recordingState as RecordingState.Completed
            completedAudioFile = comp.audioFile
            recordingDuration = comp.durationMs
            // Stop speech recognizer
            viewModel.speechRecognizerHelper.stopListening()

            // If we have recognized text, auto translate!
            if (recognizedText.isNotBlank()) {
                viewModel.translateRecordedText(
                    text = recognizedText,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    audioFile = completedAudioFile,
                    durationMs = recordingDuration
                )
                saveStatusMsg = "रिकॉर्डिंग और अनुवाद इतिहास में सुरक्षित किया गया"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.audioPlayerManager.stop()
            viewModel.speechRecognizerHelper.stopListening()
        }
    }

    val isRecording = recordingState is RecordingState.Recording
    val isPaused = recordingState is RecordingState.Paused
    val currentAmp = if (recordingState is RecordingState.Recording) {
        (recordingState as RecordingState.Recording).amplitude
    } else 0f

    val currentDurationMs = when (recordingState) {
        is RecordingState.Recording -> (recordingState as RecordingState.Recording).durationMs
        is RecordingState.Paused -> (recordingState as RecordingState.Paused).durationMs
        is RecordingState.Completed -> recordingDuration
        else -> 0L
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderBar(
            title = "वॉयस रिकॉर्डिंग (Voice Recording)",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language selector card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "स्रोत भाषा (Source)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${sourceLang.flagEmoji} ${sourceLang.nativeName}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.swapLanguages()
                            Toast.makeText(context, "${targetLang.nativeName} ⇄ ${sourceLang.nativeName}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("recording_swap_lang_button")
                    ) {
                        Text(text = "⇄", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "लक्ष्य भाषा (Target)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${targetLang.flagEmoji} ${targetLang.nativeName}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer & Status
            Text(
                text = formatDuration(currentDurationMs),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = if (isRecording) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = when {
                    isRecording -> "● रिकॉर्डिंग जारी है... (Recording in progress)"
                    isPaused -> "❚❚ रिकॉर्डिंग विरामित (Paused)"
                    completedAudioFile != null -> "✓ रिकॉर्डिंग पूर्ण (Recording Finished)"
                    else -> "रिकॉर्डिंग शुरू करने के लिए माइक बटन दबाएँ"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = when {
                    isRecording -> Color(0xFF00E5FF)
                    isPaused -> Color(0xFFFFB74D)
                    completedAudioFile != null -> Color(0xFF10B981)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Waveform Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val barCount = 36
                    val barWidth = size.width / (barCount * 1.5f)
                    val maxH = size.height * 0.9f

                    for (i in 0 until barCount) {
                        val factor = if (isRecording) {
                            val wave = Math.sin((i.toDouble() / barCount) * Math.PI).toFloat()
                            val jitter = ((i * 37) % 10) / 10f * 0.4f
                            ((currentAmp * wave) + jitter).coerceIn(0.1f, 1f)
                        } else 0.1f

                        val h = (maxH * factor).coerceAtLeast(4f)
                        val x = i * (barWidth * 1.5f)
                        val y = (size.height - h) / 2f

                        drawRoundRect(
                            color = if (isRecording) Color(0xFF00E5FF) else Color(0xFF64748B),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Recording Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel / Delete
                IconButton(
                    onClick = {
                        viewModel.voiceRecorderManager.cancelRecording()
                        viewModel.speechRecognizerHelper.stopListening()
                        viewModel.audioPlayerManager.stop()
                        completedAudioFile = null
                        recognizedText = ""
                        saveStatusMsg = null
                        Toast.makeText(context, "रिकॉर्डिंग हटा दी गई (Deleted)", Toast.LENGTH_SHORT).show()
                    },
                    enabled = isRecording || isPaused || completedAudioFile != null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording || isPaused || completedAudioFile != null)
                                Color(0xFFEF4444).copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .testTag("recording_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "हटाएँ (Delete)",
                        tint = if (isRecording || isPaused || completedAudioFile != null) Color(0xFFEF4444) else Color.Gray
                    )
                }

                // Primary Start / Stop Mic Button (Direct clickable 86dp circular box)
                val toggleRecordAction: () -> Unit = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (isRecording || isPaused) {
                        viewModel.voiceRecorderManager.stopRecording()
                        Toast.makeText(context, "रिकॉर्डिंग समाप्त। अनुवाद हो रहा है...", Toast.LENGTH_SHORT).show()
                    } else {
                        recognizedText = ""
                        saveStatusMsg = null
                        completedAudioFile = null
                        viewModel.audioPlayerManager.stop()
                        val started = viewModel.voiceRecorderManager.startRecording()
                        if (started) {
                            val listenLang = if (sourceLang.code == "auto") Language.findByCode("hi") else sourceLang
                            viewModel.speechRecognizerHelper.startListening(listenLang)
                            Toast.makeText(context, "रिकॉर्डिंग शुरू हुई... बोलिए", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "रिकॉर्डर शुरू नहीं हो सका", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(if (isRecording) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color(0xFFEF4444) else Color(0xFF00E5FF)
                        )
                        .clickable { toggleRecordAction() }
                        .testTag("recording_main_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording || isPaused) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording || isPaused) "रोकें (Stop)" else "शुरू करें (Record)",
                        tint = if (isRecording) Color.White else Color(0xFF0A1128),
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Pause / Resume Button
                IconButton(
                    onClick = {
                        if (isRecording) {
                            viewModel.voiceRecorderManager.pauseRecording()
                            viewModel.speechRecognizerHelper.stopListening()
                            Toast.makeText(context, "रिकॉर्डिंग रोकी गई (Paused)", Toast.LENGTH_SHORT).show()
                        } else if (isPaused) {
                            viewModel.voiceRecorderManager.resumeRecording()
                            Toast.makeText(context, "रिकॉर्डिंग पुनः चालू (Resumed)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isRecording || isPaused,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording || isPaused) Color(0xFFFFB74D).copy(alpha = 0.2f) else Color.Transparent
                        )
                        .testTag("recording_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "पुनः चालू करें (Resume)" else "विराम (Pause)",
                        tint = if (isRecording || isPaused) Color(0xFFFFB74D) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Audio Playback Player (If Audio File Exists)
            if (completedAudioFile != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (playbackState.isPlaying) {
                                            viewModel.audioPlayerManager.pause()
                                        } else {
                                            completedAudioFile?.let { viewModel.audioPlayerManager.play(it.absolutePath) }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                        .testTag("recording_play_audio_button")
                                ) {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "प्ले करें",
                                        tint = Color(0xFF0A1128)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "रिकॉर्डेड ऑडियो चलाएँ (Play Recording)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${formatDuration(playbackState.currentPositionMs.toLong())} / ${formatDuration(playbackState.durationMs.toLong())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Slider(
                            value = playbackState.progress,
                            onValueChange = { frac ->
                                val targetMs = (frac * playbackState.durationMs).toInt()
                                viewModel.audioPlayerManager.seekTo(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Speech to Text Result Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝 आवाज़ से टेक्स्ट (Speech-to-Text)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (lastResult?.detectedLanguage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF065F46)
                            ) {
                                Text(
                                    text = "पहचानी गई: ${lastResult?.detectedLanguage?.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = recognizedText,
                        onValueChange = { recognizedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recording_speech_input_field"),
                        placeholder = {
                            Text(
                                text = "माइक में बोलें, बोला गया टेक्स्ट यहाँ दिखेगा या स्वयं लिखें...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (recognizedText.isNotBlank()) {
                                    viewModel.translateRecordedText(
                                        text = recognizedText,
                                        sourceLang = sourceLang,
                                        targetLang = targetLang,
                                        audioFile = completedAudioFile,
                                        durationMs = recordingDuration
                                    )
                                    saveStatusMsg = "अनुवाद अपडेट किया गया"
                                    Toast.makeText(context, "अनुवाद किया जा रहा है...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "कृपया पहले माइक से बोलें या टेक्स्ट लिखें", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isTranslating,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("recording_translate_button")
                        ) {
                            Text(if (isTranslating) "अनुवाद हो रहा है..." else "अनुवाद करें (Translate)")
                        }
                    }
                }
            }

            // Translation Output Card
            if (lastResult != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 अनुवादित परिणाम (${targetLang.nativeName})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Copy Button
                                IconButton(
                                    onClick = {
                                        lastResult?.translatedText?.let { t ->
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Translation", t)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "अनुवाद कॉपी किया गया", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .testTag("recording_copy_translation_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "कॉपी करें",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Speak (TTS) Button
                                IconButton(
                                    onClick = {
                                        lastResult?.translatedText?.let {
                                            viewModel.speak(it, targetLang)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                        .testTag("recording_speak_translation_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "आवाज़ में सुनें (TTS)",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        if (!lastResult?.specialNotice.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6B21A8)
                            ) {
                                Text(
                                    text = "★ ${lastResult?.specialNotice}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF3E8FF),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = lastResult?.translatedText.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (saveStatusMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ $saveStatusMsg",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
