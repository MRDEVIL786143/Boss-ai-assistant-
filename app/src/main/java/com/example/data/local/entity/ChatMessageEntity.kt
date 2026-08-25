package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolResult: String? = null,
    val status: String = "done", // "thinking", "executing", "done", "error"
    val timestamp: Long = System.currentTimeMillis()
)
