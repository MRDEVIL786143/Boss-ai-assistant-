package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentLinkedDeque

data class CapturedNotification(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BossNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val packageName = sbn.packageName ?: ""

        if (title.isBlank() && text.isBlank()) return
        if (packageName == applicationContext.packageName) return // Ignore own notifications

        val appName = try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }

        val captured = CapturedNotification(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        recentNotifications.addFirst(captured)
        while (recentNotifications.size > MAX_NOTIFICATIONS) {
            recentNotifications.removeLast()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val MAX_NOTIFICATIONS = 30
        private val recentNotifications = ConcurrentLinkedDeque<CapturedNotification>()
        var instance: BossNotificationService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null

        fun getRecentNotificationsList(): List<CapturedNotification> {
            return recentNotifications.toList()
        }

        fun getNotificationsSummary(): String {
            val list = recentNotifications.toList()
            if (list.isEmpty()) {
                return "No recent notifications received, Boss."
            }
            val sb = StringBuilder("Recent Notifications (${list.size}):\n")
            list.take(8).forEachIndexed { index, notif ->
                sb.append("${index + 1}. [${notif.appName}] ${notif.title}: ${notif.text}\n")
            }
            return sb.toString().trim()
        }

        fun clearNotifications() {
            recentNotifications.clear()
        }
    }
}
