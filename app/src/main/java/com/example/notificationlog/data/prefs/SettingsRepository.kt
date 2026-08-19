package com.example.notificationlog.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val selfCheckEnabledKey = booleanPreferencesKey("self_check_enabled")
    private val selfCheckThresholdKey = intPreferencesKey("self_check_threshold")
    private val llmModelUrlKey = stringPreferencesKey("llm_model_url")
    private val hfTokenKey = stringPreferencesKey("hf_token")

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

    // --- セルフチェック（LINE会話の自動読取＋警告）設定 ---

    /** 自動読取＆オーバーレイ警告を使うか。既定は無効（明示的にONにしてもらう）。 */
    val selfCheckEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[selfCheckEnabledKey] ?: false }

    suspend fun currentSelfCheckEnabled(): Boolean = selfCheckEnabled.first()

    suspend fun setSelfCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[selfCheckEnabledKey] = enabled }
    }

    /** オーバーレイ警告を出す最小の危険度スコア（1中/2高/3危険）。既定は 2=高。 */
    val selfCheckThreshold: Flow<Int> = context.dataStore.data
        .map { (it[selfCheckThresholdKey] ?: 2).coerceIn(1, 3) }

    suspend fun currentSelfCheckThreshold(): Int = selfCheckThreshold.first()

    suspend fun setSelfCheckThreshold(threshold: Int) {
        context.dataStore.edit { it[selfCheckThresholdKey] = threshold.coerceIn(1, 3) }
    }

    // --- オンデバイスLLM（Gemma 4 E2B）設定 ---

    /** モデル(.task)のダウンロードURL。既定は Gemma 4 E2B の LiteRT community。 */
    val llmModelUrl: Flow<String> = context.dataStore.data
        .map { it[llmModelUrlKey] ?: DEFAULT_LLM_MODEL_URL }

    suspend fun currentLlmModelUrl(): String = llmModelUrl.first()

    suspend fun setLlmModelUrl(url: String) {
        context.dataStore.edit { it[llmModelUrlKey] = url.trim() }
    }

    /** Hugging Face アクセストークン（gated リポジトリ用。任意）。 */
    val hfToken: Flow<String> = context.dataStore.data
        .map { it[hfTokenKey] ?: "" }

    suspend fun currentHfToken(): String = hfToken.first()

    suspend fun setHfToken(token: String) {
        context.dataStore.edit { it[hfTokenKey] = token.trim() }
    }

    companion object {
        const val DEFAULT_LLM_MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-web.task"
    }
}
