package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.BossCyanPrimary
import com.example.ui.theme.BossDarkBg
import com.example.ui.theme.BossEmerald
import com.example.ui.theme.BossGoldSecondary
import com.example.ui.viewmodel.CodingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodingStudioScreen(
    viewModel: CodingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedSnippets by viewModel.savedSnippets.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()

    var showAddSnippetDialog by remember { mutableStateOf(false) }
    var snippetTitle by remember { mutableStateOf("") }
    var snippetLang by remember { mutableStateOf("kotlin") }
    var snippetCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = BossCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Boss Coding Studio",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSnippetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Save Snippet",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // Project Templates Row
            item {
                SectionHeader("READY-TO-BUILD PROJECT TEMPLATES")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.sampleProjects.forEach { proj ->
                        val isSelected = selectedTemplate?.title == proj.title
                        Card(
                            onClick = { viewModel.selectTemplate(proj) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1B2A40) else Color(0xFF131A28)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(
                                1.dp,
                                if (isSelected) BossCyanPrimary else Color(0xFF233247),
                                RoundedCornerShape(12.dp)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = proj.title,
                                    color = if (isSelected) BossCyanPrimary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = proj.language.uppercase(),
                                    color = BossGoldSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Selected Template Viewer
            if (selectedTemplate != null) {
                item {
                    val proj = selectedTemplate!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF223049), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = proj.filename,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = proj.description,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.saveSnippet(
                                            title = proj.title,
                                            language = proj.language,
                                            code = proj.codeContent,
                                            description = proj.description
                                        )
                                        Toast.makeText(context, "Saved into Boss Snippets!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2C42)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = BossCyanPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontSize = 11.sp, color = BossCyanPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            CodeBlockView(
                                code = proj.codeContent,
                                language = proj.language
                            )
                        }
                    }
                }
            }

            // Saved Snippets Section
            item {
                SectionHeader("SAVED BOSS SNIPPETS (${savedSnippets.size})")
            }

            if (savedSnippets.isEmpty()) {
                item {
                    Text(
                        text = "No saved snippets yet, Boss. Save any generated code here for one-tap retrieval.",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                items(savedSnippets, key = { it.id }) { snippet ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121929)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF223049), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = snippet.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.deleteSnippet(snippet.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            CodeBlockView(code = snippet.code, language = snippet.language)
                        }
                    }
                }
            }
        }
    }

    if (showAddSnippetDialog) {
        AlertDialog(
            onDismissRequest = { showAddSnippetDialog = false },
            title = { Text("Add Custom Snippet", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = snippetTitle,
                        onValueChange = { snippetTitle = it },
                        placeholder = { Text("Snippet Title...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF162032), unfocusedContainerColor = Color(0xFF162032), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    TextField(
                        value = snippetLang,
                        onValueChange = { snippetLang = it },
                        placeholder = { Text("Language (kotlin, python, dart)...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF162032), unfocusedContainerColor = Color(0xFF162032), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    TextField(
                        value = snippetCode,
                        onValueChange = { snippetCode = it },
                        placeholder = { Text("Paste code snippet here...") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF162032), unfocusedContainerColor = Color(0xFF162032), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (snippetTitle.isNotBlank() && snippetCode.isNotBlank()) {
                            viewModel.saveSnippet(snippetTitle, snippetLang, snippetCode)
                            snippetTitle = ""
                            snippetCode = ""
                            showAddSnippetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BossCyanPrimary, contentColor = Color.Black)
                ) { Text("Save Snippet") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSnippetDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF111827)
        )
    }
}
