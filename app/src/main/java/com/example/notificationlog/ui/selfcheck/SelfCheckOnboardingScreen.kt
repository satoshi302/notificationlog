package com.example.notificationlog.ui.selfcheck

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationlog.App
import com.example.notificationlog.ui.util.canDrawOverlays
import com.example.notificationlog.ui.util.isSelfCheckAccessibilityEnabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfCheckOnboardingScreen(
    app: App,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val vm: SelfCheckSettingsViewModel = viewModel(factory = SelfCheckSettingsViewModel.Factory(app))
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val threshold by vm.threshold.collectAsStateWithLifecycle()

    var overlayOk by remember { mutableStateOf(canDrawOverlays(context)) }
    var a11yOk by remember { mutableStateOf(isSelfCheckAccessibilityEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                overlayOk = canDrawOverlays(context)
                a11yOk = isSelfCheckAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("送信前セルフチェック") },
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
                "LINEなどで返信を打っているとき、送る前に「相手に強く受け取られやすい表現」を" +
                    "画面上でそっと知らせます。文章の解析はすべて端末内で行い、外部には一切送信しません。",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            // 有効化トグル
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("セルフチェックを有効にする", fontWeight = FontWeight.Bold)
                        Text(
                            "オフの間は監視も警告もしません。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = { vm.setEnabled(it) })
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("警告を出す感度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                val options = listOf(1 to "中以上", 2 to "高以上", 3 to "危険のみ")
                options.forEach { (value, label) ->
                    FilterChip(
                        selected = threshold == value,
                        onClick = { vm.setThreshold(value) },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                "感度を上げるほど早めに警告します（低リスクは常に無視）。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text("必要な許可", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            PermissionRow(
                title = "① 他アプリの上に表示（オーバーレイ）",
                desc = "警告をLINEの画面の上に出すために必要です。",
                granted = overlayOk,
                buttonText = "オーバーレイ許可を開く",
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                title = "② アクセシビリティ（入力欄の読み取り）",
                desc = "入力中の文章を端末内で読むために必要です。「通知ログ セルフチェック」をオンにしてください。",
                granted = a11yOk,
                buttonText = "アクセシビリティ設定を開く",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("うまくオンにできないとき", fontWeight = FontWeight.Bold)
                    Text(
                        "Android 13 以降や Galaxy では、ストア外から入れたアプリの" +
                            "アクセシビリティが既定でブロックされます（トグルが押せない/すぐ戻る）。\n" +
                            "その場合は次の手順で解除してください:\n" +
                            "1. 下のボタンでアプリ情報を開く\n" +
                            "2. 右上「⋮」→「制限された設定を許可」をタップ\n" +
                            "3. もう一度、上の②アクセシビリティ設定でオンにする",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text("アプリ情報を開く")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Card {
                Text(
                    "プライバシー\n" +
                        "・入力欄の文章と会話はすべて端末内でのみ処理され、外部サーバーには送信されません。\n" +
                        "・オーバーレイ経路は精度より即時性を優先し、相手の直近発言を厳密には参照しません。" +
                        "より詳しい分析は、各トークの『セルフチェック』から行えます。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (granted) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "許可済み", tint = Color(0xFF43A047))
                }
            }
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick, enabled = !granted) {
                Text(if (granted) "許可済み" else buttonText)
            }
        }
    }
}
