package com.example.notificationlog.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notificationlog.App
import com.example.notificationlog.data.MessageRepository
import com.example.notificationlog.data.db.MessageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ChatViewModel(
    private val messageRepository: MessageRepository,
    private val conversationKey: String
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> =
        messageRepository.observeMessages(conversationKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(
        private val app: App,
        private val conversationKey: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(app.messageRepository, conversationKey) as T
    }
}
