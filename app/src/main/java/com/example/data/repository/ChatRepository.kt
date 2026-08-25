package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessageEntity> {
        return chatDao.getRecentMessages(limit)
    }

    suspend fun insertMessage(message: ChatMessageEntity): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) {
        chatDao.updateMessage(message)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun deleteMessage(id: Long) {
        chatDao.deleteMessage(id)
    }
}
