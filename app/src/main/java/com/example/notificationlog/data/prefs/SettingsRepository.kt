package com.example.notificationlog.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 記録対象のパッケージ名集合を保存・監視する。初期状態は空（=何も記録しない）。
 */
class SettingsRepository(private val context: Context) {

    private val targetPackagesKey = stringSetPreferencesKey("target_packages")

    /** 記録対象パッケージ名の集合 */
    val targetPackages: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[targetPackagesKey] ?: emptySet() }

    /** 現在の対象集合を一度だけ取得（サービスからの同期判定用） */
    suspend fun currentTargetPackages(): Set<String> = targetPackages.first()

    suspend fun setTargetPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[targetPackagesKey] = packages
        }
    }

    suspend fun togglePackage(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[targetPackagesKey]?.toMutableSet() ?: mutableSetOf()
            if (enabled) current.add(packageName) else current.remove(packageName)
            prefs[targetPackagesKey] = current
        }
    }
}
