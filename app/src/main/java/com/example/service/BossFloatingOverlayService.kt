package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.engine.PhoneToolManager
import com.example.engine.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BossFloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = false
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required, Boss!", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }
        setupFloatingBubble()
        isRunning = true
    }

    private fun startForegroundServiceNotification() {
        val channelId = "boss_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MyBossAI Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps MyBossAI floating bubble active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyBossAI Assistant Active")
            .setContentText("At your service, Boss.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

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
            x = 50
            y = 300
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Circular Floating Bubble
        val bubble = FrameLayout(this).apply {
            val size = (56 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0A0F1D"))
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#00E5FF"))
            }
            elevation = 16f
        }

        val icon = ImageView(this).apply {
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setImageResource(R.mipmap.ic_launcher)
        }
        bubble.addView(icon)

        // Expanded Control Deck
        val menuDeck = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.parseColor("#111827"))
                setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#00E5FF"))
            }
            val paramsDeck = LinearLayout.LayoutParams(
                (220 * resources.displayMetrics.density).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
            layoutParams = paramsDeck
        }

        val title = TextView(this).apply {
            text = "MyBossAI Quick Deck"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, pad)
        }
        menuDeck.addView(title)

        // Button row 1: Home & Back
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row1.addView(createDeckButton("🏠 Home") {
            PhoneToolManager.executeLocalAction(this@BossFloatingOverlayService, "home")
        })
        row1.addView(createDeckButton("◀ Back") {
            PhoneToolManager.executeLocalAction(this@BossFloatingOverlayService, "back")
        })
        menuDeck.addView(row1)

        // Button row 2: Flashlight & Scroll
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row2.addView(createDeckButton("🔦 Torch") {
            PhoneToolManager.executeLocalAction(this@BossFloatingOverlayService, "toggle flashlight")
        })
        row2.addView(createDeckButton("📜 Scroll") {
            PhoneToolManager.executeLocalAction(this@BossFloatingOverlayService, "scroll down")
        })
        menuDeck.addView(row2)

        // Button row 3: Open App & Voice
        val row3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row3.addView(createDeckButton("💬 WhatsApp") {
            PhoneToolManager.executeLocalAction(this@BossFloatingOverlayService, "open whatsapp")
        })
        row3.addView(createDeckButton("🎙️ Listen") {
            VoiceAssistantManager.getInstance(this@BossFloatingOverlayService).speak("Ready for your command, Boss!")
        })
        menuDeck.addView(row3)

        rootLayout.addView(bubble)
        rootLayout.addView(menuDeck)

        // Drag & Click Logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        isExpanded = !isExpanded
                        menuDeck.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = rootLayout
        windowManager?.addView(rootLayout, params)
    }

    private fun createDeckButton(label: String, onClick: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * dp
                setColor(Color.parseColor("#1F293D"))
            }
            val lp = LinearLayout.LayoutParams(
                (95 * dp).toInt(),
                (36 * dp).toInt()
            ).apply {
                setMargins((4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt())
            }
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null && windowManager != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
        isRunning = false
    }

    companion object {
        private const val NOTIFICATION_ID = 9981
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
