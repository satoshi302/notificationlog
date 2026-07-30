package com.example.notificationlog.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.App
import com.example.notificationlog.data.db.MessageEntity
import com.example.notificationlog.ui.theme.ChatBackgroundDark
import com.example.notificationlog.ui.theme.ChatBackgroundLight
import com.example.notificationlog.ui.theme.IncomingBubbleDark
import com.example.notificationlog.ui.theme.IncomingBubbleLight
import com.example.notificationlog.ui.util.formatBubbleTime
import com.example.notificationlog.ui.util.formatDateSeparator
import com.example.notificationlog.ui.util.isDifferentDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    app: App,
    conversationKey: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(app, conversationKey)
    )
    val messages by vm.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 新着が来たら最下部へスクロール
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val chatBg = if (dark) ChatBackgroundDark else ChatBackgroundLight

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(chatBg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsWithSeparators(messages)
        }
    }
}

/** 日付が変わる箇所に区切りを挟みつつメッセージを描画 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsWithSeparators(
    messages: List<MessageEntity>
) {
    messages.forEachIndexed { index, msg ->
        val prev = messages.getOrNull(index - 1)
        val showDate = prev == null || isDifferentDay(prev.timestamp, msg.timestamp)
        // グループでは、直前と送信者が変わったら名前を表示
        val showSender = msg.isGroup && msg.sender != null &&
            (prev == null || prev.sender != msg.sender || showDate)

        if (showDate) {
            item(key = "date-$index") { DateSeparator(msg.timestamp) }
        }
        item(key = msg.id) { IncomingBubble(msg, showSender) }
    }
}

@Composable
private fun DateSeparator(timestamp: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ) {
            Text(
                text = formatDateSeparator(timestamp),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun IncomingBubble(msg: MessageEntity, showSender: Boolean) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val bubbleColor = if (dark) IncomingBubbleDark else IncomingBubbleLight

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showSender) {
            Text(
                text = msg.sender ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomEnd = 16.dp,
                    bottomStart = 16.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Text(
                text = formatBubbleTime(msg.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }
    }
}
