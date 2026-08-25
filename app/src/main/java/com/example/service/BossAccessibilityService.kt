package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BossAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "BossAccessibilityService Connected and Ready for Boss's commands")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event monitoring if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "BossAccessibilityService Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    // --- Action Methods Executable by MyBossAI ---

    fun performGlobal(actionId: Int): Boolean {
        return performGlobalAction(actionId)
    }

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun clickCoordinates(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null): Boolean {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                callback?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                callback?.invoke(false)
            }
        }, null)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        val swipePath = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(swipePath, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollableNode(root)
        val result = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
        scrollable?.recycle()
        root.recycle()
        return result
    }

    fun scrollBackward(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollableNode(root)
        val result = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
        scrollable?.recycle()
        root.recycle()
        return result
    }

    fun clickNodeByText(text: String, ignoreCase: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (node.isClickable) {
                    val res = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                    root.recycle()
                    return res
                } else {
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val res = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            parent.recycle()
                            node.recycle()
                            root.recycle()
                            return res
                        }
                        parent = parent.parent
                    }
                }
                node.recycle()
            }
        }
        root.recycle()
        return false
    }

    fun inputText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (focused != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val res = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focused.recycle()
            root.recycle()
            return res
        }
        root.recycle()
        return false
    }

    fun readScreenText(): String {
        val root = rootInActiveWindow ?: return "Screen content unavailable"
        val sb = StringBuilder()
        traverseNodeText(root, sb)
        root.recycle()
        return if (sb.isNotEmpty()) sb.toString().trim() else "No visible text detected on screen."
    }

    private fun traverseNodeText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrBlank()) {
            sb.append(text).append("\n")
        } else if (!desc.isNullOrBlank()) {
            sb.append("[").append(desc).append("]\n")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNodeText(child, sb)
            child?.recycle()
        }
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findFirstScrollableNode(child)
            if (result != null) return result
            child?.recycle()
        }
        return null
    }

    companion object {
        private const val TAG = "BossAccessibility"
        var instance: BossAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }
}
