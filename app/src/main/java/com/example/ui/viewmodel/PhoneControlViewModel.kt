package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BossApp
import com.example.engine.PhoneToolManager
import com.example.service.BossAccessibilityService
import com.example.service.BossFloatingOverlayService
import com.example.service.BossNotificationService
import com.example.service.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhoneControlViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val app = application as BossApp
    private val settingsRepo = app.settingsRepository

    private val _lastActionResult = MutableStateFlow<String?>(null)
    val lastActionResult: StateFlow<String?> = _lastActionResult.asStateFlow()

    private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

    val isAccessibilityEnabled: Boolean
        get() = BossAccessibilityService.isServiceRunning

    val isFloatingBubbleActive: Boolean
        get() = BossFloatingOverlayService.isRunning

    fun refreshNotifications() {
        _notifications.value = BossNotificationService.getRecentNotificationsList()
    }

    fun makeCall(number: String) {
        val res = PhoneToolManager.makePhoneCall(context, number)
        _lastActionResult.value = res.message
    }

    fun sendSms(number: String, message: String) {
        val res = PhoneToolManager.sendSms(context, number, message)
        _lastActionResult.value = res.message
    }

    fun sendWhatsApp(number: String, message: String) {
        val res = PhoneToolManager.sendWhatsAppMessage(context, number, message)
        _lastActionResult.value = res.message
    }

    fun openApp(appName: String) {
        val res = PhoneToolManager.openApp(context, appName)
        _lastActionResult.value = res.message
    }

    fun toggleFlashlight(on: Boolean) {
        val res = PhoneToolManager.toggleFlashlight(context, on)
        _lastActionResult.value = res.message
    }

    fun setVolume(percent: Int) {
        val res = PhoneToolManager.setVolume(context, percent)
        _lastActionResult.value = res.message
    }

    fun launchCamera(front: Boolean) {
        val res = PhoneToolManager.launchCamera(context, front)
        _lastActionResult.value = res.message
    }

    fun openSettings(type: String) {
        val res = PhoneToolManager.openSettings(context, type)
        _lastActionResult.value = res.message
    }

    fun executeGesture(action: String, arg1: String = "") {
        val res = PhoneToolManager.executeAccessibilityAction(action, arg1)
        _lastActionResult.value = res.message
    }

    fun toggleFloatingBubble(enable: Boolean) {
        settingsRepo.updateFloatingBubbleEnabled(enable)
        if (enable) {
            BossFloatingOverlayService.start(context)
            _lastActionResult.value = "Floating Assistant Bubble activated on screen, Boss."
        } else {
            BossFloatingOverlayService.stop(context)
            _lastActionResult.value = "Floating Bubble disabled, Boss."
        }
    }
}
