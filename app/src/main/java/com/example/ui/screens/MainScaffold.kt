package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BossCyanPrimary
import com.example.ui.theme.BossDarkBg
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.CodingViewModel
import com.example.ui.viewmodel.MemoryViewModel
import com.example.ui.viewmodel.PhoneControlViewModel
import com.example.ui.viewmodel.SettingsViewModel

enum class BossNavTab(val title: String, val icon: ImageVector, val tag: String) {
    CHAT("Chat", Icons.Default.Chat, "nav_chat"),
    PHONE("Phone", Icons.Default.PhoneAndroid, "nav_phone"),
    CODING("Code", Icons.Default.Terminal, "nav_code"),
    MEMORY("Memory", Icons.Default.Psychology, "nav_memory"),
    SETTINGS("Settings", Icons.Default.Tune, "nav_settings")
}

@Composable
fun MainScaffold(
    chatViewModel: ChatViewModel = viewModel(),
    phoneControlViewModel: PhoneControlViewModel = viewModel(),
    codingViewModel: CodingViewModel = viewModel(),
    memoryViewModel: MemoryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(BossNavTab.CHAT) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0C121F),
                contentColor = Color.White,
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(1.dp, Color(0xFF1E2B40), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                BossNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) BossCyanPrimary else Color(0xFF8E9EB5)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                color = if (isSelected) BossCyanPrimary else Color(0xFF8E9EB5)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = BossCyanPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        },
        containerColor = BossDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                BossNavTab.CHAT -> ChatScreen(viewModel = chatViewModel)
                BossNavTab.PHONE -> PhoneControlScreen(viewModel = phoneControlViewModel)
                BossNavTab.CODING -> CodingStudioScreen(viewModel = codingViewModel)
                BossNavTab.MEMORY -> MemoryScreen(viewModel = memoryViewModel)
                BossNavTab.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
