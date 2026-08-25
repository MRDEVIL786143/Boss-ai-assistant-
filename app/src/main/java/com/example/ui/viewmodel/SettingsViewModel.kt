package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.BossApp
import com.example.data.repository.BossSettings
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BossApp
    private val settingsRepo = app.settingsRepository

    val settings: StateFlow<BossSettings> = settingsRepo.settings

    fun setApiKey(apiKey: String) = settingsRepo.updateApiKey(apiKey)
    fun setBaseUrl(baseUrl: String) = settingsRepo.updateBaseUrl(baseUrl)
    fun setModelName(modelName: String) = settingsRepo.updateModelName(modelName)
    fun setHonorific(honorific: String) = settingsRepo.updateHonorific(honorific)
    fun setLanguage(language: String) = settingsRepo.updateLanguage(language)
    fun setTtsEnabled(enabled: Boolean) = settingsRepo.updateTtsEnabled(enabled)
    fun setFloatingBubbleEnabled(enabled: Boolean) = settingsRepo.updateFloatingBubbleEnabled(enabled)
    fun setWakeWordEnabled(enabled: Boolean) = settingsRepo.updateWakeWordEnabled(enabled)
    fun setAutoExecuteTools(enabled: Boolean) = settingsRepo.updateAutoExecuteTools(enabled)
}
