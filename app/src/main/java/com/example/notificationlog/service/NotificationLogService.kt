package com.example.notificationlog.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationlog.App
import com.example.notificationlog.data.InstalledAppsRepository
import com.example.notificationlog.data.MessageRepository
import com.example.notificationlog.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 通知を受信して記録するリスナー。
 *
 * ユーザーが「通知へのアクセス」を許可すると OS がこのサービスをバインドし、
 * 以降に投稿された通知が [onNotificationPosted] に届く。
 * 対象パッケージ（設定で選択）だけを保存する。
 */
class NotificationLogService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var messageRepository: MessageRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var installedApps: InstalledAppsRepository

    override fun onCreate() {
        super.onCreate()
        val app = App.from(this)
        messageRepository = app.messageRepository
        settingsRepository = app.settingsRepository
        installedApps = app.installedAppsRepository
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return

        // グループサマリや常駐（進行中）通知は本文を持たないため除外
        val flags = notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val packageName = sbn.packageName

        scope.launch {
            val targets = settingsRepository.currentTargetPackages()
            if (packageName !in targets) return@launch

            val appLabel = installedApps.labelFor(packageName)
            val messages = NotificationParser.parse(sbn, appLabel)
            messages.forEach { messageRepository.insert(it) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
