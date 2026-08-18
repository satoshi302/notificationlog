package com.example.notificationlog.data.db

/**
 * 会話一覧の1行分。messages を conversationKey でまとめた集計結果。
 */
data class ConversationSummary(
    val conversationKey: String,
    val conversationTitle: String,
    val packageName: String,
    val appLabel: String,
    val isGroup: Boolean,
    /** 最新メッセージ本文（プレビュー） */
    val lastText: String,
    /** 最新メッセージの送信者（グループ時に表示） */
    val lastSender: String?,
    /** 最新メッセージの時刻 */
    val lastTimestamp: Long,
    /** この会話のメッセージ件数 */
    val messageCount: Int
)
