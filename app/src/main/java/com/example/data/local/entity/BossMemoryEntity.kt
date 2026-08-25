package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boss_memory")
data class BossMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general", // "preference", "personal", "command", "contact", "system"
    val timestamp: Long = System.currentTimeMillis()
)
