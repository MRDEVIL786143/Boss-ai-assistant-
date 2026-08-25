package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavedSnippetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM saved_snippets ORDER BY timestamp DESC")
    fun getAllSnippets(): Flow<List<SavedSnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SavedSnippetEntity): Long

    @Update
    suspend fun updateSnippet(snippet: SavedSnippetEntity)

    @Query("DELETE FROM saved_snippets WHERE id = :id")
    suspend fun deleteSnippet(id: Long)
}
