package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.Language
import com.example.model.VoiceProfile
import com.example.service.ScreenTranslatorAccessibilityService
import com.example.touch.TouchSensorMode
import com.example.ui.components.HeaderBar
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode by viewModel.settingsRepo.themeMode.collectAsState()
    val touchSensorEnabled by viewModel.settingsRepo.touchSensorEnabled.collectAsState()
    val touchSensorMode by viewModel.settingsRepo.touchSensorMode.collectAsState()
    val isAccessibilityActive by ScreenTranslatorAccessibilityService.isServiceActive.collectAsState()

    // Voice & Playback settings
    val voiceSpeed by viewModel.settingsRepo.voiceSpeed.collectAsState()
    val voicePitch by viewModel.settingsRepo.voicePitch.collectAsState()
    val currentVoiceId by viewModel.settingsRepo.voiceId.collectAsState()
    val targetLangCode by viewModel.settingsRepo.targetLang.collectAsState()

    val targetLang = remember(targetLangCode) { Language.findByCode(targetLangCode) }
    val activeVoice = remember(currentVoiceId) { VoiceProfile.findById(currentVoiceId) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasMicPermission = it }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasNotificationPermission = it }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderBar(
            title = "सेटिंग्स व अनुमतियाँ (Settings)",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Voice Playback Settings Section (With TTS Speed Slider)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_voice_playback_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "वॉयस प्लेबैक सेटिंग्स (Voice Playback)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "अनुवादित टेक्स्ट-टू-स्पीच गति और पिच",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Test Voice Button
                        Button(
                            onClick = {
                                val sample = if (targetLang.code == "en") {
                                    "This is a test of the speech output at the selected playback speed."
                                } else if (targetLang.code == "bho") {
                                    "प्रणाम! ई चुनल गईल गति प वॉयस प्लेबैक के टेस्ट ह।"
                                } else {
                                    "नमस्कार, यह चुनी गई गति पर अनुवादित आवाज़ का परीक्षण है।"
                                }
                                viewModel.ttsManager.speak(sample, targetLang, activeVoice, voicePitch, voiceSpeed)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_test_voice_playback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF0A1128),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("टेस्ट करें", color = Color(0xFF0A1128), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Speed Slider Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "पठन गति (TTS Playback Speed):",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${String.format("%.2f", voiceSpeed)}x ${getSpeedLabel(voiceSpeed)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Speed Slider Control
                    Slider(
                        value = voiceSpeed,
                        onValueChange = { viewModel.settingsRepo.setVoiceSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_tts_speed_slider")
                    )

                    // Preset Speed Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(0.75f to "0.75x", 1.0f to "1.0x (सामान्य)", 1.25f to "1.25x", 1.5f to "1.5x (तेज़)").forEach { (presetSpeed, label) ->
                            val isSelected = Math.abs(voiceSpeed - presetSpeed) < 0.05f
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.settingsRepo.setVoiceSpeed(presetSpeed) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0A1128) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pitch Slider Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "पिच / टोन (Voice Pitch):",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFB74D).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${String.format("%.2f", voicePitch)}x",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFB74D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = voicePitch,
                        onValueChange = { viewModel.settingsRepo.setVoicePitch(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB74D),
                            activeTrackColor = Color(0xFFFFB74D),
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_tts_pitch_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Active Voice Selector Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .clickable { viewModel.navigateTo(Screen.VOICE_SELECTION) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "सक्रिय आवाज़: ${activeVoice.name} (${activeVoice.hindiTitle})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "बदलें ➔",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Theme Mode Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🎨 थीम सेटिंग्स (Theme Settings)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            title = "सिस्टम (Auto)",
                            isSelected = themeMode == "SYSTEM",
                            onClick = { viewModel.settingsRepo.setThemeMode("SYSTEM") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            title = "डार्क (Dark)",
                            isSelected = themeMode == "DARK",
                            onClick = { viewModel.settingsRepo.setThemeMode("DARK") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            title = "लाइट (Light)",
                            isSelected = themeMode == "LIGHT",
                            onClick = { viewModel.settingsRepo.setThemeMode("LIGHT") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Touch Sensor Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "👆 टच सेंसर रीडर (Touch Sensor Engine)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "शब्द या वाक्य टच करने पर आवाज़ चालू/बंद करें",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = touchSensorEnabled,
                            onCheckedChange = { viewModel.settingsRepo.setTouchSensorEnabled(it) },
                            modifier = Modifier.testTag("settings_touch_sensor_switch")
                        )
                    }

                    if (touchSensorEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.settingsRepo.setTouchSensorMode("WORD")
                                    viewModel.touchSensorManager.setMode(TouchSensorMode.SINGLE_WORD)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (touchSensorMode == "WORD") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = "सिंगल वर्ड",
                                    color = if (touchSensorMode == "WORD") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.settingsRepo.setTouchSensorMode("SENTENCE")
                                    viewModel.touchSensorManager.setMode(TouchSensorMode.FULL_SENTENCE)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (touchSensorMode == "SENTENCE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = "फुल सेंटेंस",
                                    color = if (touchSensorMode == "SENTENCE") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Permissions Explained in Hindi
            Text(
                text = "🛡️ सिस्टम अनुमतियाँ और स्पष्ट कारण (Permissions Explained):",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // 1. Microphone
            PermissionExplanationCard(
                icon = Icons.Default.Mic,
                name = "1. माइक्रोफोन अनुमति (Microphone Permission)",
                reason = "कारण: वॉयस रिकॉर्डिंग, वास्तविक समय में आवाज़ से टेक्स्ट (Speech-to-Text) और बातचीत अनुवाद के लिए आवश्यक है। बिना आपकी जानकारी के कोई रिकॉर्डिंग नहीं की जाती।",
                isGranted = hasMicPermission,
                onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            // 2. Display Over Other Apps
            PermissionExplanationCard(
                icon = Icons.Default.PictureInPicture,
                name = "2. अन्य ऐप्स के ऊपर डिस्प्ले (Display Over Other Apps)",
                reason = "कारण: WhatsApp, Facebook, Telegram आदि ऐप्स पर तैरता हुआ फ्लोटिंग ट्रांसलेटर बटन दिखाने के लिए यह अनुमति जरूरी है ताकि आप चैट छोड़े बिना अनुवाद कर सकें।",
                isGranted = hasOverlayPermission,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }
            )

            // 3. Accessibility Service
            PermissionExplanationCard(
                icon = Icons.Default.Accessibility,
                name = "3. एक्सेसिबिलिटी स्क्रीन रीडर (Accessibility Service)",
                reason = "कारण: WhatsApp या स्क्रीन पर दिख रहे संदेशों को स्वतः पढ़कर अनुवादित करने के लिए एक्सेसिबिलिटी सेवा का उपयोग होता है। बैंक विवरण और पासवर्ड कभी नहीं पढ़े जाते।",
                isGranted = isAccessibilityActive,
                onRequest = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // 4. Notifications
            PermissionExplanationCard(
                icon = Icons.Default.Notifications,
                name = "4. नोटिफिकेशन अनुमति (Notifications)",
                reason = "कारण: बैकग्राउंड में फ्लोटिंग ट्रांसलेटर सर्विस को सक्रिय रखने और तुरंत एक्सेस करने के लिए सिस्टम नोटिफिकेशन आवश्यक है।",
                isGranted = hasNotificationPermission,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            // Security & Privacy Statement
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "डेटा सुरक्षा और गोपनीयता (Privacy & Security)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• आपकी सभी ऑडियो रिकॉर्डिंग केवल आपके डिवाइस के स्थानीय स्टोरेज में सुरक्षित रहती हैं।\n" +
                                "• कोई भी निजी बातचीत क्लाउड पर बिना आपकी अनुमति के अपलोड नहीं की जाती।\n" +
                                "• आप जब चाहें इतिहास स्क्रीन से एक या सभी रिकॉर्ड्स को पूरी तरह डिलीट कर सकते हैं।\n" +
                                "• सुरक्षित ऑफ़लाइन मोड हमेशा उपलब्ध है।",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Storage & Clear Data
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "कैश व इतिहास साफ़ करें",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "सभी सहेजे गए अनुवाद व ऑडियो फाइलें मिटाएँ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            Toast.makeText(context, "समस्त इतिहास साफ़ कर दिया गया", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        modifier = Modifier.testTag("settings_clear_all_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("साफ़ करें", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun getSpeedLabel(speed: Float): String {
    return when {
        speed < 0.85f -> "(धीमा / Slow)"
        speed in 0.85f..1.15f -> "(सामान्य / Normal)"
        speed in 1.16f..1.4f -> "(तेज़ / Fast)"
        else -> "(अधिक तेज़ / Very Fast)"
    }
}

@Composable
fun ThemeOptionButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PermissionExplanationCard(
    icon: ImageVector,
    name: String,
    reason: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFF7F1D1D).copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                if (!isGranted) {
                    Button(
                        onClick = onRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("अनुमति दें", fontSize = 11.sp)
                    }
                } else {
                    Text(
                        text = "सक्रिय ✓",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
