package com.example.notificationlog.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.example.notificationlog.App
import com.example.notificationlog.ui.permission.PermissionScreen
import com.example.notificationlog.ui.theme.NotificationLogTheme
import com.example.notificationlog.ui.util.isNotificationAccessGranted

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as App

        setContent {
            NotificationLogTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootGate(app = app)
                }
            }
        }
    }
}

/**
 * 通知アクセスの許可状態で表示を切り替える。設定から戻ってきた（ON_RESUME）タイミングで再判定する。
 */
@Composable
private fun RootGate(app: App) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(isNotificationAccessGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (granted) {
        AppNav(app = app)
    } else {
        PermissionScreen()
    }
}
