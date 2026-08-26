package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.PhoneControlViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneControlScreen(
    viewModel: PhoneControlViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lastResult by viewModel.lastActionResult.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var showCallDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }

    var targetNumber by remember { mutableStateOf("") }
    var targetMessage by remember { mutableStateOf("") }
    var targetAppName by remember { mutableStateOf("") }
    var volumeLevel by remember { mutableFloatStateOf(75f) }
    var isFlashlightOn by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = ZoyaRosePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone Command Deck",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF140D24))
            )
        },
        containerColor = ZoyaDarkBg,
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
            // Service Status Card
            item {
                ServiceStatusBanner(
                    isAccessibilityActive = viewModel.isAccessibilityEnabled,
                    isOverlayActive = viewModel.isFloatingBubbleActive,
                    onOpenAccessibilitySettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    onToggleFloatingBubble = { enable ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } else {
                            viewModel.toggleFloatingBubble(enable)
                        }
                    }
                )
            }

            // Action Feedback Notification
            if (lastResult != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF231435)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ZoyaRosePrimary, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ZoyaEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastResult ?: "",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Hardware Controls Section
            item {
                SectionHeader("HARDWARE QUICK CONTROLS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = ZoyaCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, ZoyaBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Flashlight & Camera Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ControlTile(
                                icon = Icons.Default.FlashlightOn,
                                title = "Flashlight",
                                subtitle = if (isFlashlightOn) "STATE: ON" else "STATE: OFF",
                                isActive = isFlashlightOn,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isFlashlightOn = !isFlashlightOn
                                    viewModel.toggleFlashlight(isFlashlightOn)
                                }
                            )
                            ControlTile(
                                icon = Icons.Default.CameraAlt,
                                title = "Rear Camera",
                                subtitle = "LAUNCH SHOOTER",
                                isActive = false,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.launchCamera(false) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ControlTile(
                                icon = Icons.Default.Face,
                                title = "Front Selfie",
                                subtitle = "FRONT CAMERA",
                                isActive = false,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.launchCamera(true) }
                            )
                            ControlTile(
                                icon = Icons.Default.Vibration,
                                title = "Haptic Pulse",
                                subtitle = "TEST VIBRATION",
                                isActive = false,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.executeGesture("vibrate") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Volume Slider
                        Text(
                            text = "Media Volume: ${volumeLevel.toInt()}%",
                            color = ZoyaRosePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = volumeLevel,
                            onValueChange = { volumeLevel = it },
                            onValueChangeFinished = {
                                viewModel.setVolume(volumeLevel.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = ZoyaRosePrimary,
                                activeTrackColor = ZoyaRosePrimary,
                                inactiveTrackColor = Color(0xFF332049)
                            )
                        )
                    }
                }
            }

            // Direct Communication & Apps
            item {
                SectionHeader("DIRECT ACTION LAUNCHERS")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionLauncherButton("📞 Call", ZoyaRosePrimary, Modifier.weight(1f)) {
                        showCallDialog = true
                    }
                    ActionLauncherButton("✉️ SMS", ZoyaRosePrimary, Modifier.weight(1f)) {
                        showSmsDialog = true
                    }
                    ActionLauncherButton("💬 WhatsApp", ZoyaEmerald, Modifier.weight(1f)) {
                        showWhatsAppDialog = true
                    }
                    ActionLauncherButton("📱 Open App", ZoyaGoldSecondary, Modifier.weight(1f)) {
                        showAppDialog = true
                    }
                }
            }

            // Screen Gestures & Accessibility Automation
            item {
                SectionHeader("SCREEN AUTOMATION GESTURES")
                Card(
                    colors = CardDefaults.cardColors(containerColor = ZoyaCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, ZoyaBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GestureChip("🏠 Home", Modifier.weight(1f)) { viewModel.executeGesture("home") }
                            GestureChip("◀ Back", Modifier.weight(1f)) { viewModel.executeGesture("back") }
                            GestureChip("🔲 Recents", Modifier.weight(1f)) { viewModel.executeGesture("recents") }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GestureChip("⬇ Scroll Down", Modifier.weight(1f)) { viewModel.executeGesture("scroll_down") }
                            GestureChip("⬆ Scroll Up", Modifier.weight(1f)) { viewModel.executeGesture("scroll_up") }
                            GestureChip("🔍 Read Screen", Modifier.weight(1f)) { viewModel.executeGesture("read_screen") }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GestureChip("🔔 Notifications", Modifier.weight(1f)) { viewModel.executeGesture("notifications") }
                            GestureChip("⚙️ Quick Settings", Modifier.weight(1f)) { viewModel.executeGesture("quick_settings") }
                            GestureChip("📶 Wi-Fi Page", Modifier.weight(1f)) { viewModel.openSettings("wifi") }
                        }
                    }
                }
            }

            // Notifications Feed
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("CAPTURED NOTIFICATIONS")
                    TextButton(onClick = { viewModel.refreshNotifications() }) {
                        Text("Refresh", color = ZoyaRosePrimary, fontSize = 12.sp)
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    Text(
                        text = "No captured notifications yet. Ensure Zoya Notification Listener is enabled in Android Settings.",
                        color = Color(0xFF8E82A6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                items(notifications.take(6)) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ZoyaCardBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, ZoyaBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = notif.appName,
                                    color = ZoyaGoldSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = notif.text,
                                color = Color(0xFFB5A7C9),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showCallDialog) {
        AlertDialog(
            onDismissRequest = { showCallDialog = false },
            title = { Text("Make Phone Call", color = Color.White) },
            text = {
                TextField(
                    value = targetNumber,
                    onValueChange = { targetNumber = it },
                    placeholder = { Text("Enter phone number...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF22153B),
                        unfocusedContainerColor = Color(0xFF22153B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetNumber.isNotBlank()) {
                            viewModel.makeCall(targetNumber)
                            showCallDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZoyaRosePrimary, contentColor = Color.White)
                ) { Text("Call Now") }
            },
            dismissButton = {
                TextButton(onClick = { showCallDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF191029)
        )
    }

    if (showSmsDialog) {
        AlertDialog(
            onDismissRequest = { showSmsDialog = false },
            title = { Text("Send SMS Message", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = targetNumber,
                        onValueChange = { targetNumber = it },
                        placeholder = { Text("Recipient number...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF22153B),
                            unfocusedContainerColor = Color(0xFF22153B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    TextField(
                        value = targetMessage,
                        onValueChange = { targetMessage = it },
                        placeholder = { Text("Message text...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF22153B),
                            unfocusedContainerColor = Color(0xFF22153B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetNumber.isNotBlank() && targetMessage.isNotBlank()) {
                            viewModel.sendSms(targetNumber, targetMessage)
                            showSmsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZoyaRosePrimary, contentColor = Color.White)
                ) { Text("Send SMS") }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF191029)
        )
    }

    if (showWhatsAppDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppDialog = false },
            title = { Text("Send WhatsApp Message", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = targetNumber,
                        onValueChange = { targetNumber = it },
                        placeholder = { Text("Phone number with country code...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF22153B),
                            unfocusedContainerColor = Color(0xFF22153B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    TextField(
                        value = targetMessage,
                        onValueChange = { targetMessage = it },
                        placeholder = { Text("Message text...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF22153B),
                            unfocusedContainerColor = Color(0xFF22153B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendWhatsApp(targetNumber, targetMessage)
                        showWhatsAppDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZoyaEmerald, contentColor = Color.Black)
                ) { Text("Send WhatsApp") }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF191029)
        )
    }

    if (showAppDialog) {
        AlertDialog(
            onDismissRequest = { showAppDialog = false },
            title = { Text("Launch Any App", color = Color.White) },
            text = {
                TextField(
                    value = targetAppName,
                    onValueChange = { targetAppName = it },
                    placeholder = { Text("e.g. YouTube, Chrome, Spotify, Maps...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF22153B),
                        unfocusedContainerColor = Color(0xFF22153B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetAppName.isNotBlank()) {
                            viewModel.openApp(targetAppName)
                            showAppDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZoyaGoldSecondary, contentColor = Color.Black)
                ) { Text("Launch") }
            },
            dismissButton = {
                TextButton(onClick = { showAppDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color(0xFF191029)
        )
    }
}

@Composable
fun ServiceStatusBanner(
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZoyaCardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ZoyaBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Accessibility Service",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAccessibilityActive) "Active - Taps & Gestures Armed" else "Disabled - Enable to control screen",
                        color = if (isAccessibilityActive) ZoyaEmerald else Color(0xFFFFAA00),
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onOpenAccessibilitySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAccessibilityActive) Color(0xFF2E1C48) else ZoyaRosePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (isAccessibilityActive) "Configured" else "Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF2E1C48))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Persistent Floating Bubble",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zoya stays on screen & runs commands in background",
                        color = Color(0xFFB5A7C9),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = onToggleFloatingBubble,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ZoyaRosePrimary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF26193E)
                    )
                )
            }
        }
    }
}

@Composable
fun ControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF381A45) else Color(0xFF1E1430)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(
            1.dp,
            if (isActive) ZoyaRosePrimary else Color(0xFF35224D),
            RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) ZoyaRosePrimary else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = if (isActive) ZoyaRosePrimary else Color(0xFF8E82A6),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ActionLauncherButton(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF201535),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(46.dp)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GestureChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF201535),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(text = label, fontSize = 11.sp)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = ZoyaRosePrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
