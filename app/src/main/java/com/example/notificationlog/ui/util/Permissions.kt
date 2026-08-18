package com.example.notificationlog.ui.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** 「通知へのアクセス」がこのアプリに許可されているか */
fun isNotificationAccessGranted(context: Context): Boolean {
    return NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
}
