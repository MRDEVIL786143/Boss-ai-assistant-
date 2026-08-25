package com.example.data.repository

import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.SnippetDao
import com.example.data.local.entity.BossMemoryEntity
import com.example.data.local.entity.SavedSnippetEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val snippetDao: SnippetDao
) {
    val allMemories: Flow<List<BossMemoryEntity>> = memoryDao.getAllMemories()
    val allSnippets: Flow<List<SavedSnippetEntity>> = snippetDao.getAllSnippets()

    suspend fun getMemoriesList(): List<BossMemoryEntity> = memoryDao.getMemoriesList()

    suspend fun saveMemory(key: String, value: String, category: String = "general"): Long {
        val existing = memoryDao.getMemoryByKey(key)
        return if (existing != null) {
            val updated = existing.copy(value = value, category = category, timestamp = System.currentTimeMillis())
            memoryDao.updateMemory(updated)
            existing.id
        } else {
            memoryDao.insertMemory(BossMemoryEntity(key = key, value = value, category = category))
        }
    }

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemory(id)
    suspend fun clearMemories() = memoryDao.clearMemories()

    suspend fun saveSnippet(title: String, language: String, code: String, description: String = ""): Long {
        return snippetDao.insertSnippet(
            SavedSnippetEntity(title = title, language = language, code = code, description = description)
        )
    }

    suspend fun deleteSnippet(id: Long) = snippetDao.deleteSnippet(id)
}
