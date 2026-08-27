package com.userapp.applock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class LaunchBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        val blockedList = PrefsManager.getBlockedPackages(this)
        if (!blockedList.contains(pkg)) return
        if (PrefsManager.isAllowed(this, pkg)) return

        // 즉시 홈으로 이동시켜 앱이 화면에 뜨지 못하게 막음
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, OverlayCountdownService::class.java)
        intent.putExtra("target_package", pkg)
        startService(intent)
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT
        serviceInfo = info
    }
}
