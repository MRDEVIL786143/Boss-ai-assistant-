package com.example.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.example.service.BossAccessibilityService
import com.example.service.BossNotificationService
import org.json.JSONObject

data class ToolExecutionResult(
    val success: Boolean,
    val toolName: String,
    val message: String,
    val data: String? = null
)

object PhoneToolManager {
    private const val TAG = "PhoneToolManager"
    private var isTorchOn = false

    // --- Phone Calls ---
    fun makePhoneCall(context: Context, phoneNumber: String): ToolExecutionResult {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$cleanNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(intent)
                ToolExecutionResult(true, "make_phone_call", "Dialing $cleanNumber for you, Boss.")
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                ToolExecutionResult(true, "make_phone_call", "Opened dialer with $cleanNumber, Boss.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making phone call", e)
            ToolExecutionResult(false, "make_phone_call", "Failed to place call: ${e.localizedMessage}")
        }
    }

    // --- SMS ---
    fun sendSms(context: Context, phoneNumber: String, messageText: String): ToolExecutionResult {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(cleanNumber, null, messageText, null, null)
                ToolExecutionResult(true, "send_sms", "SMS sent to $cleanNumber: \"$messageText\"")
            } else {
                val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("smsto:$cleanNumber")
                    putExtra("sms_body", messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                ToolExecutionResult(true, "send_sms", "Opened SMS composer for $cleanNumber with message.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
            ToolExecutionResult(false, "send_sms", "Failed to send SMS: ${e.localizedMessage}")
        }
    }

    // --- WhatsApp Message ---
    fun sendWhatsAppMessage(context: Context, phoneNumber: String, messageText: String): ToolExecutionResult {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(messageText)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                ToolExecutionResult(true, "send_whatsapp", "Opening WhatsApp for $cleanNumber with your message, Boss.")
            } catch (e: Exception) {
                // Fallback to browser or regular intent
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolExecutionResult(true, "send_whatsapp", "WhatsApp opened via link for $cleanNumber, Boss.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp", e)
            ToolExecutionResult(false, "send_whatsapp", "Could not send WhatsApp message: ${e.localizedMessage}")
        }
    }

    // --- Open Any Installed App ---
    fun openApp(context: Context, appQuery: String): ToolExecutionResult {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val queryLower = appQuery.lowercase().trim()

            // Check common app mappings
            val commonPackage = when (queryLower) {
                "youtube" -> "com.google.android.youtube"
                "whatsapp" -> "com.whatsapp"
                "chrome", "google chrome", "browser" -> "com.android.chrome"
                "maps", "google maps" -> "com.google.android.apps.maps"
                "camera" -> "com.android.camera"
                "settings" -> "com.android.settings"
                "spotify" -> "com.spotify.music"
                "instagram" -> "com.instagram.android"
                "telegram" -> "org.telegram.messenger"
                "gmail", "email" -> "com.google.android.gm"
                "calculator" -> "com.google.android.calculator"
                "clock" -> "com.google.android.deskclock"
                "gallery", "photos" -> "com.google.android.apps.photos"
                "play store", "playstore" -> "com.android.vending"
                else -> null
            }

            var launchIntent: Intent? = null
            if (commonPackage != null) {
                launchIntent = pm.getLaunchIntentForPackage(commonPackage)
            }

            if (launchIntent == null) {
                // Search by package name or label
                for (app in packages) {
                    val label = pm.getApplicationLabel(app).toString().lowercase()
                    if (label.contains(queryLower) || app.packageName.lowercase().contains(queryLower)) {
                        launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) break
                    }
                }
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ToolExecutionResult(true, "open_app", "Opened $appQuery instantly, Boss!")
            } else {
                // Fallback: search on Play Store or Web
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$queryLower")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(marketIntent)
                    ToolExecutionResult(true, "open_app", "App not found locally; opened Play Store for $appQuery, Boss.")
                } catch (e: Exception) {
                    ToolExecutionResult(false, "open_app", "Could not find app $appQuery on device.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app", e)
            ToolExecutionResult(false, "open_app", "Failed to open $appQuery: ${e.localizedMessage}")
        }
    }

    // --- Flashlight Toggle ---
    fun toggleFlashlight(context: Context, state: Boolean? = null): ToolExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return ToolExecutionResult(false, "toggle_flashlight", "No flashlight hardware detected on this device.")
            }

            val targetState = state ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            val stateStr = if (targetState) "ON" else "OFF"
            ToolExecutionResult(true, "toggle_flashlight", "Flashlight turned $stateStr as ordered, Boss!")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight", e)
            ToolExecutionResult(false, "toggle_flashlight", "Failed to toggle flashlight: ${e.localizedMessage}")
        }
    }

    // --- Volume Control ---
    fun setVolume(context: Context, levelPercent: Int, stream: String = "media"): ToolExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = when (stream.lowercase()) {
                "ring", "call", "ringer" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                "notification" -> AudioManager.STREAM_NOTIFICATION
                "voice" -> AudioManager.STREAM_VOICE_CALL
                else -> AudioManager.STREAM_MUSIC
            }
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            val clampedPercent = levelPercent.coerceIn(0, 100)
            val targetIndex = ((clampedPercent / 100.0) * maxVol).toInt().coerceIn(0, maxVol)
            audioManager.setStreamVolume(streamType, targetIndex, AudioManager.FLAG_SHOW_UI)
            ToolExecutionResult(true, "set_volume", "Volume set to $clampedPercent% ($targetIndex/$maxVol), Boss.")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            ToolExecutionResult(false, "set_volume", "Failed to set volume: ${e.localizedMessage}")
        }
    }

    // --- Settings Pages ---
    fun openSettings(context: Context, settingType: String): ToolExecutionResult {
        return try {
            val action = when (settingType.lowercase()) {
                "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                "notifications" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
                "overlay", "draw_overlay" -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "open_settings", "Opened $settingType settings for you, Boss.")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            ToolExecutionResult(false, "open_settings", "Could not open $settingType settings: ${e.localizedMessage}")
        }
    }

    // --- Camera / Photos ---
    fun launchCamera(context: Context, frontCamera: Boolean = false): ToolExecutionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                if (frontCamera) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val camType = if (frontCamera) "front" else "rear"
            ToolExecutionResult(true, "take_photo", "Opened $camType camera ready to shoot, Boss!")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            ToolExecutionResult(false, "take_photo", "Failed to open camera: ${e.localizedMessage}")
        }
    }

    // --- Alarm / Timer ---
    fun setAlarm(context: Context, hour: Int, minute: Int, message: String): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Boss's Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ToolExecutionResult(true, "set_alarm", "Alarm set for $timeStr: \"$message\", Boss!")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm", e)
            ToolExecutionResult(false, "set_alarm", "Could not set alarm: ${e.localizedMessage}")
        }
    }

    fun setTimer(context: Context, seconds: Int, message: String): ToolExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Boss's Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "set_timer", "Timer set for $seconds seconds: \"$message\", Boss!")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting timer", e)
            ToolExecutionResult(false, "set_timer", "Could not set timer: ${e.localizedMessage}")
        }
    }

    // --- Web Search / Open URL ---
    fun searchWeb(context: Context, query: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "search_web", "Searching web for \"$query\", Boss.")
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ToolExecutionResult(true, "search_web", "Opened Google Search for \"$query\", Boss.")
        }
    }

    // --- Vibration Feedback ---
    fun vibratePhone(context: Context, durationMs: Long = 400): ToolExecutionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
            ToolExecutionResult(true, "vibrate", "Haptic pulse executed, Boss.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "vibrate", "Vibration failed: ${e.localizedMessage}")
        }
    }

    // --- Accessibility Automation & Screen Gestures ---
    fun executeAccessibilityAction(action: String, arg1: String = "", arg2: String = ""): ToolExecutionResult {
        val service = BossAccessibilityService.instance
            ?: return ToolExecutionResult(
                false,
                "accessibility_action",
                "Accessibility Service is not enabled. Please enable MyBossAI in Settings > Accessibility."
            )

        return when (action.lowercase()) {
            "home" -> {
                val success = service.goHome()
                ToolExecutionResult(success, "accessibility_action", if (success) "Navigated Home, Boss." else "Failed to go home")
            }
            "back" -> {
                val success = service.goBack()
                ToolExecutionResult(success, "accessibility_action", if (success) "Pressed Back, Boss." else "Failed to go back")
            }
            "recents", "recent_apps" -> {
                val success = service.openRecents()
                ToolExecutionResult(success, "accessibility_action", if (success) "Opened Recent Apps, Boss." else "Failed to open recents")
            }
            "notifications" -> {
                val success = service.openNotifications()
                ToolExecutionResult(success, "accessibility_action", if (success) "Pulled down notifications, Boss." else "Failed to open notifications")
            }
            "quick_settings" -> {
                val success = service.openQuickSettings()
                ToolExecutionResult(success, "accessibility_action", if (success) "Opened Quick Settings, Boss." else "Failed to open quick settings")
            }
            "scroll_down", "scroll_forward" -> {
                val success = service.scrollForward()
                ToolExecutionResult(success, "accessibility_action", if (success) "Scrolled down screen, Boss." else "Could not scroll down")
            }
            "scroll_up", "scroll_backward" -> {
                val success = service.scrollBackward()
                ToolExecutionResult(success, "accessibility_action", if (success) "Scrolled up screen, Boss." else "Could not scroll up")
            }
            "tap_text", "click_text" -> {
                val success = service.clickNodeByText(arg1)
                ToolExecutionResult(success, "accessibility_action", if (success) "Tapped \"$arg1\" on screen, Boss." else "Could not locate \"$arg1\" on screen")
            }
            "type_text", "input_text" -> {
                val success = service.inputText(arg1)
                ToolExecutionResult(success, "accessibility_action", if (success) "Typed text: \"$arg1\", Boss." else "No active input field found to type")
            }
            "read_screen" -> {
                val text = service.readScreenText()
                ToolExecutionResult(true, "accessibility_action", "Screen contents retrieved, Boss.", text)
            }
            "tap_coordinates" -> {
                val x = arg1.toFloatOrNull() ?: 500f
                val y = arg2.toFloatOrNull() ?: 1000f
                val success = service.clickCoordinates(x, y)
                ToolExecutionResult(success, "accessibility_action", "Tapped at coordinates ($x, $y), Boss.")
            }
            else -> ToolExecutionResult(false, "accessibility_action", "Unknown gesture action: $action")
        }
    }

    // --- Read Notifications ---
    fun readNotifications(): ToolExecutionResult {
        val summary = BossNotificationService.getNotificationsSummary()
        return ToolExecutionResult(true, "read_notifications", summary, summary)
    }

    // --- Universal Local / Rule-based Quick Action Dispatcher ---
    fun executeLocalAction(context: Context, rawCommand: String): ToolExecutionResult {
        val cmd = rawCommand.lowercase().trim()

        return when {
            cmd.contains("torch") || cmd.contains("flashlight") || cmd.contains("light") -> {
                if (cmd.contains("off") || cmd.contains("band")) {
                    toggleFlashlight(context, false)
                } else {
                    toggleFlashlight(context, true)
                }
            }
            cmd.startsWith("open ") || cmd.startsWith("launch ") || cmd.startsWith("kholo ") -> {
                val app = cmd.replace(Regex("^(open|launch|kholo)\\s+"), "").replace("app", "").trim()
                openApp(context, app)
            }
            cmd.startsWith("call ") || cmd.startsWith("dial ") || cmd.startsWith("phone lagao ") -> {
                val target = cmd.replace(Regex("^(call|dial|phone lagao)\\s+"), "").trim()
                makePhoneCall(context, target)
            }
            cmd.contains("whatsapp") -> {
                openApp(context, "whatsapp")
            }
            cmd.contains("youtube") -> {
                openApp(context, "youtube")
            }
            cmd.contains("camera") || cmd.contains("photo") -> {
                launchCamera(context, cmd.contains("front") || cmd.contains("selfie"))
            }
            cmd.contains("home") || cmd == "go home" || cmd == "ghar jao" -> {
                executeAccessibilityAction("home")
            }
            cmd.contains("back") || cmd == "piche jao" -> {
                executeAccessibilityAction("back")
            }
            cmd.contains("recents") || cmd.contains("recent apps") -> {
                executeAccessibilityAction("recents")
            }
            cmd.contains("scroll down") || cmd.contains("niche scroll") -> {
                executeAccessibilityAction("scroll_down")
            }
            cmd.contains("scroll up") || cmd.contains("upar scroll") -> {
                executeAccessibilityAction("scroll_up")
            }
            cmd.contains("notification") -> {
                readNotifications()
            }
            cmd.contains("wifi") || cmd.contains("wi-fi") -> {
                openSettings(context, "wifi")
            }
            cmd.contains("bluetooth") -> {
                openSettings(context, "bluetooth")
            }
            cmd.contains("vibrate") -> {
                vibratePhone(context)
            }
            cmd.startsWith("search ") || cmd.startsWith("google ") -> {
                val q = cmd.replace(Regex("^(search|google)\\s+"), "").trim()
                searchWeb(context, q)
            }
            else -> ToolExecutionResult(false, "unknown", "Command not recognized locally. Connecting to Boss AI brain...")
        }
    }
}
