package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BossSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val modelName: String = "gpt-4o-mini",
    val honorific: String = "Aap", // "Aap", "Boss", "Sahab", "Jaan", "Dost"
    val language: String = "Urdu", // "Urdu", "Roman Urdu", "English", "Hindi"
    val ttsEnabled: Boolean = true,
    val floatingBubbleEnabled: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val autoExecuteTools: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zoya_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<BossSettings> = _settings.asStateFlow()

    private fun loadSettings(): BossSettings {
        return BossSettings(
            apiKey = prefs.getString("api_key", "") ?: "",
            baseUrl = prefs.getString("base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1",
            modelName = prefs.getString("model_name", "gpt-4o-mini") ?: "gpt-4o-mini",
            honorific = prefs.getString("honorific", "Aap") ?: "Aap",
            language = prefs.getString("language", "Urdu") ?: "Urdu",
            ttsEnabled = prefs.getBoolean("tts_enabled", true),
            floatingBubbleEnabled = prefs.getBoolean("floating_bubble_enabled", false),
            wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false),
            autoExecuteTools = prefs.getBoolean("auto_execute_tools", true)
        )
    }

    fun updateApiKey(apiKey: String) {
        prefs.edit().putString("api_key", apiKey).apply()
        _settings.value = _settings.value.copy(apiKey = apiKey)
    }

    fun updateBaseUrl(baseUrl: String) {
        prefs.edit().putString("base_url", baseUrl).apply()
        _settings.value = _settings.value.copy(baseUrl = baseUrl)
    }

    fun updateModelName(modelName: String) {
        prefs.edit().putString("model_name", modelName).apply()
        _settings.value = _settings.value.copy(modelName = modelName)
    }

    fun updateHonorific(honorific: String) {
        prefs.edit().putString("honorific", honorific).apply()
        _settings.value = _settings.value.copy(honorific = honorific)
    }

    fun updateLanguage(language: String) {
        prefs.edit().putString("language", language).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun updateTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("tts_enabled", enabled).apply()
        _settings.value = _settings.value.copy(ttsEnabled = enabled)
    }

    fun updateFloatingBubbleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("floating_bubble_enabled", enabled).apply()
        _settings.value = _settings.value.copy(floatingBubbleEnabled = enabled)
    }

    fun updateWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word_enabled", enabled).apply()
        _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
    }

    fun updateAutoExecuteTools(enabled: Boolean) {
        prefs.edit().putBoolean("auto_execute_tools", enabled).apply()
        _settings.value = _settings.value.copy(autoExecuteTools = enabled)
    }
}

