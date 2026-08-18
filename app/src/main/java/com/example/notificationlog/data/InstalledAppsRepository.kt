package com.example.notificationlog.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** アプリ選択画面に出す1アプリ分の情報 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

/**
 * 端末にインストールされた「ランチャーから起動できるアプリ」を列挙する。
 * QUERY_ALL_PACKAGES 権限が前提。
 */
class InstalledAppsRepository(private val context: Context) {

    suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        resolved
            .asSequence()
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName } // 自分自身は除外
            .distinct()
            .mapNotNull { pkg ->
                runCatching {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    InstalledApp(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo)
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** パッケージ名から表示名を引く（取得できなければパッケージ名を返す） */
    fun labelFor(packageName: String): String {
        val pm = context.packageManager
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    fun iconFor(packageName: String): Drawable? = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()
}
