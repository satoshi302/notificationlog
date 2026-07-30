package com.example.notificationlog.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.notificationlog.data.InstalledAppsRepository

/**
 * パッケージ名からアプリアイコンを表示。取得できなければチャットアイコンで代替。
 */
@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            InstalledAppsRepository(context).iconFor(packageName)
                ?.toBitmap()
                ?.asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(size)
        )
    }
}
