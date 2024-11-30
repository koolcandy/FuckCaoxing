package com.example.fuckcaoxing

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessibleComponents : AccessibilityService() {
    private var problemList: MutableList<String> = ArrayList()
    private var job: Job? = null

    private var width: Int = 0
    private var height: Int = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        extractAllTextViews(rootInActiveWindow)
        Log.d("AccessibilityService", "Service Connected")
        val (w, h) = MainActivity.getScreenResolution(this)
        width = w
        height = h
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or AccessibilityEvent.TYPE_TOUCH_INTERACTION_END
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 500
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        this.serviceInfo = info
        startPeriodicTask()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("AccessibilityService", "Accessibility Event")
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Service Interrupted")
        job?.cancel()
    }

    private fun startPeriodicTask() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(10000)
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    for (i in 1..3) {
                        extractAllTextViews(rootNode) // get problem
                        delay(500)
                    }
                    Log.d("AccessibilityService", "Problem List: $problemList")
                    if (problemList.isEmpty()) {
                        Log.d("AccessibilityService", "左右横跳之术!!!")
                        clickAtCoordinates(
                            this@AccessibleComponents,
                            (width / 6).toFloat(),
                            (height - 100).toFloat()
                        )
                        delay(3000)
                        clickAtCoordinates(
                            this@AccessibleComponents,
                            (2 * width / 3).toFloat(),
                            (height - 100).toFloat()
                        )
                        continue
                    }
                    withContext(Dispatchers.IO) {
                        val answer = MainService.getAnswer(
                            problemList.joinToString(""),
                            this@AccessibleComponents
                        )
                        problemList.clear()
                        Log.d("AccessibilityService", "Answer: $answer")
                        answer.answer.forEach {
                            val position =
                                getPositionByText(this@AccessibleComponents, rootNode, it)
                            if (position != null) {
                                clickAtCoordinates(
                                    this@AccessibleComponents,
                                    position.first,
                                    position.second
                                )
                            }
                            delay(1000)
                        }
                        delay(5000)
                        clickAtCoordinates(
                            this@AccessibleComponents,
                            (2 * width / 3).toFloat(),
                            (height - 100).toFloat()
                        )
                        Log.d("AccessibilityService", "Problem List Cleared")
                    }
                } else {
                    Log.d("AccessibilityService", "Root node is null!")
                }
            }
        }
    }

    private fun getPositionByText(
        accessibilityService: AccessibilityService,
        node: AccessibilityNodeInfo?,
        answer: String
    ): Pair<Float, Float>? {
        if (node == null) return null

        val text = node.text?.toString()
        if (!text.isNullOrEmpty() && answer.contains(text)) {
            val rect = Rect()

            node.getBoundsInScreen(rect)

            val centerX = rect.exactCenterX()
            val centerY = rect.exactCenterY()

            return Pair(centerX, centerY)
        }

        for (i in 0 until node.childCount) {
            val result = getPositionByText(accessibilityService, node.getChild(i), answer)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun clickAtCoordinates(accessibilityService: AccessibilityService, x: Float, y: Float) {
        Log.d("AccessibilityService", "click: ($x, $y)")
        val builder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x, y)
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 1))
        val gesture = builder.build()
        accessibilityService.dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.d("AccessibilityService", "Gesture Cancelled")
            }

            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d("AccessibilityService", "Gesture Completed")
            }
        }, null)
    }

    private fun extractAllTextViews(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return

        for (i in 0 until rootNode.childCount) {
            val childNode = rootNode.getChild(i)

            if (childNode != null) {
                val className = childNode.className

                if (className == "android.widget.TextView" && !childNode.text.isNullOrEmpty()) {
                    val text = childNode.text.toString()

                    val elementsToRemove = listOf(
                        "当前第",
                        "下一题",
                        "上一题",
                        "离开考试",
                        "继续考试",
                        "下拉可以刷新",
                        "已经是最后一题了",
                        "下一步",
                        "上一步",
                        "交卷"
                    )
                    if (!elementsToRemove.any { element ->
                            text.contains(element) ||
                                    Regex("^.{2,4}\\s\\(\\d+\\)\$").containsMatchIn(text) ||
                                    Regex("\\s*（(\\d+(\\.\\d+)?)\\s*分）\\s*").containsMatchIn(text) ||
                                    text.isBlank()
                        }) {
                        if (!problemList.contains(text)) {
                            Log.d("AccessibilityService", "Adding to Problem List: $text")
                            problemList.add(text)
                        }
                    }
                }

                extractAllTextViews(childNode)
            }
        }
    }
}
