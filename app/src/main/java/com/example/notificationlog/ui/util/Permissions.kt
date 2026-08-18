package com.example.notificationlog.ui.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import com.example.notificationlog.selfcheck.LineWatchAccessibilityService

/** 「通知へのアクセス」がこのアプリに許可されているか */
fun isNotificationAccessGranted(context: Context): Boolean {
    return NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
}

/** 他アプリ上に表示（オーバーレイ）が許可されているか */
fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

/** セルフチェックのアクセシビリティサービスが有効か */
fun isSelfCheckAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${LineWatchAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expected, ignoreCase = true)) return true
    }
    return false
}
