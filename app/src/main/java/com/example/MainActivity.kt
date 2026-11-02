package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.ChatTranslatorScreen
import com.example.ui.screens.FloatingTranslatorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TouchSensorScreen
import com.example.ui.screens.VoiceRecordingScreen
import com.example.ui.screens.VoiceSelectionScreen
import com.example.ui.theme.SmartTranslatorTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.settingsRepo.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SmartTranslatorTheme(darkTheme = isDark) {
                val currentScreen by viewModel.currentScreen.collectAsState()

                BackHandler(enabled = currentScreen != Screen.HOME) {
                    viewModel.navigateBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.HOME -> HomeScreen(viewModel = viewModel)
                        Screen.VOICE_RECORDING -> VoiceRecordingScreen(viewModel = viewModel)
                        Screen.CHAT_TRANSLATOR -> ChatTranslatorScreen(viewModel = viewModel)
                        Screen.FLOATING_TRANSLATOR -> FloatingTranslatorScreen(viewModel = viewModel)
                        Screen.LANGUAGE_SELECTION -> LanguageSelectionScreen(viewModel = viewModel)
                        Screen.VOICE_SELECTION -> VoiceSelectionScreen(viewModel = viewModel)
                        Screen.TOUCH_SENSOR -> TouchSensorScreen(viewModel = viewModel)
                        Screen.TRANSLATION_HISTORY -> HistoryScreen(viewModel = viewModel)
                        Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
