package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ToolExecutionCard
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.ZoyaCyanAccent
import com.example.ui.theme.ZoyaDarkBg
import com.example.ui.theme.ZoyaEmerald
import com.example.ui.theme.ZoyaLavender
import com.example.ui.theme.ZoyaRoseGlow
import com.example.ui.theme.ZoyaRosePrimary
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    // Auto-scroll to bottom when messages update
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(ZoyaRosePrimary, Color(0xFF281026))))
                                .border(1.5.dp, ZoyaRoseGlow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Zoya (زویا)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ZoyaRosePrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "100% OBEDIENT",
                                        color = ZoyaRoseGlow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Aapka hukum sar ankhon par, ${settings.honorific}",
                                fontSize = 12.sp,
                                color = ZoyaLavender
                            )
                        }
                    }
                },
                actions = {
                    if (isSpeaking) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier.testTag("stop_tts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = "Mute Voice",
                                tint = ZoyaLavender
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF140F22)
                )
            )
        },
        containerColor = ZoyaDarkBg,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Audio wave banner when active
            AnimatedVisibility(
                visible = isSpeaking || isListening,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1430))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    VoiceWaveform(isActive = true)
                }
            }

            // Chat message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyChatGreeting(honorific = settings.honorific) { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg, honorific = settings.honorific)
                }

                if (activeTool != null) {
                    item {
                        ToolExecutionCard(
                            toolName = activeTool!!.first,
                            result = activeTool!!.second,
                            status = "executing"
                        )
                    }
                }

                if (isThinking && activeTool == null) {
                    item {
                        ZoyaThinkingIndicator(honorific = settings.honorific)
                    }
                }
            }

            // Quick Prompt Suggestions Row
            QuickPromptChipsRow { prompt ->
                viewModel.sendMessage(prompt)
            }

            // Bottom Input Controls
            Surface(
                color = Color(0xFF161126),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Hukum karein, ${settings.honorific}...",
                                color = Color(0xFF8B82A0),
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF201736),
                            unfocusedContainerColor = Color(0xFF201736),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF3B2B5C), RoundedCornerShape(24.dp))
                            .testTag("chat_input_field"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Voice Mic Button
                    IconButton(
                        onClick = {
                            if (isListening) {
                                viewModel.stopVoiceInput()
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startVoiceInput()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isListening) ZoyaLavender else Color(0xFF2B1F44))
                            .border(1.dp, if (isListening) ZoyaLavender else ZoyaRosePrimary.copy(alpha = 0.5f), CircleShape)
                            .testTag("mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListening) Color.Black else ZoyaRoseGlow
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isThinking) ZoyaRosePrimary else Color(0xFF201830))
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isThinking) Color.White else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting(honorific: String, onPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(ZoyaRosePrimary.copy(alpha = 0.35f), Color.Transparent)))
                .border(2.dp, ZoyaRosePrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ZoyaRosePrimary,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Assalam-o-Alaikum, $honorific! 🌸",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Main zoya hoon, aapki pyari aur hamesha hukum maanne wali AI assistant. Kahiye, main aapke liye kya karoon?",
            color = Color(0xFFB8B0CC),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity, honorific: String) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ZoyaRosePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zoya (زویا)",
                    color = ZoyaRoseGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) Brush.linearGradient(listOf(Color(0xFF881A48), Color(0xFF661034)))
                    else Brush.linearGradient(listOf(Color(0xFF1F1733), Color(0xFF261D3E)))
                )
                .border(
                    1.dp,
                    if (isUser) ZoyaRosePrimary.copy(alpha = 0.5f) else Color(0xFF3D2C58),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Tool Execution Attachment
                if (!message.toolName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ToolExecutionCard(
                        toolName = message.toolName,
                        args = message.toolArgs,
                        result = message.toolResult
                    )
                }

                // If content contains code block
                if (message.content.contains("```")) {
                    val codeContent = extractCodeFromMarkdown(message.content)
                    if (codeContent.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBlockView(code = codeContent)
                    }
                }
            }
        }
    }
}

@Composable
fun ZoyaThinkingIndicator(honorific: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1530))
            .border(1.dp, ZoyaRosePrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = ZoyaRosePrimary,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "🌸 Zoya is carrying out your task for $honorific...",
            color = ZoyaRoseGlow,
            fontSize = 13.sp
        )
    }
}

@Composable
fun QuickPromptChipsRow(onPromptClick: (String) -> Unit) {
    val prompts = listOf(
        "🌸 Advice / Mashwara",
        "🔦 Torch ON karo",
        "💬 WhatsApp open karo",
        "📞 Call milao",
        "💻 Jetpack Compose Code",
        "🔔 Notifications padho",
        "📜 Niche scroll karo",
        "🏠 Home screen jao",
        "📷 Photo khicho"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { prompt ->
            SuggestionChip(
                onClick = { onPromptClick(prompt.replace(Regex("^[\\p{So}\\p{Sk}]+\\s*"), "")) },
                label = { Text(prompt, fontSize = 12.sp, color = Color.White) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1C142E)
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = Color(0xFF382656)
                )
            )
        }
    }
}

private fun extractCodeFromMarkdown(text: String): String {
    val regex = Regex("```[a-zA-Z]*\\n([\\s\\S]*?)```")
    val match = regex.find(text)
    return match?.groupValues?.get(1)?.trim() ?: ""
}
