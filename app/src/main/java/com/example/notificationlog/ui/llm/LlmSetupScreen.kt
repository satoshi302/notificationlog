package com.example.notificationlog.ui.llm

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSetupScreen(
    app: App,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: LlmSetupViewModel = viewModel(factory = LlmSetupViewModel.Factory(app))
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("AIモデル（オンデバイス）") },
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
                "手動の「AIで詳しく分析」で使うオンデバイスLLM（Gemma 4 E2B / Apache-2.0）です。" +
                    "モデルは端末内にのみ保存され、分析も端末内で行います（外部送信なし）。",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (s.downloaded) "状態: ダウンロード済み" else "状態: 未ダウンロード",
                        fontWeight = FontWeight.Bold
                    )
                    if (s.downloaded && s.sizeBytes > 0) {
                        Text(
                            "サイズ: %.0f MB".format(s.sizeBytes / 1_000_000.0),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (s.downloading) {
                        Spacer(Modifier.height(12.dp))
                        if (s.totalBytes > 0) {
                            val p = (s.downloadedBytes.toFloat() / s.totalBytes.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { p },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "%.0f / %.0f MB".format(
                                    s.downloadedBytes / 1_000_000.0,
                                    s.totalBytes / 1_000_000.0
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "%.0f MB ダウンロード中…".format(s.downloadedBytes / 1_000_000.0),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    s.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "エラー: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    if (!s.downloaded) {
                        Button(
                            onClick = { vm.download() },
                            enabled = !s.downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (s.downloading) "ダウンロード中…" else "モデルをダウンロード（数百MB）")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { vm.delete() },
                            enabled = !s.downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("モデルを削除")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("詳細設定", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = s.url,
                onValueChange = vm::setUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("モデル(.task)のURL") },
                singleLine = true,
                enabled = !s.downloading
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = s.token,
                onValueChange = vm::setToken,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hugging Face トークン（gatedリポジトリ用・任意）") },
                singleLine = true,
                enabled = !s.downloading
            )

            Spacer(Modifier.height(16.dp))
            Card {
                Text(
                    "メモ\n" +
                        "・Gemma 4 は Apache-2.0 ですが、公式リポジトリは規約同意付き(gated)の場合があります。" +
                        "その時はトークンを入れるか、ungated のミラーURLに変更してください。\n" +
                        "・動作にはある程度のRAM（おおむね2GB前後の空き）と、比較的新しめの端末が必要です。\n" +
                        "・分析は端末内で完結し、外部に送信されません。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
