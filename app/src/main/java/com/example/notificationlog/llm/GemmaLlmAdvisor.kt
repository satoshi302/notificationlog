package com.example.notificationlog.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaPipe LLM Inference で Gemma 4 E2B（.task）を実行する [LlmAdvisor]。
 * モデルは初回ロードに数秒かかり、メモリも使うため、生成は必要時のみ。
 */
class GemmaLlmAdvisor(
    private val context: Context,
    private val modelManager: LlmModelManager
) : LlmAdvisor {

    @Volatile
    private var engine: LlmInference? = null

    override fun isReady(): Boolean = modelManager.isDownloaded()

    override suspend fun advise(
        draft: String,
        recentOtherMessages: List<String>
    ): String = withContext(Dispatchers.Default) {
        val llm = ensureLoaded()
        val prompt = LlmPrompt.build(draft, recentOtherMessages)
        llm.generateResponse(prompt).trim()
    }

    private fun ensureLoaded(): LlmInference {
        engine?.let { return it }
        synchronized(this) {
            engine?.let { return it }
            check(modelManager.isDownloaded()) { "モデルがダウンロードされていません。" }
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelManager.modelFile.absolutePath)
                .setMaxTokens(1024)
                .build()
            val created = LlmInference.createFromOptions(context, options)
            engine = created
            return created
        }
    }

    override fun release() {
        synchronized(this) {
            runCatching { engine?.close() }
            engine = null
        }
    }
}
