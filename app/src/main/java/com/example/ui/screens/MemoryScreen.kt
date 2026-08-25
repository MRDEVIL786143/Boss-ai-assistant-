package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BossCyanPrimary
import com.example.ui.theme.BossDarkBg
import com.example.ui.theme.BossEmerald
import com.example.ui.theme.BossGoldSecondary
import com.example.ui.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var memoryKey by remember { mutableStateOf("") }
    var memoryValue by remember { mutableStateOf("") }
    var memoryCategory by remember { mutableStateOf("preference") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = BossCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Boss Memory Vault",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Memory",
                            tint = BossCyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1523))
            )
        },
        containerColor = BossDarkBg,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // Memory Overview Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121B2D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF263750), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BossCyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = BossCyanPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Persistent AI Knowledge Base",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${memories.size} facts remembered about Boss",
                                    color = BossGoldSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Every detail saved here is permanently retained in local storage and automatically loaded into the AI engine so MyBossAI personalizes every action to your lifestyle.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Quick Predefined Memory Fillers (if empty)
            if (memories.isEmpty()) {
                item {
                    SectionHeader("SUGGESTED INITIAL MEMORIES")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestedMemoryTile("Name / Title", "Boss / Sahab") {
                            viewModel.addMemory("preferred_name", "Boss", "personal")
                        }
                        SuggestedMemoryTile("Default Theme", "Obsidian Dark Cyberpunk") {
                            viewModel.addMemory("favorite_theme", "Cyberpunk Obsidian", "preference")
                        }
                        SuggestedMemoryTile("Favorite Programming Language", "Kotlin & Python") {
                            viewModel.addMemory("favorite_language", "Kotlin & Python", "preference")
                        }
                    }
                }
            }

            item {
                SectionHeader("STORED FACTS & PREFERENCES")
            }

            items(memories, key = { it.id }) { mem ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A28)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF223049), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mem.key.uppercase(),
                                    color = BossCyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E2C44))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = mem.category,
                                        color = BossGoldSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mem.value,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteMemory(mem.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Memory",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Remember Fact About Boss", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = memoryKey,
                        onValueChange = { memoryKey = it },
                        placeholder = { Text("Fact Key (e.g. boss_car, home_city)...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF162032), unfocusedContainerColor = Color(0xFF162032), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    TextField(
                        value = memoryValue,
                        onValueChange = { memoryValue = it },
                        placeholder = { Text("Fact Value (e.g. Mustang GT, Tokyo)...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF162032), unfocusedContainerColor = Color(0xFF162032), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memoryKey.isNotBlank() && memoryValue.isNotBlank()) {
                            viewModel.addMemory(memoryKey, memoryValue, memoryCategory)
                            memoryKey = ""
                            memoryValue = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossCyanPrimary, contentColor = Color.Black)
                ) { Text("Save Memory") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF111827)
        )
    }
}

@Composable
fun SuggestedMemoryTile(title: String, example: String, onAdd: () -> Unit) {
    Card(
        onClick = onAdd,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF223048), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = "e.g. $example", color = Color(0xFF9CA3AF), fontSize = 11.sp)
            }
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BossCyanPrimary)
        }
    }
}
