package com.example.notificationlog

import android.app.Application
import com.example.notificationlog.data.InstalledAppsRepository
import com.example.notificationlog.data.MessageRepository
import com.example.notificationlog.data.db.AppDatabase
import com.example.notificationlog.data.prefs.SettingsRepository

/**
 * 軽量な手動 DI。Application が各 Repository を保持し、
 * ViewModel / Service から [App.from] 経由でアクセスする。
 */
class App : Application() {

    val messageRepository: MessageRepository by lazy {
        MessageRepository(AppDatabase.get(this).messageDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val installedAppsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepository(this)
    }

    companion object {
        fun from(context: android.content.Context): App =
            context.applicationContext as App
    }
}
