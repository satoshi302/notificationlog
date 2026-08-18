package com.example.notificationlog.selfcheck

/**
 * 送る前のドラフトを解析するエンジンの抽象。
 * 現状は完全オンデバイスの [HeuristicAnalyzer]。将来オンデバイスLLM実装を差し込める。
 */
interface SelfCheckAnalyzer {
    /**
     * @param draft これから送ろうとしている自分の文
     * @param recentOtherMessages 直近の相手メッセージ本文（古い→新しい）。本アプリは受信通知のみ
     *   記録するため、履歴は基本的に相手側の発言。
     */
    fun analyze(draft: String, recentOtherMessages: List<String>): SelfCheckResult
}
