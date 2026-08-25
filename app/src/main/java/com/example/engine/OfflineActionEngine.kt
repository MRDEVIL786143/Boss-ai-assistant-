package com.example.engine

import android.content.Context

data class AssistantResponse(
    val replyText: String,
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolResult: String? = null,
    val isCodeGenerated: Boolean = false,
    val codeLang: String = "",
    val codeSnippet: String = ""
)

object OfflineActionEngine {

    fun processCommand(
        context: Context,
        input: String,
        honorific: String = "Boss",
        language: String = "English"
    ): AssistantResponse {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        val salutation = when (language.lowercase()) {
            "hindi" -> if (honorific == "Sahab") "जी साहब! " else "जी बॉस! "
            "urdu" -> if (honorific == "Sahab") "حکم صاحب! " else "جی باس! "
            else -> if (honorific == "Sahab") "Right away, Sahab! " else "Right away, Boss! "
        }

        // --- Flashlight ---
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("light")) {
            val turnOff = lower.contains("off") || lower.contains("band") || lower.contains("close")
            val res = PhoneToolManager.toggleFlashlight(context, !turnOff)
            val stateWord = if (!turnOff) "ON" else "OFF"
            return AssistantResponse(
                replyText = "${salutation}Flashlight has been turned $stateWord as ordered.",
                toolName = "toggle_flashlight",
                toolArgs = "{\"state\": ${!turnOff}}",
                toolResult = res.message
            )
        }

        // --- Phone Call ---
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.contains("phone lagao") || lower.contains("call karo")) {
            val number = trimmed.replace(Regex("(?i)^(call|dial|phone lagao|call karo)\\s*"), "").trim()
            val res = PhoneToolManager.makePhoneCall(context, number)
            return AssistantResponse(
                replyText = "${salutation}Calling $number for you right now.",
                toolName = "make_phone_call",
                toolArgs = "{\"phone_number\": \"$number\"}",
                toolResult = res.message
            )
        }

        // --- SMS ---
        if (lower.startsWith("sms ") || lower.startsWith("send sms") || lower.contains("message bhejo")) {
            val parts = trimmed.replace(Regex("(?i)^(sms|send sms|message bhejo)\\s*"), "").split(Regex("[,:]|\\s+to\\s+|\\s+message\\s+"), 2)
            val recipient = if (parts.isNotEmpty()) parts[0].trim() else "12345"
            val msgBody = if (parts.size > 1) parts[1].trim() else "Hello from MyBossAI"
            val res = PhoneToolManager.sendSms(context, recipient, msgBody)
            return AssistantResponse(
                replyText = "${salutation}Dispatched SMS to $recipient.",
                toolName = "send_sms",
                toolArgs = "{\"phone_number\": \"$recipient\", \"message\": \"$msgBody\"}",
                toolResult = res.message
            )
        }

        // --- WhatsApp ---
        if (lower.contains("whatsapp")) {
            val phoneRegex = Regex("\\+?[0-9]{7,15}")
            val match = phoneRegex.find(trimmed)
            val phone = match?.value ?: ""
            val msg = if (phone.isNotEmpty()) trimmed.substringAfter(phone).trim() else "Hello from Boss"
            val res = if (phone.isNotEmpty()) {
                PhoneToolManager.sendWhatsAppMessage(context, phone, msg)
            } else {
                PhoneToolManager.openApp(context, "whatsapp")
            }
            return AssistantResponse(
                replyText = "${salutation}WhatsApp opened for you instantly.",
                toolName = "send_whatsapp",
                toolArgs = "{\"phone\": \"$phone\", \"message\": \"$msg\"}",
                toolResult = res.message
            )
        }

        // --- Open App ---
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("kholo ")) {
            val app = trimmed.replace(Regex("(?i)^(open|launch|kholo)\\s+"), "").replace(Regex("(?i)\\s+app"), "").trim()
            val res = PhoneToolManager.openApp(context, app)
            return AssistantResponse(
                replyText = "${salutation}Launching $app as requested.",
                toolName = "open_app",
                toolArgs = "{\"app_name\": \"$app\"}",
                toolResult = res.message
            )
        }

        // --- Volume ---
        if (lower.contains("volume") || lower.contains("awaz")) {
            val percentMatch = Regex("([0-9]{1,3})%?").find(lower)
            val percent = percentMatch?.groupValues?.get(1)?.toIntOrNull() ?: 70
            val res = PhoneToolManager.setVolume(context, percent)
            return AssistantResponse(
                replyText = "${salutation}Adjusted system volume to $percent%.",
                toolName = "set_volume",
                toolArgs = "{\"level\": $percent}",
                toolResult = res.message
            )
        }

        // --- Camera / Photo ---
        if (lower.contains("camera") || lower.contains("take photo") || lower.contains("photo khicho") || lower.contains("selfie")) {
            val front = lower.contains("selfie") || lower.contains("front")
            val res = PhoneToolManager.launchCamera(context, front)
            return AssistantResponse(
                replyText = "${salutation}Camera activated and ready to capture.",
                toolName = "take_photo",
                toolArgs = "{\"front_camera\": $front}",
                toolResult = res.message
            )
        }

        // --- Notifications ---
        if (lower.contains("notification") || lower.contains("notif")) {
            val res = PhoneToolManager.readNotifications()
            return AssistantResponse(
                replyText = "${salutation}Here are your recent notifications:\n\n${res.message}",
                toolName = "read_notifications",
                toolResult = res.message
            )
        }

        // --- Gestures / Screen Navigation ---
        if (lower.contains("go home") || lower == "home" || lower == "ghar jao") {
            val res = PhoneToolManager.executeAccessibilityAction("home")
            return AssistantResponse(replyText = "${salutation}Returning to Home screen.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("go back") || lower == "back" || lower == "piche") {
            val res = PhoneToolManager.executeAccessibilityAction("back")
            return AssistantResponse(replyText = "${salutation}Navigated back.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("scroll down") || lower.contains("niche scroll")) {
            val res = PhoneToolManager.executeAccessibilityAction("scroll_down")
            return AssistantResponse(replyText = "${salutation}Scrolled down.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("scroll up") || lower.contains("upar scroll")) {
            val res = PhoneToolManager.executeAccessibilityAction("scroll_up")
            return AssistantResponse(replyText = "${salutation}Scrolled up.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("read screen") || lower.contains("screen padho")) {
            val res = PhoneToolManager.executeAccessibilityAction("read_screen")
            return AssistantResponse(replyText = "${salutation}Screen contents captured:\n\n${res.data}", toolName = "accessibility_action", toolResult = res.data)
        }

        // --- Settings ---
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            val res = PhoneToolManager.openSettings(context, "wifi")
            return AssistantResponse(replyText = "${salutation}Opened Wi-Fi settings.", toolName = "open_settings", toolResult = res.message)
        }
        if (lower.contains("bluetooth") || lower.contains("bt")) {
            val res = PhoneToolManager.openSettings(context, "bluetooth")
            return AssistantResponse(replyText = "${salutation}Opened Bluetooth settings.", toolName = "open_settings", toolResult = res.message)
        }

        // --- Coding / Development Assistance ---
        if (lower.contains("code") || lower.contains("program") || lower.contains("python") || lower.contains("kotlin") || lower.contains("flutter") || lower.contains("script")) {
            val (lang, snippet) = generateCodeSnippet(lower)
            return AssistantResponse(
                replyText = "${salutation}Here is the complete, high-performance code as commanded, $honorific:\n\n```$lang\n$snippet\n```",
                isCodeGenerated = true,
                codeLang = lang,
                codeSnippet = snippet
            )
        }

        // --- General Obedient Response ---
        val generalReply = when (language.lowercase()) {
            "hindi" -> "${salutation}आपका हर हुक्म सर आंखों पर! बताइए क्या करना है — फोन कॉल, मैसेज, ऐप खोलना, टॉर्च, या कोडिंग?"
            "urdu" -> "${salutation}آپ کا ہر حکم سر آنکھوں پر! بتائیں کیا کرنا ہے — فون، واٹس ایپ، ٹارچ، یا کوئی کوڈ لکھنا ہے؟"
            else -> "${salutation}I am entirely at your command, $honorific. Give me any task — phone actions, coding, system controls, or messaging — and I will execute it instantly!"
        }

        return AssistantResponse(replyText = generalReply)
    }

    private fun generateCodeSnippet(query: String): Pair<String, String> {
        return when {
            query.contains("compose") || query.contains("kotlin") || query.contains("android") -> {
                "kotlin" to """
// Production-Ready Jetpack Compose Component for $1
@Composable
fun BossActionButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(Icons.Default.Bolt, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
""".trimIndent()
            }
            query.contains("flutter") || query.contains("dart") -> {
                "dart" to """
import 'package:flutter/material.dart';

class BossCardWidget extends StatelessWidget {
  final String title;
  final VoidCallback onTap;

  const BossCardWidget({Key? key, required this.title, required this.onTap}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFF111827),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFF00E5FF), width: 1.5),
        ),
        child: Row(
          children: [
            const Icon(Icons.flash_on, color: Color(0xFF00E5FF)),
            const SizedBox(width: 12),
            Text(
              title,
              style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ),
    );
  }
}
""".trimIndent()
            }
            else -> {
                "python" to """
import time
import requests

def execute_boss_task(task_name: str) -> dict:
    print(f"[*] Executing task '{task_name}' immediately for Boss...")
    # Simulated execution engine
    start_time = time.time()
    time.sleep(0.5)
    execution_time = round(time.time() - start_time, 3)
    
    return {
        "status": "success",
        "task": task_name,
        "execution_time_sec": execution_time,
        "message": "Task completed with 100% accuracy, Boss."
    }

if __name__ == "__main__":
    result = execute_boss_task("System Optimization & Phone Control")
    print(result)
""".trimIndent()
            }
        }
    }
}
