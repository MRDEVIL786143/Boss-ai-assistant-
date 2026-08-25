package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BossApp
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.repository.BossSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BossApp
    private val chatRepo = app.chatRepository
    private val settingsRepo = app.settingsRepository
    private val openAIClient = app.openAIClient
    private val voiceManager = app.voiceManager

    val messages: StateFlow<List<ChatMessageEntity>> = chatRepo.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<BossSettings> = settingsRepo.settings

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _activeTool = MutableStateFlow<Pair<String, String>?>(null) // toolName, status
    val activeTool: StateFlow<Pair<String, String>?> = _activeTool.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val speechRms: StateFlow<Float> = voiceManager.speechRms

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isThinking.value) return

        viewModelScope.launch {
            // Save User message
            chatRepo.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = trimmed
                )
            )

            _isThinking.value = true
            _activeTool.value = null

            val currentSettings = settings.value
            val history = messages.value.takeLast(10).map { it.role to it.content }

            val response = openAIClient.sendMessage(
                userMessage = trimmed,
                settings = currentSettings,
                conversationHistory = history,
                onToolExecuting = { toolName, args ->
                    _activeTool.value = toolName to "Executing $toolName..."
                },
                onToolFinished = { toolName, result ->
                    _activeTool.value = toolName to "Completed: $result"
                }
            )

            // Save Assistant reply
            chatRepo.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = response.replyText,
                    toolName = response.toolName,
                    toolArgs = response.toolArgs,
                    toolResult = response.toolResult,
                    status = "done"
                )
            )

            _isThinking.value = false
            _activeTool.value = null

            // TTS Speech feedback if enabled
            if (currentSettings.ttsEnabled) {
                voiceManager.speak(response.replyText, currentSettings.language)
            }
        }
    }

    fun startVoiceInput() {
        voiceManager.startListening(
            onResult = { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    sendMessage(recognizedText)
                }
            }
        )
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepo.clearHistory()
        }
    }
}
