package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BossApp
import com.example.data.local.entity.SavedSnippetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CodeProjectTemplate(
    val title: String,
    val language: String,
    val description: String,
    val filename: String,
    val codeContent: String
)

class CodingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BossApp
    private val memoryRepo = app.memoryRepository

    val savedSnippets: StateFlow<List<SavedSnippetEntity>> = memoryRepo.allSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTemplate = MutableStateFlow<CodeProjectTemplate?>(null)
    val selectedTemplate: StateFlow<CodeProjectTemplate?> = _selectedTemplate.asStateFlow()

    val sampleProjects = listOf(
        CodeProjectTemplate(
            title = "Android Jetpack Compose UI",
            language = "kotlin",
            filename = "BossScreen.kt",
            description = "High-performance Material 3 modern screen with glowing neon action cards",
            codeContent = """
package com.boss.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BossDashboardScreen(onActionClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Boss Command Deck",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXECUTE PROTOCOL", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
""".trimIndent()
        ),
        CodeProjectTemplate(
            title = "Python Automation & Phone Webhook",
            language = "python",
            filename = "boss_automation.py",
            description = "Async daemon with API endpoint triggering automated system actions",
            codeContent = """
import asyncio
import httpx
from datetime import datetime

async def execute_boss_mission(command: str):
    print(f"[{datetime.now()}] Obedient AI Executing: {command}")
    await asyncio.sleep(1.0)
    return {"status": "SUCCESS", "message": f"Command '{command}' executed perfectly for Boss."}

async def main():
    print("--- MyBossAI Python Core Initialized ---")
    res = await execute_boss_mission("Optimize Device Resources & Clean Cache")
    print("Result:", res)

if __name__ == "__main__":
    asyncio.run(main())
""".trimIndent()
        ),
        CodeProjectTemplate(
            title = "Flutter Cyberpunk Glassmorphic Card",
            language = "dart",
            filename = "cyber_card.dart",
            description = "Custom paint glassmorphic card widget with glowing gradient border",
            codeContent = """
import 'package:flutter/material.dart';

class CyberBossCard extends StatelessWidget {
  final String title;
  final VoidCallback onTap;

  const CyberBossCard({Key? key, required this.title, required this.onTap}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        decoration: BoxDecoration(
          color: const Color(0xFF131B2E),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: const Color(0xFF00E5FF), width: 1.5),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF00E5FF).withOpacity(0.2),
              blurRadius: 16,
              spreadRadius: 2,
            )
          ],
        ),
        child: Row(
          children: [
            const Icon(Icons.security, color: Color(0xFF00E5FF), size: 28),
            const SizedBox(width: 16),
            Text(
              title,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
""".trimIndent()
        ),
        CodeProjectTemplate(
            title = "Kotlin Spring Boot REST Controller",
            language = "kotlin",
            filename = "BossApiController.kt",
            description = "Reactive REST controller with coroutine endpoints for remote agent dispatch",
            codeContent = """
package com.boss.server.controller

import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class BossCommand(val action: String, val payload: Map<String, String>)
data class BossResponse(val status: String, val result: String)

@RestController
@RequestMapping("/api/v1/boss")
class BossApiController {

    @PostMapping("/execute")
    suspend fun executeCommand(@RequestBody command: BossCommand): BossResponse {
        return BossResponse(
            status = "EXECUTED",
            result = "Command '${'$'}{command.action}' completed with 100% obedience, Boss."
        )
    }
}
""".trimIndent()
        )
    )

    init {
        _selectedTemplate.value = sampleProjects.first()
    }

    fun selectTemplate(template: CodeProjectTemplate) {
        _selectedTemplate.value = template
    }

    fun saveSnippet(title: String, language: String, code: String, description: String = "") {
        viewModelScope.launch {
            memoryRepo.saveSnippet(title, language, code, description)
        }
    }

    fun deleteSnippet(id: Long) {
        viewModelScope.launch {
            memoryRepo.deleteSnippet(id)
        }
    }
}
