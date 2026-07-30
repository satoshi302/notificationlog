package com.example.notificationlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /** 重複（ユニークインデックス衝突）は無視して挿入。挿入された行ID、無視時は -1 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    /**
     * 会話一覧。conversationKey ごとに最新メッセージと件数を集計。
     * window 関数を使わず（minSdk 26 の SQLite 互換のため）、
     * 各会話の MAX(timestamp) を持つ行を join して1行に畳む。
     */
    @Query(
        """
        SELECT m.conversationKey AS conversationKey,
               m.conversationTitle AS conversationTitle,
               m.packageName AS packageName,
               m.appLabel AS appLabel,
               m.isGroup AS isGroup,
               m.text AS lastText,
               m.sender AS lastSender,
               m.timestamp AS lastTimestamp,
               agg.messageCount AS messageCount
        FROM messages m
        INNER JOIN (
            SELECT conversationKey, MAX(timestamp) AS maxTs, COUNT(*) AS messageCount
            FROM messages
            GROUP BY conversationKey
        ) agg
          ON m.conversationKey = agg.conversationKey AND m.timestamp = agg.maxTs
        GROUP BY m.conversationKey
        ORDER BY lastTimestamp DESC
        """
    )
    fun observeConversations(): Flow<List<ConversationSummary>>

    /** 指定会話のメッセージを時系列（昇順）で監視 */
    @Query(
        """
        SELECT * FROM messages
        WHERE conversationKey = :conversationKey
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun observeMessages(conversationKey: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("DELETE FROM messages WHERE conversationKey = :conversationKey")
    suspend fun deleteConversation(conversationKey: String)
}
