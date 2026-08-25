package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.BossAccessibilityService
import com.example.service.BossFloatingOverlayService
import com.example.service.BossNotificationService
import com.example.ui.theme.BossCyanPrimary
import com.example.ui.theme.BossDarkBg
import com.example.ui.theme.BossEmerald
import com.example.ui.theme.BossGoldSecondary
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var apiKeyInput by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var baseUrlInput by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var modelNameInput by remember(settings.modelName) { mutableStateOf(settings.modelName) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val grantedCount = grants.values.count { it }
        Toast.makeText(context, "$grantedCount permissions updated, Boss!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = BossCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings & AI Architecture",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // AI Model & API Configuration Card
            item {
                SectionHeader("AI BRAIN & API CONFIGURATION")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF223049), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "OpenAI-Compatible API Key",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                viewModel.setApiKey(it)
                            },
                            placeholder = { Text("sk-...", color = Color(0xFF6B7280)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF162032),
                                unfocusedContainerColor = Color(0xFF162032),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input")
                        )
                        Text(
                            text = "Leave empty to use the built-in fast offline action engine without internet.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "API Base URL (OpenAI / Groq / Ollama / DeepSeek)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextField(
                            value = baseUrlInput,
                            onValueChange = {
                                baseUrlInput = it
                                viewModel.setBaseUrl(it)
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF162032),
                                unfocusedContainerColor = Color(0xFF162032),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Preset Base URLs
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PresetChip("OpenAI") {
                                baseUrlInput = "https://api.openai.com/v1"
                                modelNameInput = "gpt-4o-mini"
                                viewModel.setBaseUrl(baseUrlInput)
                                viewModel.setModelName(modelNameInput)
                            }
                            PresetChip("Groq") {
                                baseUrlInput = "https://api.groq.com/openai/v1"
                                modelNameInput = "llama-3.3-70b-versatile"
                                viewModel.setBaseUrl(baseUrlInput)
                                viewModel.setModelName(modelNameInput)
                            }
                            PresetChip("DeepSeek") {
                                baseUrlInput = "https://api.deepseek.com/v1"
                                modelNameInput = "deepseek-chat"
                                viewModel.setBaseUrl(baseUrlInput)
                                viewModel.setModelName(modelNameInput)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Model Name",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextField(
                            value = modelNameInput,
                            onValueChange = {
                                modelNameInput = it
                                viewModel.setModelName(it)
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF162032),
                                unfocusedContainerColor = Color(0xFF162032),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Honorific & Persona Customization
            item {
                SectionHeader("HONORIFIC & LANGUAGE")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF223049), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Address User As:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HonorificChoiceChip(
                                label = "Boss",
                                isSelected = settings.honorific == "Boss",
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setHonorific("Boss")
                            }
                            HonorificChoiceChip(
                                label = "Sahab",
                                isSelected = settings.honorific == "Sahab",
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.setHonorific("Sahab")
                            }
                        }

                        HorizontalDivider(color = Color(0xFF202C40))

                        Text(
                            text = "Language Response Mode:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LanguageChoiceChip("English", settings.language == "English") { viewModel.setLanguage("English") }
                            LanguageChoiceChip("Hindi", settings.language == "Hindi") { viewModel.setLanguage("Hindi") }
                            LanguageChoiceChip("Urdu", settings.language == "Urdu") { viewModel.setLanguage("Urdu") }
                            LanguageChoiceChip("Hinglish", settings.language == "Hinglish") { viewModel.setLanguage("Hinglish") }
                        }

                        HorizontalDivider(color = Color(0xFF202C40))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Voice Text-To-Speech (TTS)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Read AI replies aloud automatically", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                            }
                            Switch(
                                checked = settings.ttsEnabled,
                                onCheckedChange = { viewModel.setTtsEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = BossCyanPrimary)
                            )
                        }
                    }
                }
            }

            // System Permissions Hub
            item {
                SectionHeader("PHONE PERMISSIONS & SERVICE INTEGRATIONS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A2A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF223049), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PermissionTile(
                            icon = Icons.Default.Accessibility,
                            title = "Accessibility Service",
                            desc = "Required to tap buttons, scroll screens, and type text",
                            isGranted = BossAccessibilityService.isServiceRunning,
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )

                        PermissionTile(
                            icon = Icons.Default.Layers,
                            title = "Draw Over Other Apps (Overlay)",
                            desc = "Required for Floating Assistant Bubble",
                            isGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context),
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )

                        PermissionTile(
                            icon = Icons.Default.NotificationsActive,
                            title = "Notification Listener",
                            desc = "Allows AI to read and summarize incoming notifications",
                            isGranted = BossNotificationService.isServiceRunning,
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )

                        PermissionTile(
                            icon = Icons.Default.Mic,
                            title = "Microphone & Telephony Permissions",
                            desc = "Voice input, Phone calling, and SMS actions",
                            isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                            onClick = {
                                permissionsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.RECORD_AUDIO,
                                        Manifest.permission.CALL_PHONE,
                                        Manifest.permission.SEND_SMS,
                                        Manifest.permission.CAMERA
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // APK Installation & Setup Guide
            item {
                SectionHeader("APK BUILD & INSTALLATION GUIDE")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101726)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BossCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = BossCyanPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("How to Install & Grant Full Power", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "1. Export project or build APK directly from the top menu.\n" +
                                   "2. Install the APK on your Android device.\n" +
                                   "3. Go to Android Settings > Accessibility > Downloaded Apps > Enable 'MyBossAI'.\n" +
                                   "4. Grant 'Display over other apps' to summon Boss AI over any app.\n" +
                                   "5. Enter your OpenAI or Groq API Key above, or simply use offline speech commands!",
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, color = BossCyanPrimary) },
        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF192438))
    )
}

@Composable
fun HonorificChoiceChip(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) BossCyanPrimary else Color(0xFF162032),
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(44.dp)
            .border(1.dp, if (isSelected) BossCyanPrimary else Color(0xFF283852), RoundedCornerShape(12.dp))
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun LanguageChoiceChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BossCyanPrimary,
            selectedLabelColor = Color.Black,
            containerColor = Color(0xFF162032),
            labelColor = Color.White
        )
    )
}

@Composable
fun PermissionTile(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF151E2E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF223046), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = BossCyanPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = desc, color = Color(0xFF9CA3AF), fontSize = 10.sp)
            }
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF1C2C40) else BossCyanPrimary,
                contentColor = if (isGranted) BossEmerald else Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(if (isGranted) "Granted" else "Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
