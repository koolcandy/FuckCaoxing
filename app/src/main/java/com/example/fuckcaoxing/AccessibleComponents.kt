package com.example.fuckcaoxing

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*

class AccessibleComponents : AccessibilityService() {
    private var lastPageIdentifier: String? = null
    private var problemList: MutableList<String> = ArrayList()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 500
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        this.serviceInfo = info
        Log.d("AccessibilityService", "Service Connected")

        CoroutineScope(Dispatchers.Main).launch {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                traverseNode(rootNode)
                Log.d("AccessibilityService", "Problem List: $problemList")
                withContext(Dispatchers.IO) {
                    MainService.getans(problemList.toString(), this@AccessibleComponents)
                }
                problemList.clear() // clear problem list
            } else {
                Log.d("AccessibilityService", "Root node is null!")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            Log.d("AccessibilityService", "Foreground app package name: $packageName")
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Service Interrupted")
    }

    private fun traverseNode(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val text = node.text?.toString()
        if (!text.isNullOrEmpty()) {
            Log.d("AccessibilityService", "Text Found: $text")
            val elementsToRemove = listOf(
                "当前第", "下一题", "上一题", "离开考试", "继续考试", "下拉可以刷新", "已经是最后一题了", "下一步", "上一步", "交卷"
            )
            if (!elementsToRemove.any { element ->
                    text.contains(element) ||
                            Regex("^.{2,4}\\s\\(\\d+\\)\$").containsMatchIn(text) ||
                            Regex("\\s*（(\\d+(\\.\\d+)?)\\s*分）\\s*").containsMatchIn(text) ||
                            text.isBlank()
                }) {
                if (!problemList.contains(text)) {
                    problemList.add(text)
                }
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i))
        }
    }
}