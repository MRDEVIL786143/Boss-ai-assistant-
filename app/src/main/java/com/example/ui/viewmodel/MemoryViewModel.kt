package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BossApp
import com.example.data.local.entity.BossMemoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BossApp
    private val memoryRepo = app.memoryRepository

    val memories: StateFlow<List<BossMemoryEntity>> = memoryRepo.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMemory(key: String, value: String, category: String = "general") {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryRepo.saveMemory(key.trim(), value.trim(), category)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepo.clearMemories()
        }
    }
}
