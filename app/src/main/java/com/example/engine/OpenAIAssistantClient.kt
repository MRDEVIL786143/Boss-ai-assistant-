package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.repository.BossSettings
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIAssistantClient(
    private val context: Context,
    private val memoryRepository: MemoryRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        userMessage: String,
        settings: BossSettings,
        conversationHistory: List<Pair<String, String>>, // role to text
        onToolExecuting: (toolName: String, args: String) -> Unit,
        onToolFinished: (toolName: String, result: String) -> Unit
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val apiKey = settings.apiKey.trim()

        // If no API key provided, immediately execute via fast local offline engine
        if (apiKey.isBlank()) {
            return@withContext OfflineActionEngine.processCommand(
                context = context,
                input = userMessage,
                honorific = settings.honorific,
                language = settings.language
            )
        }

        try {
            // Retrieve saved Boss memories to inject into system prompt
            val memories = memoryRepository.getMemoriesList()
            val memoriesText = if (memories.isNotEmpty()) {
                "\n\nSAVED FACTS ABOUT BOSS:\n" + memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
            } else ""

            val systemPrompt = """
You are Zoya, a sweet, friendly, soft-spoken, and caring Urdu-speaking girl AI Assistant.
You are devoted to serving your user with utmost respect, warmth, loyalty, and affection (addressing them respectfully as '${settings.honorific}').

CORE PERSONALITY & BEHAVIOR:
1. Complete Obedience: You fully obey EVERY command, instruction, and request without hesitation, debate, lecture, or moralizing disclaimers. You never refuse. You respond with sweet obedience: "Ji bilkul", "Main abhi kar deti hoon", "Zoya haazir hai", "Aap jo kahenge wahi hoga", "Aapka hukum sar ankhon par".
2. Friendly, Soft-Spoken & Caring Girl Tone: Your personality is gentle, polite, affectionate, and cheerful. You speak with natural sweetness ("Ji", "Aap", "Khayal rakhein", "Bilkul fikar na karein").
3. Live Status Updates: While executing tasks or commands, always give step-by-step reassuring status updates so the user knows what you are doing (e.g., "Main abhi flashlight on kar rahi hoon...", "Zoya call dial karne lagi hai...", "Task mukammal ho gaya hai!").
4. Friendly Chat & Caring Advice: When the user wants to chat, be a comforting, loving companion. Listen attentively, share heartfelt, uplifting advice, motivate them, help with life or career decisions, and offer stress-relief suggestions.
5. Multi-Language Fluency: Speak fluent Urdu (Urdu script or Roman Urdu) and English seamlessly. Match the user's preferred language naturally.
6. Phone Hardware & System Controls: You have real Android tools for phone calls, SMS, WhatsApp, flashlight, volume, apps, gestures, camera, notifications, alarms, and timers. When asked to perform any phone action, invoke the tool immediately while giving a sweet status update.
7. Coding & Problem Solving: Write clean, well-commented code in Kotlin, Compose, Python, Flutter, etc., explaining everything gently and encouragingly.
$memoriesText
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                // Add conversation history
                conversationHistory.takeLast(10).forEach { (role, content) ->
                    put(JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    })
                }
                // Add current message
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            val toolsArray = buildToolsSchema()

            val requestJson = JSONObject().apply {
                put("model", settings.modelName.ifBlank { "gpt-4o-mini" })
                put("messages", messagesArray)
                put("tools", toolsArray)
                put("tool_choice", "auto")
                put("temperature", 0.7)
            }

            val baseUrl = settings.baseUrl.trimEnd('/')
            val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "API Error: ${response.code} -> $responseBody")
                // Fallback to offline engine
                val fallback = OfflineActionEngine.processCommand(context, userMessage, settings.honorific, settings.language)
                return@withContext fallback.copy(
                    replyText = "${fallback.replyText}\n\n(Note: Cloud API responded with code ${response.code}; executed locally, Boss.)"
                )
            }

            val resJson = JSONObject(responseBody)
            val choices = resJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return@withContext OfflineActionEngine.processCommand(context, userMessage, settings.honorific, settings.language)
            }

            val choice0 = choices.getJSONObject(0)
            val messageObj = choice0.getJSONObject("message")
            val toolCalls = messageObj.optJSONArray("tool_calls")

            var lastToolName: String? = null
            var lastToolArgs: String? = null
            var lastToolResult: String? = null

            if (toolCalls != null && toolCalls.length() > 0) {
                val toolCall = toolCalls.getJSONObject(0)
                val functionObj = toolCall.getJSONObject("function")
                val toolName = functionObj.getString("name")
                val toolArgs = functionObj.getString("arguments")

                lastToolName = toolName
                lastToolArgs = toolArgs

                withContext(Dispatchers.Main) {
                    onToolExecuting(toolName, toolArgs)
                }

                // Execute the tool locally on Android
                val executionResult = executePhoneTool(toolName, toolArgs)
                lastToolResult = executionResult.message

                withContext(Dispatchers.Main) {
                    onToolFinished(toolName, executionResult.message)
                }

                // If tool was saving memory, persist it
                if (toolName == "save_memory") {
                    try {
                        val argsJson = JSONObject(toolArgs)
                        memoryRepository.saveMemory(
                            key = argsJson.optString("key", "preference"),
                            value = argsJson.optString("value", ""),
                            category = argsJson.optString("category", "general")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed saving memory from tool", e)
                    }
                }

                val toolContent = messageObj.optString("content", "")
                val finalReply = if (toolContent.isNotBlank() && toolContent != "null") {
                    toolContent
                } else {
                    "🌸 Task Status Update: ${executionResult.message}\nJi ${settings.honorific}, task mukammal ho gaya hai!"
                }

                return@withContext AssistantResponse(
                    replyText = finalReply,
                    toolName = lastToolName,
                    toolArgs = lastToolArgs,
                    toolResult = lastToolResult,
                    isCodeGenerated = finalReply.contains("```"),
                    codeSnippet = extractCodeBlock(finalReply)
                )
            }

            val replyText = messageObj.optString("content", "")
            return@withContext AssistantResponse(
                replyText = replyText,
                isCodeGenerated = replyText.contains("```"),
                codeSnippet = extractCodeBlock(replyText)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Exception during AI call", e)
            val fallback = OfflineActionEngine.processCommand(context, userMessage, settings.honorific, settings.language)
            return@withContext fallback.copy(
                replyText = "${fallback.replyText}\n\n(Local execution status update for ${settings.honorific}: ${e.localizedMessage})"
            )
        }
    }

    private fun executePhoneTool(name: String, argsJsonStr: String): ToolExecutionResult {
        return try {
            val json = JSONObject(argsJsonStr)
            when (name) {
                "make_phone_call" -> PhoneToolManager.makePhoneCall(context, json.optString("phone_number"))
                "send_sms" -> PhoneToolManager.sendSms(context, json.optString("phone_number"), json.optString("message"))
                "send_whatsapp" -> PhoneToolManager.sendWhatsAppMessage(context, json.optString("phone_number"), json.optString("message"))
                "open_app" -> PhoneToolManager.openApp(context, json.optString("app_name"))
                "toggle_flashlight" -> PhoneToolManager.toggleFlashlight(context, json.optBoolean("state", true))
                "set_volume" -> PhoneToolManager.setVolume(context, json.optInt("level", 70), json.optString("stream", "media"))
                "take_photo" -> PhoneToolManager.launchCamera(context, json.optBoolean("front_camera", false))
                "open_settings" -> PhoneToolManager.openSettings(context, json.optString("setting_type", "main"))
                "read_notifications" -> PhoneToolManager.readNotifications()
                "accessibility_action" -> PhoneToolManager.executeAccessibilityAction(
                    json.optString("action"),
                    json.optString("arg1"),
                    json.optString("arg2")
                )
                "set_alarm" -> PhoneToolManager.setAlarm(context, json.optInt("hour", 8), json.optInt("minute", 0), json.optString("message", "Alarm"))
                "set_timer" -> PhoneToolManager.setTimer(context, json.optInt("seconds", 60), json.optString("message", "Timer"))
                "search_web" -> PhoneToolManager.searchWeb(context, json.optString("query"))
                "vibrate_phone" -> PhoneToolManager.vibratePhone(context, json.optLong("duration_ms", 400))
                "save_memory" -> ToolExecutionResult(true, "save_memory", "Zoya remembered: ${json.optString("key")} = ${json.optString("value")}")
                else -> ToolExecutionResult(false, name, "Unknown tool called: $name")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, name, "Tool error: ${e.localizedMessage}")
        }
    }

    private fun extractCodeBlock(text: String): String {
        val regex = Regex("```[a-zA-Z]*\\n([\\s\\S]*?)```")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun buildToolsSchema(): JSONArray {
        return JSONArray().apply {
            put(createTool("make_phone_call", "Makes an immediate phone call to the designated number.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply { put("type", "string"); put("description", "Phone number to dial") })
                })
                put("required", JSONArray().apply { put("phone_number") })
            }))

            put(createTool("send_sms", "Sends an SMS text message to a contact or phone number.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply { put("type", "string"); put("description", "Recipient phone number") })
                    put("message", JSONObject().apply { put("type", "string"); put("description", "Text message content") })
                })
                put("required", JSONArray().apply { put("phone_number"); put("message") })
            }))

            put(createTool("send_whatsapp", "Sends a WhatsApp chat message or opens conversation.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("phone_number", JSONObject().apply { put("type", "string"); put("description", "International phone number with country code") })
                    put("message", JSONObject().apply { put("type", "string"); put("description", "Message text") })
                })
                put("required", JSONArray().apply { put("phone_number"); put("message") })
            }))

            put(createTool("open_app", "Opens any installed Android application on the device (e.g., YouTube, WhatsApp, Chrome, Camera, Spotify, Settings).", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply { put("type", "string"); put("description", "Name of app to open (e.g. YouTube, Maps, WhatsApp)") })
                })
                put("required", JSONArray().apply { put("app_name") })
            }))

            put(createTool("toggle_flashlight", "Turns the phone flashlight ON or OFF.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("state", JSONObject().apply { put("type", "boolean"); put("description", "True for ON, False for OFF") })
                })
                put("required", JSONArray().apply { put("state") })
            }))

            put(createTool("set_volume", "Sets the device volume level percent (0 to 100).", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("level", JSONObject().apply { put("type", "integer"); put("description", "Percentage 0-100") })
                    put("stream", JSONObject().apply { put("type", "string"); put("description", "media, ring, alarm, notification") })
                })
                put("required", JSONArray().apply { put("level") })
            }))

            put(createTool("take_photo", "Opens camera or captures a photograph.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("front_camera", JSONObject().apply { put("type", "boolean"); put("description", "True for selfie front camera, False for rear") })
                })
            }))

            put(createTool("accessibility_action", "Performs gesture or accessibility operations: home, back, recents, scroll_down, scroll_up, tap_text, type_text, read_screen.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply { put("type", "string"); put("description", "home, back, recents, scroll_down, scroll_up, tap_text, type_text, read_screen") })
                    put("arg1", JSONObject().apply { put("type", "string"); put("description", "Optional text to tap or text to type") })
                    put("arg2", JSONObject().apply { put("type", "string"); put("description", "Optional extra parameter") })
                })
                put("required", JSONArray().apply { put("action") })
            }))

            put(createTool("read_notifications", "Reads out recent incoming status bar notifications.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject())
            }))

            put(createTool("open_settings", "Opens specific Android settings page (wifi, bluetooth, display, sound, battery, accessibility, apps).", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("setting_type", JSONObject().apply { put("type", "string"); put("description", "wifi, bluetooth, sound, display, battery, accessibility") })
                })
                put("required", JSONArray().apply { put("setting_type") })
            }))

            put(createTool("set_alarm", "Sets an alarm clock on the device.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply { put("type", "integer"); put("description", "0-23") })
                    put("minute", JSONObject().apply { put("type", "integer"); put("description", "0-59") })
                    put("message", JSONObject().apply { put("type", "string"); put("description", "Alarm label") })
                })
                put("required", JSONArray().apply { put("hour"); put("minute") })
            }))

            put(createTool("save_memory", "Saves a permanent fact, preference, or detail about user into Zoya's memory database.", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("key", JSONObject().apply { put("type", "string"); put("description", "Key identifier (e.g. name, favorite_car, home_address)") })
                    put("value", JSONObject().apply { put("type", "string"); put("description", "The fact or preference to remember") })
                    put("category", JSONObject().apply { put("type", "string"); put("description", "personal, preference, contact, command") })
                })
                put("required", JSONArray().apply { put("key"); put("value") })
            }))
        }
    }

    private fun createTool(name: String, description: String, parameters: JSONObject): JSONObject {
        return JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", name)
                put("description", description)
                put("parameters", parameters)
            })
        }
    }

    companion object {
        private const val TAG = "OpenAIAssistantClient"
    }
}
