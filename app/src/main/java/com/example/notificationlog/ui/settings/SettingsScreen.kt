package com.example.notificationlog.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.App
import com.example.notificationlog.ui.util.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: App,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(app))
    val apps by vm.installedApps.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val targets by vm.targetPackages.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SectionHeader("記録するアプリ")
                Text(
                    text = "選んだアプリの通知だけを記録します。LINE などメッセージアプリをオンにしてください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            items(apps, key = { it.packageName }) { installed ->
                val checked = installed.packageName in targets
                ListItem(
                    leadingContent = { AppIcon(packageName = installed.packageName, size = 40.dp) },
                    headlineContent = { Text(installed.label) },
                    supportingContent = {
                        Text(
                            installed.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = checked,
                            onCheckedChange = { vm.setEnabled(installed.packageName, it) }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), thickness = 0.5.dp)
            }

            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader("データ")
                ListItem(
                    headlineContent = { Text("記録した履歴をすべて削除") },
                    modifier = Modifier.clickableRow { showClearDialog = true }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("履歴を削除しますか？") },
            text = { Text("記録したすべてのメッセージが削除されます。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    showClearDialog = false
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
