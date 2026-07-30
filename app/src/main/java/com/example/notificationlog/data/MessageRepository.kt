package com.example.notificationlog.data

import com.example.notificationlog.data.db.ConversationSummary
import com.example.notificationlog.data.db.MessageDao
import com.example.notificationlog.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val dao: MessageDao) {

    fun observeConversations(): Flow<List<ConversationSummary>> = dao.observeConversations()

    fun observeMessages(conversationKey: String): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationKey)

    suspend fun insert(message: MessageEntity): Boolean = dao.insert(message) != -1L

    suspend fun clearAll() = dao.clearAll()

    suspend fun deleteConversation(conversationKey: String) = dao.deleteConversation(conversationKey)
}
