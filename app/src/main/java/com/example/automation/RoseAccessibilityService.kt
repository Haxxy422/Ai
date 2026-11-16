package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Optional, user-authorized AccessibilityService for safe screen automation.
 * Performs actions only upon explicit user voice/text command.
 */
class RoseAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: RoseAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        fun getInstance(): RoseAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive: Rose never silently inspects or transmits screen content.
        // Screen nodes are queried strictly when the user issues an explicit command.
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        instance = null
        _isConnected.value = false
        super.onDestroy()
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun performScroll(scrollDown: Boolean): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findScrollableNode(root)
            if (scrollable != null) {
                val action = if (scrollDown) {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                if (scrollable.performAction(action)) {
                    return true
                }
            }
        }
        // Fallback: dispatch vertical swipe gesture using display metrics
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val startY = if (scrollDown) metrics.heightPixels * 0.72f else metrics.heightPixels * 0.28f
        val endY = if (scrollDown) metrics.heightPixels * 0.28f else metrics.heightPixels * 0.72f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 280L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun clickNodeByQuery(rawQuery: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val query = rawQuery.trim().lowercase()
        if (query.isBlank()) return false

        // Special friendly targets
        val candidates = when {
            query.contains("first video") || query.contains("first result") -> {
                collectClickableNodes(root).drop(1)
            }
            query.contains("search") -> {
                findNodesMatching(root, listOf("search", "খুঁজুন", "खोजें"))
            }
            query.contains("menu") -> {
                findNodesMatching(root, listOf("menu", "more options", "navigation", "মেনু"))
            }
            else -> {
                findNodesMatching(root, listOf(query))
            }
        }

        for (node in candidates) {
            var target: AccessibilityNodeInfo? = node
            while (target != null) {
                if (target.isClickable && target.isEnabled) {
                    if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                }
                target = target.parent
            }
        }
        return false
    }

    fun typeText(text: String): Boolean {
        if (text.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val target = if (focused != null && focused.isEditable) {
            focused
        } else {
            findFirstEditableNode(root)
        } ?: return false

        target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun findNodesMatching(
        node: AccessibilityNodeInfo,
        terms: List<String>
    ): List<AccessibilityNodeInfo> {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(current: AccessibilityNodeInfo) {
            val text = current.text?.toString()?.lowercase().orEmpty()
            val desc = current.contentDescription?.toString()?.lowercase().orEmpty()
            val viewId = current.viewIdResourceName?.lowercase().orEmpty()
            if (terms.any { term -> text.contains(term) || desc.contains(term) || viewId.contains(term) }) {
                matches.add(current)
            }
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { traverse(it) }
            }
        }
        traverse(node)
        return matches
    }

    private fun collectClickableNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(current: AccessibilityNodeInfo) {
            if (current.isClickable && current.isVisibleToUser) {
                list.add(current)
            }
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { traverse(it) }
            }
        }
        traverse(node)
        return list
    }
}
