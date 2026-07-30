package com.example.notificationlog.ui.permission

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 通知アクセス未許可時に表示する案内画面。
 */
@Composable
fun PermissionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "通知へのアクセスを許可してください",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "LINE などのメッセージ通知を記録するには、このアプリに「通知へのアクセス」" +
                "権限が必要です。下のボタンから設定を開き、「通知ログ」をオンにしてください。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }) {
            Text("通知アクセス設定を開く")
        }
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "ご注意\n" +
                    "・記録できるのは許可した後に届いた通知だけです。過去の通知は取得できません。\n" +
                    "・Galaxy では、確実に記録するために設定でこのアプリを電池最適化の対象外にすることを" +
                    "おすすめします。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
