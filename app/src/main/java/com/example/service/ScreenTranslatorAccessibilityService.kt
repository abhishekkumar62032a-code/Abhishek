package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenTranslatorAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.d("AccessibilityService", "Screen Translator Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are monitored to keep node tree responsive
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    fun readCurrentScreenText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()
        traverseNodes(rootNode, textBuilder)
        return textBuilder.toString().trim()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return

        // Respect privacy: never read password fields
        if (node.isPassword) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            if (builder.isNotEmpty()) builder.append("\n")
            builder.append(text)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            traverseNodes(child, builder)
            child?.recycle()
        }
    }

    companion object {
        var instance: ScreenTranslatorAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun captureActiveScreenText(): String {
            return instance?.readCurrentScreenText() ?: ""
        }
    }
}
