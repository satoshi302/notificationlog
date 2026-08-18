package com.example.notificationlog.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.App
import com.example.notificationlog.data.db.ConversationSummary
import com.example.notificationlog.ui.util.AppIcon
import com.example.notificationlog.ui.util.formatListTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    app: App,
    onOpenConversation: (ConversationSummary) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSelfCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app))
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val hasTargets by vm.hasTargets.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("トーク") },
                actions = {
                    IconButton(onClick = onOpenSelfCheck) {
                        Icon(Icons.Filled.Psychology, contentDescription = "セルフチェック")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(
                hasTargets = hasTargets,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(conversations, key = { it.conversationKey }) { convo ->
                    ConversationRow(convo, onClick = { onOpenConversation(convo) })
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    convo: ConversationSummary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = convo.packageName)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = convo.conversationTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatListTime(convo.lastTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            val preview = if (convo.isGroup && convo.lastSender != null) {
                "${convo.lastSender}: ${convo.lastText}"
            } else {
                convo.lastText
            }
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState(
    hasTargets: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasTargets) {
            Text(
                text = "記録するアプリが選ばれていません",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "右上の設定から、LINE など記録したいメッセージアプリを選んでください。",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = onOpenSettings) {
                Text("設定を開く")
            }
        } else {
            Text(
                text = "まだ通知がありません",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "対象アプリに新しいメッセージ通知が届くと、ここに会話が表示されます。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
