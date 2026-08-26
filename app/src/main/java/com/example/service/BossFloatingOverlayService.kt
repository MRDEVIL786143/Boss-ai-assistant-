package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.BossApp
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.ChatMessageEntity
import com.example.engine.OfflineActionEngine
import com.example.engine.PhoneToolManager
import com.example.engine.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BossFloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootOverlayView: View? = null
    private var isExpanded = false
    private var isInputFocused = false

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    // UI View References in Floating Overlay
    private var statusTextView: TextView? = null
    private var responseTextView: TextView? = null
    private var promptEditText: EditText? = null
    private var chatCardView: View? = null
    private var menuDeck: LinearLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundServiceNotification("Zoya haazir hai! Hukum karein.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant Overlay Permission for Zoya's bubble", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        setupFloatingBubbleAndWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            handleNotificationAction(action)
        }
        return START_STICKY
    }

    private fun handleNotificationAction(action: String) {
        when (action) {
            ACTION_TOGGLE_TORCH -> {
                val res = PhoneToolManager.toggleFlashlight(this)
                updateStatusAndResponse("Torch Action", res.message)
                VoiceAssistantManager.getInstance(this).speak(res.message, "Urdu")
            }
            ACTION_VOICE_LISTEN -> {
                startVoiceListeningInOverlay()
            }
            ACTION_OPEN_CHAT -> {
                expandOverlay()
            }
        }
    }

    private fun startForegroundServiceNotification(statusMessage: String) {
        val channelId = "zoya_assistant_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Zoya AI Persistent Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Zoya background AI engine and floating bubble active"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Voice Command
        val voiceIntent = Intent(this, BossFloatingOverlayService::class.java).apply {
            action = ACTION_VOICE_LISTEN
        }
        val pVoice = PendingIntent.getService(
            this,
            1,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Quick Torch
        val torchIntent = Intent(this, BossFloatingOverlayService::class.java).apply {
            action = ACTION_TOGGLE_TORCH
        }
        val pTorch = PendingIntent.getService(
            this,
            2,
            torchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🌸 Zoya AI Assistant (زویا)")
            .setContentText(statusMessage)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.mipmap.ic_launcher, "🎙️ Bolain", pVoice)
            .addAction(R.mipmap.ic_launcher, "🔦 Torch", pTorch)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingBubbleAndWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dp = resources.displayMetrics.density

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (20 * dp).toInt()
            y = (200 * dp).toInt()
        }
        overlayParams = params

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // --- 1. Floating Circular Bubble ---
        val bubbleFrame = FrameLayout(this).apply {
            val size = (60 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    Color.parseColor("#1B122C"),
                    Color.parseColor("#120A1E")
                )
                setStroke((2.5f * dp).toInt(), Color.parseColor("#FF4081"))
            }
            elevation = 20f
        }

        val icon = ImageView(this).apply {
            val pad = (12 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = "Zoya AI Assistant"
        }
        bubbleFrame.addView(icon)

        // Live Online Indicator Dot
        val onlineDot = View(this).apply {
            val dotSize = (14 * dp).toInt()
            val lp = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, (4 * dp).toInt(), (4 * dp).toInt())
            }
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E676")) // Vibrant Emerald Green
                setStroke((1.5f * dp).toInt(), Color.parseColor("#120A1E"))
            }
        }
        bubbleFrame.addView(onlineDot)

        // --- 2. Expandable Mini Floating Chat & Command Window ---
        val deckWidth = (330 * dp).toInt()
        val deck = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val pad = (14 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * dp
                colors = intArrayOf(
                    Color.parseColor("#1E1433"),
                    Color.parseColor("#140D24")
                )
                setStroke((1.5f * dp).toInt(), Color.parseColor("#FF4081"))
            }
            layoutParams = LinearLayout.LayoutParams(deckWidth, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * dp).toInt()
            }
            elevation = 24f
        }
        menuDeck = deck

        // --- Header Row ---
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val avatarSmall = ImageView(this).apply {
            val sz = (28 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                setMargins(0, 0, (8 * dp).toInt(), 0)
            }
            setImageResource(R.mipmap.ic_launcher)
        }
        headerRow.addView(avatarSmall)

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleText = TextView(this).apply {
            text = "Zoya (زویا)"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitleText = TextView(this).apply {
            text = "Haazir & Obedient 🌸"
            setTextColor(Color.parseColor("#FF80AB"))
            textSize = 10f
        }
        titleCol.addView(titleText)
        titleCol.addView(subtitleText)
        headerRow.addView(titleCol)

        // Fullscreen Launcher Button
        val openAppBtn = createHeaderIconButton("📱") {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            collapseOverlay()
        }
        headerRow.addView(openAppBtn)

        // Minimize Button
        val closeBtn = createHeaderIconButton("✕") {
            collapseOverlay()
        }
        headerRow.addView(closeBtn)

        deck.addView(headerRow)

        // --- Live Status Banner ---
        val statusBanner = TextView(this).apply {
            text = "🌸 [Status: Ready] Aapka hukum sar ankhon par!"
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 11f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * dp
                setColor(Color.parseColor("#2A1C44"))
                setStroke((1f * dp).toInt(), Color.parseColor("#4A2E78"))
            }
            val padH = (8 * dp).toInt()
            val padV = (5 * dp).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (10 * dp).toInt()
                bottomMargin = (8 * dp).toInt()
            }
        }
        statusTextView = statusBanner
        deck.addView(statusBanner)

        // --- Recent Interaction / Advice Display Card ---
        val responseCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * dp
                setColor(Color.parseColor("#150E24"))
                setStroke((1f * dp).toInt(), Color.parseColor("#342352"))
            }
            val pad = (10 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }
        chatCardView = responseCard

        val responseScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (95 * dp).toInt()
            )
        }
        val responseText = TextView(this).apply {
            text = "Salam! Main Zoya hoon. Aap mujhe koi bhi hukum dein ya dil ki baat share karein, main hamesha aapke saath hoon. 🌸"
            setTextColor(Color.parseColor("#F3E5F5"))
            textSize = 12f
            setLineSpacing(3f * dp, 1f)
        }
        responseTextView = responseText
        responseScrollView.addView(responseText)
        responseCard.addView(responseScrollView)
        deck.addView(responseCard)

        // --- Quick Urdu Actions Scroll Bar ---
        val quickActionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }
        val quickActionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        quickActionsRow.addView(createQuickChip("🌸 Advice") { executeCommand("Mujhe ek pyara sa mashwara do") })
        quickActionsRow.addView(createQuickChip("🔦 Torch") { executeCommand("Torch toggle karo") })
        quickActionsRow.addView(createQuickChip("📞 Call") { executeCommand("Phone dialer kholo") })
        quickActionsRow.addView(createQuickChip("💬 WhatsApp") { executeCommand("WhatsApp open karo") })
        quickActionsRow.addView(createQuickChip("📜 Scroll") { executeCommand("Screen scroll down karo") })
        quickActionsRow.addView(createQuickChip("🏠 Home") { executeCommand("Home screen jao") })
        quickActionsRow.addView(createQuickChip("📷 Photo") { executeCommand("Camera kholo") })
        quickActionsRow.addView(createQuickChip("🔔 Notifs") { executeCommand("Notification summary batao") })

        quickActionScroll.addView(quickActionsRow)
        deck.addView(quickActionScroll)

        // --- Interactive Input Row (EditText + Mic + Send) ---
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val inputEdit = EditText(this).apply {
            hint = "Hukum karein, Aap..."
            setHintTextColor(Color.parseColor("#8E82A6"))
            setTextColor(Color.WHITE)
            textSize = 12f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            maxLines = 3
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * dp
                setColor(Color.parseColor("#25193E"))
                setStroke((1f * dp).toInt(), Color.parseColor("#442D6B"))
            }
            val padH = (12 * dp).toInt()
            val padV = (8 * dp).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            setOnFocusChangeListener { _, hasFocus ->
                isInputFocused = hasFocus
                updateOverlayFocus(hasFocus)
            }

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val text = text.toString().trim()
                    if (text.isNotEmpty()) {
                        executeCommand(text)
                        setText("")
                        hideKeyboard(this)
                    }
                    true
                } else false
            }
        }
        promptEditText = inputEdit
        inputRow.addView(inputEdit)

        // Mic Button
        val micBtn = TextView(this).apply {
            text = "🎙️"
            textSize = 16f
            gravity = Gravity.CENTER
            val sz = (38 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                setMargins((6 * dp).toInt(), 0, 0, 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2A1A46"))
                setStroke((1.2f * dp).toInt(), Color.parseColor("#B388FF"))
            }
            setOnClickListener {
                startVoiceListeningInOverlay()
            }
        }
        inputRow.addView(micBtn)

        // Send Button
        val sendBtn = TextView(this).apply {
            text = "➤"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val sz = (38 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                setMargins((6 * dp).toInt(), 0, 0, 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4081"))
            }
            setOnClickListener {
                val text = inputEdit.text.toString().trim()
                if (text.isNotEmpty()) {
                    executeCommand(text)
                    inputEdit.setText("")
                    hideKeyboard(inputEdit)
                }
            }
        }
        inputRow.addView(sendBtn)

        deck.addView(inputRow)

        // Assemble root view
        rootLayout.addView(bubbleFrame)
        rootLayout.addView(deck)

        // --- Drag & Touch Interaction ---
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDrag = false

        bubbleFrame.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                        isDrag = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDrag) {
                        toggleOverlay()
                    }
                    true
                }
                else -> false
            }
        }

        rootOverlayView = rootLayout
        windowManager?.addView(rootLayout, params)
    }

    private fun toggleOverlay() {
        if (isExpanded) {
            collapseOverlay()
        } else {
            expandOverlay()
        }
    }

    private fun expandOverlay() {
        isExpanded = true
        menuDeck?.visibility = View.VISIBLE
        updateOverlayFocus(false)
    }

    private fun collapseOverlay() {
        isExpanded = false
        menuDeck?.visibility = View.GONE
        promptEditText?.let { hideKeyboard(it) }
        updateOverlayFocus(false)
    }

    private fun updateOverlayFocus(isFocusable: Boolean) {
        val root = rootOverlayView ?: return
        val params = overlayParams ?: return
        if (isFocusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager?.updateViewLayout(root, params)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
        updateOverlayFocus(false)
    }

    private fun createHeaderIconButton(label: String, onClick: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        val sz = (28 * dp).toInt()
        return TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#B388FF"))
            textSize = 13f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                setMargins((4 * dp).toInt(), 0, 0, 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#26193E"))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createQuickChip(label: String, onClick: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#FFD1E3"))
            textSize = 11f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#25173B"))
                setStroke((1f * dp).toInt(), Color.parseColor("#442866"))
            }
            val padH = (10 * dp).toInt()
            val padV = (5 * dp).toInt()
            setPadding(padH, padV, padH, padV)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, (6 * dp).toInt(), 0)
            }
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun startVoiceListeningInOverlay() {
        if (!isExpanded) {
            expandOverlay()
        }
        statusTextView?.text = "🎙️ [Status: Listening] Zoya sun rahi hai..."
        startForegroundServiceNotification("🎙️ Listening for your voice command...")

        val voiceManager = VoiceAssistantManager.getInstance(this)
        voiceManager.startListening(
            onResult = { recognized ->
                serviceScope.launch {
                    statusTextView?.text = "🌸 [Status: Processing] \"$recognized\""
                    executeCommand(recognized)
                }
            },
            onError = { err ->
                serviceScope.launch {
                    statusTextView?.text = "🌸 [Status: Ready] $err"
                }
            }
        )
    }

    fun executeCommand(command: String) {
        if (!isExpanded) {
            expandOverlay()
        }

        statusTextView?.text = "🌸 [Status: Executing] Zoya kaam shuru kar rahi hai..."
        responseTextView?.text = "Command: \"$command\"\n\nZoya sooch rahi hai... 🌸"
        startForegroundServiceNotification("Executing: $command")

        serviceScope.launch {
            val app = BossApp.instance
            val settings = app.settingsRepository.settings.value
            val chatRepo = app.chatRepository

            // Save user query to local database
            chatRepo.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = command,
                    status = "done"
                )
            )

            // Execute via OpenAI or Offline Action Engine
            val response = if (settings.apiKey.isNotBlank()) {
                val recentHistory = chatRepo.getRecentMessages(6).map { it.role to it.content }
                app.openAIClient.sendMessage(
                    userMessage = command,
                    settings = settings,
                    conversationHistory = recentHistory,
                    onToolExecuting = { toolName, _ ->
                        statusTextView?.text = "⚡ [Status: Executing $toolName] Zoya kar rahi hai..."
                    },
                    onToolFinished = { toolName, resMsg ->
                        statusTextView?.text = "✅ [Status: Done] $resMsg"
                    }
                )
            } else {
                OfflineActionEngine.processCommand(
                    context = this@BossFloatingOverlayService,
                    input = command,
                    honorific = settings.honorific,
                    language = settings.language
                )
            }

            // Save assistant reply to database
            chatRepo.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = response.replyText,
                    toolName = response.toolName,
                    toolArgs = response.toolArgs,
                    toolResult = response.toolResult,
                    status = "done"
                )
            )

            // Update UI & Speech
            statusTextView?.text = "🌸 [Status: Done] Task mukammal ho gaya!"
            responseTextView?.text = response.replyText

            if (settings.ttsEnabled) {
                VoiceAssistantManager.getInstance(this@BossFloatingOverlayService).speak(
                    text = response.replyText,
                    language = settings.language
                )
            }

            startForegroundServiceNotification(
                response.replyText.take(60) + if (response.replyText.length > 60) "..." else ""
            )
        }
    }

    private fun updateStatusAndResponse(status: String, text: String) {
        statusTextView?.text = "🌸 [Status: $status]"
        responseTextView?.text = text
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep foreground service and notification active
        startForegroundServiceNotification("Zoya background me haazir hai 🌸")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rootOverlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(rootOverlayView)
            } catch (e: Exception) {
                // Ignore view cleanup errors on destroy
            }
            rootOverlayView = null
        }
        isRunning = false
    }

    companion object {
        const val NOTIFICATION_ID = 9981
        const val ACTION_TOGGLE_TORCH = "com.example.service.ACTION_TOGGLE_TORCH"
        const val ACTION_VOICE_LISTEN = "com.example.service.ACTION_VOICE_LISTEN"
        const val ACTION_OPEN_CHAT = "com.example.service.ACTION_OPEN_CHAT"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, BossFloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BossFloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }
}
