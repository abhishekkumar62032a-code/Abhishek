package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_translator_settings", Context.MODE_PRIVATE)

    private val _sourceLang = MutableStateFlow(prefs.getString("source_lang", "auto") ?: "auto")
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow(prefs.getString("target_lang", "hi") ?: "hi")
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _voiceId = MutableStateFlow(prefs.getString("voice_id", "female_1") ?: "female_1")
    val voiceId: StateFlow<String> = _voiceId.asStateFlow()

    private val _favoriteVoiceId = MutableStateFlow(prefs.getString("fav_voice_id", "female_1") ?: "female_1")
    val favoriteVoiceId: StateFlow<String> = _favoriteVoiceId.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(prefs.getFloat("voice_speed", 1.0f))
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val _voicePitch = MutableStateFlow(prefs.getFloat("voice_pitch", 1.0f))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _touchSensorEnabled = MutableStateFlow(prefs.getBoolean("touch_sensor_enabled", true))
    val touchSensorEnabled: StateFlow<Boolean> = _touchSensorEnabled.asStateFlow()

    private val _touchSensorMode = MutableStateFlow(prefs.getString("touch_sensor_mode", "WORD") ?: "WORD")
    val touchSensorMode: StateFlow<String> = _touchSensorMode.asStateFlow()

    private val _floatingSide = MutableStateFlow(prefs.getString("floating_side", "RIGHT") ?: "RIGHT")
    val floatingSide: StateFlow<String> = _floatingSide.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setSourceLang(code: String) {
        prefs.edit().putString("source_lang", code).apply()
        _sourceLang.value = code
    }

    fun setTargetLang(code: String) {
        prefs.edit().putString("target_lang", code).apply()
        _targetLang.value = code
    }

    fun setVoiceId(id: String) {
        prefs.edit().putString("voice_id", id).apply()
        _voiceId.value = id
    }

    fun setFavoriteVoiceId(id: String) {
        prefs.edit().putString("fav_voice_id", id).apply()
        _favoriteVoiceId.value = id
    }

    fun setVoiceSpeed(speed: Float) {
        prefs.edit().putFloat("voice_speed", speed).apply()
        _voiceSpeed.value = speed
    }

    fun setVoicePitch(pitch: Float) {
        prefs.edit().putFloat("voice_pitch", pitch).apply()
        _voicePitch.value = pitch
    }

    fun setTouchSensorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("touch_sensor_enabled", enabled).apply()
        _touchSensorEnabled.value = enabled
    }

    fun setTouchSensorMode(mode: String) {
        prefs.edit().putString("touch_sensor_mode", mode).apply()
        _touchSensorMode.value = mode
    }

    fun setFloatingSide(side: String) {
        prefs.edit().putString("floating_side", side).apply()
        _floatingSide.value = side
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }
}
