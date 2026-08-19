package com.example.notificationlog.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * オンデバイスLLMモデル（Gemma3 1B の `.task`）の保存・ダウンロード・削除を管理する。
 * モデルは端末内 `filesDir/models/` に置き、外部送信はしない（DL は取得のみ）。
 */
class LlmModelManager(private val context: Context) {

    val modelDir: File = File(context.filesDir, "models").apply { mkdirs() }
    // Gemma 4 E2B（Apache-2.0）の MediaPipe 用 .task。
    val modelFile: File = File(modelDir, "gemma-4-E2B-it-web.task")

    /** ざっくり妥当性チェック（途中で切れた小さいファイルを「あり」と誤認しないため 50MB 超で判定）。 */
    fun isDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 50_000_000L

    fun modelSizeBytes(): Long = if (modelFile.exists()) modelFile.length() else 0L

    fun delete() {
        runCatching { if (modelFile.exists()) modelFile.delete() }
    }

    /**
     * モデルをダウンロード。HTTP リダイレクト追従、任意の Bearer トークン対応。
     * @param onProgress (downloaded, total) total 不明時は -1。
     */
    suspend fun download(
        url: String,
        token: String?,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tmp = File(modelDir, modelFile.name + ".part")
        try {
            var current = url
            var conn: HttpURLConnection
            var redirects = 0
            while (true) {
                conn = (URL(current).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    instanceFollowRedirects = false
                    if (!token.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    current = loc
                    if (++redirects > 5) return@withContext Result.failure(
                        IllegalStateException("リダイレクトが多すぎます")
                    )
                    continue
                }
                if (code == 401 || code == 403) {
                    conn.disconnect()
                    return@withContext Result.failure(
                        IllegalStateException(
                            "ダウンロードが拒否されました（$code）。Gemma 4 は Apache-2.0 ですが、" +
                                "Hugging Face の公式リポジトリは規約同意が必要（gated）な場合があります。" +
                                "設定で Hugging Face のアクセストークンを入れるか、ungated のミラーURLに変更してください。"
                        )
                    )
                }
                if (code != 200) {
                    conn.disconnect()
                    return@withContext Result.failure(IllegalStateException("HTTP $code"))
                }
                break
            }

            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(1 shl 16)
                    var downloaded = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        downloaded += n
                        onProgress(downloaded, total)
                    }
                }
            }
            conn.disconnect()

            if (tmp.length() < 50_000_000L) {
                tmp.delete()
                return@withContext Result.failure(
                    IllegalStateException("ダウンロードが不完全です（サイズが小さすぎます）。")
                )
            }
            if (modelFile.exists()) modelFile.delete()
            if (!tmp.renameTo(modelFile)) {
                tmp.delete()
                return@withContext Result.failure(IllegalStateException("保存に失敗しました。"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { tmp.delete() }
            Result.failure(e)
        }
    }
}
