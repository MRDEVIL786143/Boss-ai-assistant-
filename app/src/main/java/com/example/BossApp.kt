package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.SettingsRepository
import com.example.engine.OpenAIAssistantClient
import com.example.engine.VoiceAssistantManager

class BossApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var memoryRepository: MemoryRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var openAIClient: OpenAIAssistantClient
        private set

    lateinit var voiceManager: VoiceAssistantManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        chatRepository = ChatRepository(database.chatDao())
        memoryRepository = MemoryRepository(database.memoryDao(), database.snippetDao())
        settingsRepository = SettingsRepository(this)
        openAIClient = OpenAIAssistantClient(this, memoryRepository)
        voiceManager = VoiceAssistantManager.getInstance(this)
    }

    companion object {
        lateinit var instance: BossApp
            private set
    }
}
