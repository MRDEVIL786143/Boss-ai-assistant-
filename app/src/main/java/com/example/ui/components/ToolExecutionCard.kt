package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BossCyanPrimary
import com.example.ui.theme.BossEmerald
import com.example.ui.theme.BossGoldSecondary

@Composable
fun ToolExecutionCard(
    toolName: String,
    args: String? = null,
    result: String? = null,
    status: String = "done",
    modifier: Modifier = Modifier
) {
    val (icon, title, color) = when (toolName.lowercase()) {
        "make_phone_call" -> Triple(Icons.Default.Phone, "Phone Call Action", BossCyanPrimary)
        "send_sms" -> Triple(Icons.Default.Sms, "SMS Dispatch", BossCyanPrimary)
        "send_whatsapp" -> Triple(Icons.Default.Chat, "WhatsApp Message", BossEmerald)
        "open_app" -> Triple(Icons.Default.Apps, "Application Launch", BossGoldSecondary)
        "toggle_flashlight" -> Triple(Icons.Default.FlashlightOn, "Flashlight Hardware", BossGoldSecondary)
        "set_volume" -> Triple(Icons.Default.VolumeUp, "Audio Volume", BossCyanPrimary)
        "take_photo" -> Triple(Icons.Default.CameraAlt, "Camera Capture", BossCyanPrimary)
        "open_settings" -> Triple(Icons.Default.Settings, "Device Settings", BossGoldSecondary)
        "read_notifications" -> Triple(Icons.Default.Notifications, "Notifications Reader", BossCyanPrimary)
        "accessibility_action" -> Triple(Icons.Default.TouchApp, "Screen Gesture", BossEmerald)
        "set_alarm", "set_timer" -> Triple(Icons.Default.Alarm, "Clock / Alarm", BossGoldSecondary)
        "save_memory" -> Triple(Icons.Default.Psychology, "Boss Memory Engine", BossCyanPrimary)
        else -> Triple(Icons.Default.Bolt, "System Tool: $toolName", BossCyanPrimary)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF101726))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (status == "executing") "EXECUTING..." else "EXECUTED",
                color = if (status == "executing") BossGoldSecondary else BossEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (!result.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
