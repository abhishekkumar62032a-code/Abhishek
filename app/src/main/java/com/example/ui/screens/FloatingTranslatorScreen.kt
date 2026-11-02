package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ScreenTranslatorAccessibilityService
import com.example.ui.components.HeaderBar
import com.example.viewmodel.MainViewModel

@Composable
fun FloatingTranslatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFloatingActive by viewModel.isFloatingOverlayActive.collectAsState()
    val floatingSide by viewModel.settingsRepo.floatingSide.collectAsState()
    val isAccessibilityActive by ScreenTranslatorAccessibilityService.isServiceActive.collectAsState()

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    // Refresh permission status on resume
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
            title = "फ्लोटिंग ट्रांसलेटर (Floating Translator)",
            onBackClick = { viewModel.navigateBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Overlay Switch Card
            val toggleOverlay: () -> Unit = {
                if (!hasOverlayPermission) {
                    openOverlayPermissionSettings(context)
                    Toast.makeText(
                        context,
                        "कृपया पहले 'अन्य ऐप्स के ऊपर डिस्प्ले' की अनुमति चालू करें",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    viewModel.toggleFloatingService()
                    val willBeActive = !isFloatingActive
                    Toast.makeText(
                        context,
                        if (willBeActive) "फ्लोटिंग ट्रांसलेटर सक्रिय किया गया ✓" else "फ्लोटिंग ट्रांसलेटर बंद किया गया",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Card(
                onClick = { toggleOverlay() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("floating_overlay_main_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "फ्लोटिंग ट्रांसलेटर ओवरले",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "स्क्रीन पर तैरता बटन दिखाएं जो अन्य सभी ऐप्स के ऊपर काम करेगा",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isFloatingActive,
                        onCheckedChange = { toggleOverlay() },
                        modifier = Modifier.testTag("floating_overlay_toggle_switch")
                    )
                }
            }

            // Permission Status Cards
            Text(
                text = "आवश्यक अनुमतियाँ (Required Permissions)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // 1. Overlay Permission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasOverlayPermission) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF7F1D1D).copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "अन्य ऐप्स के ऊपर डिस्प्ले (Overlay)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        if (!hasOverlayPermission) {
                            Button(
                                onClick = { openOverlayPermissionSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.testTag("request_overlay_permission_button")
                            ) {
                                Text("अनुमति दें", fontSize = 12.sp)
                            }
                        } else {
                            Text(text = "सक्रिय ✓", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "कारण: यह अनुमति फ्लोटिंग बटन और अनुवादक विंडो को WhatsApp, Instagram, Telegram या Facebook जैसी किसी भी ऐप के ऊपर बनाए रखने के लिए अनिवार्य है।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // 2. Accessibility Permission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityActive) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF78350F).copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Accessibility,
                                contentDescription = null,
                                tint = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFFFB74D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "एक्सेसिबिलिटी सेवा (Screen OCR Reader)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        if (!isAccessibilityActive) {
                            Button(
                                onClick = { openAccessibilitySettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9E00)),
                                modifier = Modifier.testTag("request_accessibility_permission_button")
                            ) {
                                Text("सक्षम करें", fontSize = 12.sp, color = Color.Black)
                            }
                        } else {
                            Text(text = "सक्रिय ✓", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "कारण: यह सेवा अन्य ऐप्स (जैसे WhatsApp चैट) के ऑन-स्क्रीन टेक्स्ट को बिना कॉपी-पेस्ट किए तुरंत पढ़कर अनुवाद करने की क्षमता देती है। पासवर्ड व संवेदनशील फ़ील्ड कभी नहीं पढ़े जाते।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Floating Position Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "फ्लोटिंग बटन स्थिति (Initial Screen Side)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.settingsRepo.setFloatingSide("LEFT") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (floatingSide == "LEFT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "बाईं ओर (Left)",
                                color = if (floatingSide == "LEFT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { viewModel.settingsRepo.setFloatingSide("RIGHT") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (floatingSide == "RIGHT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "दाईं ओर (Right)",
                                color = if (floatingSide == "RIGHT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Hindi Guide Steps Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📖 उपयोग करने का आसान तरीका (How to Use):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val steps = listOf(
                        "1. ऊपर दिए गए स्विच से 'फ्लोटिंग ट्रांसलेटर ओवरले' चालू करें।",
                        "2. स्क्रीन के किनारे एक गोल फ्लोटिंग आइकन दिखाई देगा।",
                        "3. अपनी कोई भी ऐप खोलें (जैसे WhatsApp, Telegram, Instagram, Browser)।",
                        "4. फ्लोटिंग आइकन पर टैप करें - अनुवादक पैनल खुल जाएगा।",
                        "5. 'स्क्रीन से टेक्स्ट पढ़ें' बटन दबाएँ - स्क्रीन का टेक्स्ट तुरंत अनुवाद हो जाएगा।",
                        "6. अनुवाद को आवाज़ में सुनने के लिए 'सुनें' या दोस्तों को भेजने के लिए 'कॉपी' दबाएँ।"
                    )
                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun openOverlayPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
