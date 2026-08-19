package com.example.notificationlog.llm

/**
 * オンデバイスLLMによる詳細分析。手動の「AIで詳しく分析」でのみ使う（オーバーレイでは使わない）。
 */
interface LlmAdvisor {
    /** モデルが利用可能（ダウンロード済み）か。 */
    fun isReady(): Boolean

    /**
     * ドラフトを分析し、自然文のアドバイスを返す。
     * @param recentOtherMessages 直近の相手メッセージ（古い→新しい、あれば）。
     */
    suspend fun advise(draft: String, recentOtherMessages: List<String>): String

    /** ロード済みモデルを解放（メモリ節約）。 */
    fun release()
}
