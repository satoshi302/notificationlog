package com.example.notificationlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.MessageRepository
import com.example.notificationlog.data.db.ConversationSummary
import com.example.notificationlog.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val messageRepository: MessageRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val conversations: StateFlow<List<ConversationSummary>> =
        messageRepository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 対象アプリが1つ以上選択されているか（空状態の案内切り替え用） */
    val hasTargets: StateFlow<Boolean> =
        settingsRepository.targetPackages
            .map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    class Factory(private val app: App) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(app.messageRepository, app.settingsRepository) as T
    }
}
