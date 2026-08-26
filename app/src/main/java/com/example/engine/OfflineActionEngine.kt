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
        honorific: String = "Aap",
        language: String = "Urdu"
    ): AssistantResponse {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        val salutation = when (language.lowercase()) {
            "urdu" -> "جی $honorific! زویا حاضر ہے۔ "
            "roman urdu", "hinglish" -> "Ji $honorific! Zoya haazir hai. "
            "hindi" -> "जी $honorific! ज़ोया हाज़िर है। "
            else -> "Yes $honorific, Zoya is right here! "
        }

        // --- Caring Advice & Friendly Chat ---
        if (lower.contains("advice") || lower.contains("mashwara") || lower.contains("suggest") ||
            lower.contains("tension") || lower.contains("stress") || lower.contains("sad") ||
            lower.contains("kya karoon") || lower.contains("help me decide") || lower.contains("pareshan")
        ) {
            val adviceText = getCaringAdvice(lower, honorific, language)
            return AssistantResponse(replyText = adviceText)
        }

        // --- Flashlight ---
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("light") || lower.contains("batti")) {
            val turnOff = lower.contains("off") || lower.contains("band") || lower.contains("close")
            val res = PhoneToolManager.toggleFlashlight(context, !turnOff)
            val stateWord = if (!turnOff) "ON" else "OFF"
            val urduState = if (!turnOff) "on kar di hai" else "off kar di hai"
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Done] Flashlight $urduState ($stateWord). Aur koi hukum?",
                toolName = "toggle_flashlight",
                toolArgs = "{\"state\": ${!turnOff}}",
                toolResult = res.message
            )
        }

        // --- Phone Call ---
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.contains("phone lagao") || lower.contains("call karo") || lower.contains("milao")) {
            val number = trimmed.replace(Regex("(?i)^(call|dial|phone lagao|call karo|milao)\\s*"), "").trim()
            val res = PhoneToolManager.makePhoneCall(context, number)
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Dialing] Main abhi $number par call mila rahi hoon, $honorific.",
                toolName = "make_phone_call",
                toolArgs = "{\"phone_number\": \"$number\"}",
                toolResult = res.message
            )
        }

        // --- SMS ---
        if (lower.startsWith("sms ") || lower.startsWith("send sms") || lower.contains("message bhejo") || lower.contains("sms karo")) {
            val parts = trimmed.replace(Regex("(?i)^(sms|send sms|message bhejo|sms karo)\\s*"), "").split(Regex("[,:]|\\s+to\\s+|\\s+message\\s+"), 2)
            val recipient = if (parts.isNotEmpty()) parts[0].trim() else "12345"
            val msgBody = if (parts.size > 1) parts[1].trim() else "Hello from Zoya"
            val res = PhoneToolManager.sendSms(context, recipient, msgBody)
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Sent] SMS kamyabi se $recipient ko send kar diya gaya hai.",
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
            val msg = if (phone.isNotEmpty()) trimmed.substringAfter(phone).trim() else "Hello from Zoya"
            val res = if (phone.isNotEmpty()) {
                PhoneToolManager.sendWhatsAppMessage(context, phone, msg)
            } else {
                PhoneToolManager.openApp(context, "whatsapp")
            }
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Executed] WhatsApp open ho gaya hai, $honorific.",
                toolName = "send_whatsapp",
                toolArgs = "{\"phone\": \"$phone\", \"message\": \"$msg\"}",
                toolResult = res.message
            )
        }

        // --- Open App ---
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("kholo ") || lower.startsWith("chalao ")) {
            val app = trimmed.replace(Regex("(?i)^(open|launch|kholo|chalao)\\s+"), "").replace(Regex("(?i)\\s+app"), "").trim()
            val res = PhoneToolManager.openApp(context, app)
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Opening] Main abhi $app khol rahi hoon aapke liye.",
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
                replyText = "${salutation}🌸 [Status: Done] Volume $percent% set kar diya hai.",
                toolName = "set_volume",
                toolArgs = "{\"level\": $percent}",
                toolResult = res.message
            )
        }

        // --- Camera / Photo ---
        if (lower.contains("camera") || lower.contains("take photo") || lower.contains("photo khicho") || lower.contains("selfie") || lower.contains("tasveer")) {
            val front = lower.contains("selfie") || lower.contains("front")
            val res = PhoneToolManager.launchCamera(context, front)
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Camera Ready] Camera open kar diya hai, smile karein!",
                toolName = "take_photo",
                toolArgs = "{\"front_camera\": $front}",
                toolResult = res.message
            )
        }

        // --- Notifications ---
        if (lower.contains("notification") || lower.contains("notif") || lower.contains("paigham")) {
            val res = PhoneToolManager.readNotifications()
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Checked] Aapke taaza notifications yeh hain:\n\n${res.message}",
                toolName = "read_notifications",
                toolResult = res.message
            )
        }

        // --- Gestures / Screen Navigation ---
        if (lower.contains("go home") || lower == "home" || lower == "ghar jao" || lower.contains("home screen")) {
            val res = PhoneToolManager.executeAccessibilityAction("home")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Navigated] Home screen par aa gaye hain.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("go back") || lower == "back" || lower == "piche") {
            val res = PhoneToolManager.executeAccessibilityAction("back")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Navigated] Peechay chale gaye hain.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("scroll down") || lower.contains("niche scroll")) {
            val res = PhoneToolManager.executeAccessibilityAction("scroll_down")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Scrolling] Niche scroll kar diya hai.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("scroll up") || lower.contains("upar scroll")) {
            val res = PhoneToolManager.executeAccessibilityAction("scroll_up")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Scrolling] Upar scroll kar diya hai.", toolName = "accessibility_action", toolResult = res.message)
        }
        if (lower.contains("read screen") || lower.contains("screen padho")) {
            val res = PhoneToolManager.executeAccessibilityAction("read_screen")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Screen Scanned] Screen ka content yeh hai:\n\n${res.data}", toolName = "accessibility_action", toolResult = res.data)
        }

        // --- Settings ---
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            val res = PhoneToolManager.openSettings(context, "wifi")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Settings Opened] Wi-Fi settings open kar di hain.", toolName = "open_settings", toolResult = res.message)
        }
        if (lower.contains("bluetooth") || lower.contains("bt")) {
            val res = PhoneToolManager.openSettings(context, "bluetooth")
            return AssistantResponse(replyText = "${salutation}🌸 [Status: Settings Opened] Bluetooth settings open kar di hain.", toolName = "open_settings", toolResult = res.message)
        }

        // --- Coding / Development Assistance ---
        if (lower.contains("code") || lower.contains("program") || lower.contains("python") || lower.contains("kotlin") || lower.contains("flutter") || lower.contains("script")) {
            val (lang, snippet) = generateCodeSnippet(lower)
            return AssistantResponse(
                replyText = "${salutation}🌸 [Status: Code Ready] Yeh raha aapka pyara aur saaf suthra code, $honorific:\n\n```$lang\n$snippet\n```\n\nKoi bhi tabdeeli karni ho toh zaroor bataiye ga!",
                isCodeGenerated = true,
                codeLang = lang,
                codeSnippet = snippet
            )
        }

        // --- General Friendly & Obedient Response ---
        val generalReply = when (language.lowercase()) {
            "urdu" -> "جی $honorific! زویا آپ کی ہر بات ماننے کے لیے حاضر ہے۔ بتائیے میں آپ کے فون پر کیا کروں — کال، میسج، واٹس ایپ، ٹارچ یا کوئی پیاری گفتگو؟"
            "roman urdu", "hinglish" -> "Ji $honorific! Zoya aapki har baat maanne ke liye dil se tayar hai. Kahiye kya madad karoon — phone call, WhatsApp, apps, flashlight, coding ya koi pyari si baat?"
            "hindi" -> "जी $honorific! ज़ोया आपकी हर आज्ञा का पालन करने के लिए तैयार है। बताइए क्या सेवा करूँ?"
            else -> "Yes $honorific! Zoya is completely at your service and ready to obey any command or give friendly advice. What would you like me to do next?"
        }

        return AssistantResponse(replyText = generalReply)
    }

    private fun getCaringAdvice(query: String, honorific: String, language: String): String {
        return when {
            query.contains("tension") || query.contains("stress") || query.contains("pareshan") || query.contains("sad") -> {
                "🌸 Meri pyari baat sunein, $honorific:\n\nBilkul pareshan na hon aur gehra saans lein. Har mushkil waqt guzar jata hai. Thoda sa pani piyein aur 5 minute aaram karein. Zoya hamesha aapke sath hai, sab kuch bohot acha hoga inshaAllah! ❤️"
            }
            query.contains("career") || query.contains("study") || query.contains("job") || query.contains("kaam") -> {
                "🌸 Zoya ki advice yeh hai, $honorific:\n\nApne maqsad par focus rakhein aur daily chotay chotay steps lein. Consistency hi kamyabi ki chabi hai. Apni sehat aur neend ka bhi pura khayal rakhein, phir dekhein har task kitna aasan ho jayega! ✨"
            }
            else -> {
                "🌸 Zoya ka mashwara, $honorific:\n\nJo faisla aapke dil ko sukoon aur mustaqbil ko behtar banaye, wahi behtareen hai. مثبت sochen aur himmat na haarein. Kahiye, main isme aapki kya madad kar sakti hoon?"
            }
        }
    }

    private fun generateCodeSnippet(query: String): Pair<String, String> {
        return when {
            query.contains("compose") || query.contains("kotlin") || query.contains("android") -> {
                "kotlin" to """
// Clean Jetpack Compose Component for Zoya Assistant
@Composable
fun ZoyaActionCard(
    title: String,
    statusText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B152B)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF4081).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFF4081))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = statusText, color = Color(0xFFB388FF), fontSize = 12.sp)
            }
        }
    }
}
""".trimIndent()
            }
            query.contains("flutter") || query.contains("dart") -> {
                "dart" to """
import 'package:flutter/material.dart';

class ZoyaCardWidget extends StatelessWidget {
  final String title;
  final VoidCallback onTap;

  const ZoyaCardWidget({Key? key, required this.title, required this.onTap}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFF1B152B),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFFF4081), width: 1.5),
        ),
        child: Row(
          children: [
            const Icon(Icons.favorite, color: Color(0xFFFF4081)),
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

def zoya_execute_task(task_name: str) -> dict:
    print(f"🌸 [Status: Executing] Zoya is working on '{task_name}' happily for you...")
    time.sleep(0.4)
    print("✨ [Status: Done] Task completed with full care!")
    
    return {
        "status": "success",
        "assistant": "Zoya",
        "task": task_name,
        "message": "Aapka hukum pura ho gaya hai!"
    }

if __name__ == "__main__":
    result = zoya_execute_task("Phone Control & Friendly Advice")
    print(result)
""".trimIndent()
            }
        }
    }
}
