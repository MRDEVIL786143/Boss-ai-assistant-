package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BossMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM boss_memory ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<BossMemoryEntity>>

    @Query("SELECT * FROM boss_memory")
    suspend fun getMemoriesList(): List<BossMemoryEntity>

    @Query("SELECT * FROM boss_memory WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): BossMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: BossMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: BossMemoryEntity)

    @Query("DELETE FROM boss_memory WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM boss_memory")
    suspend fun clearMemories()
}
