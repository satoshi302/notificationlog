package com.example.notificationlog.ui.selfcheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notificationlog.selfcheck.RiskLevel
import com.example.notificationlog.selfcheck.SelfCheckResult

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.LOW -> Color(0xFF43A047)
    RiskLevel.MEDIUM -> Color(0xFFF9A825)
    RiskLevel.HIGH -> Color(0xFFFB8C00)
    RiskLevel.CRITICAL -> Color(0xFFE53935)
}

@Composable
fun SelfCheckResultCard(result: SelfCheckResult, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    Column(modifier = modifier.fillMaxWidth()) {

        // 危険度バッジ + メーター
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = riskColor(result.riskLevel),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "危険度: ${result.riskLevel.label}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        RiskMeter(result.riskLevel)

        // 警告
        result.warning?.let {
            Spacer(Modifier.height(12.dp))
            Card {
                Text(
                    text = "⚠ $it",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // クールダウン提案
        if (result.cooldownSuggested && result.cooldownReason != null) {
            Spacer(Modifier.height(8.dp))
            Card {
                Text(
                    text = "⏸ 今は送らない方がいいかも\n${result.cooldownReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 相手の状態
        Spacer(Modifier.height(12.dp))
        Text("相手の状態", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(result.otherParty.note, style = MaterialTheme.typography.bodyMedium)

        // 検出された言動（意図 vs 受け取られ方）
        if (result.flags.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "特に悪化させやすい表現は見つかりませんでした。とはいえ、最終判断はあなた自身で。",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "検出された言動",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            result.flags.forEach { flag ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            flag.category.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (flag.evidence.isNotEmpty()) {
                            Text(
                                "根拠: ${flag.evidence.joinToString(" / ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("あなたの意図: ${flag.category.intent}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "相手の受け取られ方: ${flag.category.reception}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 言い換え
        if (result.rephrases.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "言い換えの提案（たたき台）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            result.rephrases.forEach { r ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.text, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            r.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(r.text))
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            Text(" コピー")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "※ これは判定ではなく“気づき”のための材料です。送るかどうかはあなたが決めてください。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RiskMeter(level: RiskLevel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val active = level.score // 0..3
        for (i in 0..3) {
            val on = i <= active
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp),
                color = if (on) riskColor(level) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {}
        }
    }
}
