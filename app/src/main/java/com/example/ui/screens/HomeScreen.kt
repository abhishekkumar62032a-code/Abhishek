package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Language
import com.example.model.VoiceProfile
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

data class HomeCardItem(
    val id: String,
    val screen: Screen,
    val titleHindi: String,
    val titleEnglish: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val tag: String
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sourceLangCode by viewModel.settingsRepo.sourceLang.collectAsState()
    val targetLangCode by viewModel.settingsRepo.targetLang.collectAsState()
    val voiceId by viewModel.settingsRepo.voiceId.collectAsState()
    val isFloatingActive by viewModel.isFloatingOverlayActive.collectAsState()

    val sourceLang = remember(sourceLangCode) { Language.findByCode(sourceLangCode) }
    val targetLang = remember(targetLangCode) { Language.findByCode(targetLangCode) }
    val activeVoice = remember(voiceId) { VoiceProfile.findById(voiceId) }

    val isOnline = remember { checkIsOnline(context) }

    val cards = remember {
        listOf(
            HomeCardItem(
                id = "recording",
                screen = Screen.VOICE_RECORDING,
                titleHindi = "वॉयस रिकॉर्डिंग",
                titleEnglish = "Voice Recording",
                subtitle = "रिकॉर्ड, स्वतः पहचान व अनुवाद",
                icon = Icons.Default.Mic,
                accentColor = Color(0xFF00E5FF),
                tag = "home_card_voice_recording"
            ),
            HomeCardItem(
                id = "chat",
                screen = Screen.CHAT_TRANSLATOR,
                titleHindi = "चैट ट्रांसलेटर",
                titleEnglish = "Chat Translator",
                subtitle = "दो-तरफा लाइव वार्तालाप अनुवाद",
                icon = Icons.Default.Chat,
                accentColor = Color(0xFF38BDF8),
                tag = "home_card_chat_translator"
            ),
            HomeCardItem(
                id = "floating",
                screen = Screen.FLOATING_TRANSLATOR,
                titleHindi = "फ्लोटिंग ट्रांसलेटर",
                titleEnglish = "Floating Translator",
                subtitle = "स्क्रीन टेक्स्ट व WhatsApp रीडर",
                icon = Icons.Default.PictureInPicture,
                accentColor = Color(0xFFFF9E00),
                tag = "home_card_floating_translator"
            ),
            HomeCardItem(
                id = "language",
                screen = Screen.LANGUAGE_SELECTION,
                titleHindi = "भाषा चयन",
                titleEnglish = "Select Language",
                subtitle = "10 भाषाएँ (भोजपुरी विशेष सहित)",
                icon = Icons.Default.Language,
                accentColor = Color(0xFF10B981),
                tag = "home_card_select_language"
            ),
            HomeCardItem(
                id = "voice",
                screen = Screen.VOICE_SELECTION,
                titleHindi = "10 आवाज़ें",
                titleEnglish = "Voice Selection",
                subtitle = "5 महिला व 5 पुरुष विशिष्ट आवाज़ें",
                icon = Icons.Default.RecordVoiceOver,
                accentColor = Color(0xFFA855F7),
                tag = "home_card_voice_selection"
            ),
            HomeCardItem(
                id = "touch",
                screen = Screen.TOUCH_SENSOR,
                titleHindi = "टच सेंसर रीडर",
                titleEnglish = "Touch Sensor",
                subtitle = "सिंगल वर्ड व फुल सेंटेंस टच पठन",
                icon = Icons.Default.TouchApp,
                accentColor = Color(0xFFEC4899),
                tag = "home_card_touch_sensor"
            ),
            HomeCardItem(
                id = "history",
                screen = Screen.TRANSLATION_HISTORY,
                titleHindi = "अनुवाद इतिहास",
                titleEnglish = "Translation History",
                subtitle = "ऑडियो रिकॉर्डिंग्स व सहेजे अनुवाद",
                icon = Icons.Default.History,
                accentColor = Color(0xFF6366F1),
                tag = "home_card_translation_history"
            ),
            HomeCardItem(
                id = "settings",
                screen = Screen.SETTINGS,
                titleHindi = "सेटिंग्स व अनुमतियाँ",
                titleEnglish = "Settings",
                subtitle = "माइक्रोफोन, एक्सेसिबिलिटी व थीम्स",
                icon = Icons.Default.Settings,
                accentColor = Color(0xFF64748B),
                tag = "home_card_settings"
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Hero Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF003543),
                                Color(0xFF0F1A33),
                                Color(0xFF162A5A)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App Icon or Glow Badge
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D1B2A))
                                .border(2.dp, Color(0xFF00E5FF), CircleShape)
                                .scale(pulseScale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Logo",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMART VOICE TRANSLATOR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "स्मार्ट वॉयस ट्रांसलेटर",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF38D9F5)
                            )
                        }

                        // Network badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isOnline) Color(0xFF065F46) else Color(0xFF7F1D1D))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isOnline) "🟢 ऑनलाइन" else "🔴 ऑफ़लाइन",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Language & Voice Info Pill Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.navigateTo(Screen.LANGUAGE_SELECTION) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sourceLang.flagEmoji} ${sourceLang.nativeName} ➔ ${targetLang.flagEmoji} ${targetLang.nativeName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.navigateTo(Screen.VOICE_SELECTION) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎙️ ${activeVoice.name}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFB74D)
                                )
                            }
                        }
                    }

                    if (isFloatingActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E1065))
                                .clickable { viewModel.navigateTo(Screen.FLOATING_TRANSLATOR) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⚡ फ्लोटिंग ओवरले सक्रिय है (स्क्रीन पर तैरता बटन दृश्यमान है)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE9D5FF)
                            )
                        }
                    }
                }
            }
        }

        // 8 Main Action Cards in a 2-Column Grid
        Text(
            text = "मुख्य सुविधाएँ (Main Features)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { card ->
                Card(
                    onClick = { viewModel.navigateTo(card.screen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag(card.tag),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(card.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = card.icon,
                                    contentDescription = card.titleHindi,
                                    tint = card.accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = card.titleHindi,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = card.titleEnglish,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = card.accentColor,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = card.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun checkIsOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
