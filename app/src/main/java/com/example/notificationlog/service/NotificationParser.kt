package com.example.notificationlog.service

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.notificationlog.data.db.MessageEntity

/**
 * 通知から表示用のメッセージ群を抽出する。
 *
 * LINE などのメッセージ系アプリは MessagingStyle を使うため、まずそれを解析して
 * 送信者・本文・時刻つきの個別メッセージを取り出す。取れない場合は
 * タイトル/本文からの単純抽出にフォールバックする。
 */
object NotificationParser {

    fun parse(sbn: StatusBarNotification, appLabel: String): List<MessageEntity> {
        val notification = sbn.notification ?: return emptyList()
        val extras = notification.extras ?: return emptyList()
        val packageName = sbn.packageName

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()

        val messagingStyle =
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        return if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            parseMessagingStyle(packageName, appLabel, title, notification, messagingStyle)
        } else {
            parseFallback(packageName, appLabel, title, notification, sbn.postTime)
        }
    }

    private fun parseMessagingStyle(
        packageName: String,
        appLabel: String,
        title: String?,
        notification: Notification,
        style: NotificationCompat.MessagingStyle
    ): List<MessageEntity> {
        val isGroup = style.isGroupConversation
        val groupName = style.conversationTitle?.toString()?.trim()

        // 1:1 の会話名は最新メッセージの送信者名を採用する（conversationTitle は null のことが多い）
        val lastPersonName = style.messages.lastOrNull()?.person?.name?.toString()?.trim()
        val displayTitle = when {
            isGroup -> groupName ?: title ?: lastPersonName ?: packageName
            else -> lastPersonName ?: title ?: packageName
        }

        val keyBase = notification.shortcutId ?: groupName ?: displayTitle
        val conversationKey = "$packageName|$keyBase"

        return style.messages.mapNotNull { msg ->
            val text = msg.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null
            val sender = msg.person?.name?.toString()?.trim()
            MessageEntity(
                packageName = packageName,
                appLabel = appLabel,
                conversationKey = conversationKey,
                conversationTitle = displayTitle,
                isGroup = isGroup,
                sender = sender,
                text = text,
                timestamp = if (msg.timestamp > 0) msg.timestamp else notification.`when`
            )
        }
    }

    private fun parseFallback(
        packageName: String,
        appLabel: String,
        title: String?,
        notification: Notification,
        postTime: Long
    ): List<MessageEntity> {
        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.lastOrNull()?.toString()?.trim()

        if (text.isNullOrEmpty()) return emptyList()

        val displayTitle = title ?: packageName
        val keyBase = notification.shortcutId ?: displayTitle
        val conversationKey = "$packageName|$keyBase"
        val timestamp = if (notification.`when` > 0) notification.`when` else postTime

        return listOf(
            MessageEntity(
                packageName = packageName,
                appLabel = appLabel,
                conversationKey = conversationKey,
                conversationTitle = displayTitle,
                isGroup = false,
                sender = null,
                text = text,
                timestamp = timestamp
            )
        )
    }
}
