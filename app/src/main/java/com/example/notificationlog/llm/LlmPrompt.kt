package com.example.notificationlog.llm

/**
 * Gemma 向けのプロンプト整形。中立コーチとして「お前が悪い」ではなく
 * 「相手にこう受け取られやすい」を短く返させる。最終判断は本人。
 */
object LlmPrompt {

    fun build(draft: String, recentOtherMessages: List<String>): String {
        val context = if (recentOtherMessages.isNotEmpty()) {
            "相手の直近メッセージ:\n" +
                recentOtherMessages.takeLast(6).joinToString("\n") { "- $it" } + "\n\n"
        } else {
            ""
        }

        val instruction = buildString {
            append("あなたは中立的なコミュニケーションのコーチです。")
            append("ユーザーがこれからLINEで送ろうとしている返信を分析します。")
            append("『あなたが悪い』と断罪してはいけません。")
            append("『この返信は相手にこう受け取られやすい』という観点で、短く具体的に指摘してください。")
            append("相手の感情も尊重し、最終判断はユーザー本人がすることを前提にします。\n\n")
            append(context)
            append("あなたの返信案:\n「")
            append(draft.trim())
            append("」\n\n")
            append("次の形式で、日本語で簡潔に答えてください:\n")
            append("■危険度: 低 / 中 / 高 のどれか\n")
            append("■なぜそう伝わるか: 自分の意図と、相手の受け取られ方を1〜2行で\n")
            append("■今は送らない方がいい？: 必要なら理由を1行で\n")
            append("■言い換え案: 角が立ちにくく、意図は保った文を1つ\n")
            append("最後に一言、判断はあなた次第です、と添えてください。")
        }

        // Gemma のチャットテンプレート
        return "<start_of_turn>user\n$instruction<end_of_turn>\n<start_of_turn>model\n"
    }
}
