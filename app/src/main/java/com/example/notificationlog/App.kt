package com.example.notificationlog

import android.app.Application
import com.example.notificationlog.data.InstalledAppsRepository
import com.example.notificationlog.data.MessageRepository
import com.example.notificationlog.data.SelfCheckRepository
import com.example.notificationlog.data.db.AppDatabase
import com.example.notificationlog.data.prefs.SettingsRepository
import com.example.notificationlog.llm.GemmaLlmAdvisor
import com.example.notificationlog.llm.LlmAdvisor
import com.example.notificationlog.llm.LlmModelManager
import com.example.notificationlog.selfcheck.HeuristicAnalyzer
import com.example.notificationlog.selfcheck.SelfCheckAnalyzer

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

    val selfCheckAnalyzer: SelfCheckAnalyzer by lazy { HeuristicAnalyzer() }

    val selfCheckRepository: SelfCheckRepository by lazy {
        val db = AppDatabase.get(this)
        SelfCheckRepository(db.messageDao(), db.selfCheckDao(), selfCheckAnalyzer)
    }

    val llmModelManager: LlmModelManager by lazy { LlmModelManager(this) }

    val llmAdvisor: LlmAdvisor by lazy { GemmaLlmAdvisor(this, llmModelManager) }

    companion object {
        fun from(context: android.content.Context): App =
            context.applicationContext as App
    }
}
