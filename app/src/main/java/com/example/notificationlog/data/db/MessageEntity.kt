package com.example.notificationlog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1件の受信メッセージ（通知から抽出したもの）。
 *
 * メッセージ系アプリは会話全体を毎回通知として再投稿するため、
 * (packageName, conversationKey, sender, text, timestamp) のユニークインデックスで
 * 同一メッセージの二重記録を防ぐ。
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(
            value = ["packageName", "conversationKey", "sender", "text", "timestamp"],
            unique = true
        ),
        Index(value = ["conversationKey"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 通知を出したアプリのパッケージ名（例: jp.naver.line.android） */
    val packageName: String,

    /** アプリ表示名（例: LINE） */
    val appLabel: String,

    /** 会話（スレッド）を束ねるキー */
    val conversationKey: String,

    /** 会話の表示名（連絡先名 or グループ名） */
    val conversationTitle: String,

    /** グループ会話か */
    val isGroup: Boolean,

    /** 送信者名（グループ時。1:1では会話名と同じか null） */
    val sender: String?,

    /** メッセージ本文 */
    val text: String,

    /** メッセージのタイムスタンプ（epoch millis） */
    val timestamp: Long
)
