package com.example.notificationlog.ui.selfcheck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfCheckScreen(
    app: App,
    conversationKey: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: SelfCheckViewModel = viewModel(
        factory = SelfCheckViewModel.Factory(app, conversationKey)
    )
    val draft by vm.draft.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (title.isBlank()) "セルフチェック" else "セルフチェック: $title",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "送る前の文を貼り付け（または入力）してチェックします。" +
                    if (conversationKey.isNotBlank()) "この会話の直近のやり取りも考慮します。" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = vm::onDraftChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("送ろうとしている返信") },
                minLines = 3
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = vm::check,
                enabled = draft.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("チェックする")
            }

            if (loading) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }

            result?.let {
                Spacer(Modifier.height(20.dp))
                SelfCheckResultCard(it)
            }
        }
    }
}
