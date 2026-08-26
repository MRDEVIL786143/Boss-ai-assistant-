package com.example

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.service.BossFloatingOverlayService
import com.example.ui.screens.MainScaffold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ZoyaDarkBg

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle back press to minimize app rather than terminate background AI
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }

        // Auto-start persistent floating overlay service if overlay permission is granted
        ensureBackgroundService()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ZoyaDarkBg
                ) {
                    MainScaffold()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ensureBackgroundService()
    }

    private fun ensureBackgroundService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            if (!BossFloatingOverlayService.isRunning) {
                BossFloatingOverlayService.start(this)
            }
        }
    }
}
