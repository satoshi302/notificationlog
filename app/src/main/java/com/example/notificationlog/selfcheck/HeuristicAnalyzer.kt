package com.example.notificationlog.selfcheck

/**
 * 完全オンデバイスのルールベース解析。外部送信なし・即時・オフライン。
 * 「お前が悪い」ではなく「今の返信は相手にこう受け取られやすい」という観点で結果を返す。
 */
class HeuristicAnalyzer : SelfCheckAnalyzer {

    override fun analyze(draft: String, recentOtherMessages: List<String>): SelfCheckResult {
        val text = draft.trim()
        val flags = detectFlags(text)
        val amplifierHits = SelfCheckRules.amplifiers.count { it.containsMatchIn(text) }
        val other = estimateOtherParty(recentOtherMessages)

        val level = scoreRisk(flags, amplifierHits, other, text)

        val warning = buildWarning(level, flags, other)
        val cooldown = shouldCooldown(level, other)
        val cooldownReason = if (cooldown) buildCooldownReason(level, other) else null
        val rephrases = if (flags.isEmpty() && amplifierHits == 0) {
            emptyList()
        } else {
            buildRephrases(text, flags, other)
        }

        return SelfCheckResult(
            draft = draft,
            riskLevel = level,
            flags = flags,
            otherParty = other,
            warning = warning,
            cooldownSuggested = cooldown,
            cooldownReason = cooldownReason,
            rephrases = rephrases
        )
    }

    private fun detectFlags(text: String): List<DetectedFlag> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<DetectedFlag>()
        for ((category, regexes) in SelfCheckRules.patterns) {
            val evidence = mutableListOf<String>()
            for (r in regexes) {
                r.findAll(text).forEach { m ->
                    val hit = m.value.trim().ifEmpty { m.groupValues.lastOrNull()?.trim().orEmpty() }
                    if (hit.isNotEmpty() && hit !in evidence) evidence.add(hit)
                }
            }
            // 詰問は単発の「なんで」等は弱いので、2回以上か疑問符連続のときだけ採用
            if (category == SelfCheckCategory.INTERROGATION) {
                val questionWords = Regex("なんで|なぜ|どうして").findAll(text).count()
                val stackedMarks = Regex("[?？]{2,}").containsMatchIn(text)
                if (questionWords < 2 && !stackedMarks) continue
            }
            if (evidence.isNotEmpty()) {
                result.add(DetectedFlag(category, evidence.take(3)))
            }
        }
        return result
    }

    private fun estimateOtherParty(messages: List<String>): OtherPartyState {
        if (messages.isEmpty()) {
            return OtherPartyState(0, "相手の直近メッセージがないため、感情の強さは判定できません。")
        }
        val recent = messages.takeLast(6)
        var hits = 0
        for (m in recent) {
            for (cue in SelfCheckRules.otherPartyIntensityCues) {
                if (cue.containsMatchIn(m)) hits++
            }
        }
        val intensity = when {
            hits >= 3 -> 3
            hits == 2 -> 2
            hits == 1 -> 1
            else -> 0
        }
        val note = when (intensity) {
            3 -> "相手はかなり強い感情状態のようです。今は何を送っても強く受け取られやすい状態です。"
            2 -> "相手は感情的になっている可能性があります。言葉が普段より強く伝わりやすい状態です。"
            1 -> "相手にやや強い言葉が見られます。"
            else -> "相手は比較的落ち着いているように見えます。"
        }
        return OtherPartyState(intensity, note)
    }

    private fun scoreRisk(
        flags: List<DetectedFlag>,
        amplifierHits: Int,
        other: OtherPartyState,
        text: String
    ): RiskLevel {
        if (text.isEmpty()) return RiskLevel.LOW
        var score = flags.sumOf { it.category.weight }
        score += amplifierHits.coerceAtMost(3)
        // 相手が感情的なら同じ言葉でも悪化しやすい → 加点
        if (other.intensity >= 2) score += 1

        var level = when {
            score >= 5 -> RiskLevel.CRITICAL
            score in 3..4 -> RiskLevel.HIGH
            score in 1..2 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        // 最も刺さりやすい2カテゴリが出ていて相手も高ぶっていれば1段引き上げ
        val hasHarsh = flags.any {
            it.category == SelfCheckCategory.SARCASM ||
                it.category == SelfCheckCategory.DENY_FEELINGS
        }
        if (hasHarsh && other.intensity >= 2 && level != RiskLevel.CRITICAL) {
            level = RiskLevel.entries[(level.ordinal + 1).coerceAtMost(RiskLevel.CRITICAL.ordinal)]
        }
        return level
    }

    private fun buildWarning(
        level: RiskLevel,
        flags: List<DetectedFlag>,
        other: OtherPartyState
    ): String? {
        if (level.score < RiskLevel.HIGH.score) return null
        val cats = flags.joinToString("・") { it.category.label }
        val base = "この返信には「$cats」の要素が含まれています。"
        val tail = if (other.intensity >= 2) {
            "相手が感情的な今は、意図とは別に強く受け取られる可能性が高いです。"
        } else {
            "意図とは別に、相手には強く受け取られる可能性があります。"
        }
        return base + tail
    }

    private fun shouldCooldown(level: RiskLevel, other: OtherPartyState): Boolean {
        return level == RiskLevel.CRITICAL || (level == RiskLevel.HIGH && other.intensity >= 2)
    }

    private fun buildCooldownReason(level: RiskLevel, other: OtherPartyState): String {
        return if (other.intensity >= 2) {
            "お互い感情が高ぶっています。今は返信せず、少し時間を置いてから送る方が、後悔しにくいはずです。"
        } else {
            "危険度が高めです。一度読み返して、5分ほど置いてから送るか判断するのがおすすめです。"
        }
    }

    /** 中身は保ちつつ、悪化しにくい“たたき台”を作る（そのまま送れる完成文ではない）。 */
    private fun buildRephrases(
        text: String,
        flags: List<DetectedFlag>,
        other: OtherPartyState
    ): List<RephraseSuggestion> {
        var body = text

        // 皮肉・煽りトークンの除去
        for (r in SelfCheckRules.sarcasmStrip) body = r.replace(body, "")
        // 断定語の緩和
        for ((r, rep) in SelfCheckRules.absoluteSoften) body = r.replace(body, rep)
        // 詰問の緩和（最初の1箇所）
        body = Regex("(なんで|なぜ|どうして)").replaceFirst(body, "よかったら理由を教えてほしいんだけど、")
        // 二人称の攻撃をやわらげる
        body = Regex("(お前|おまえ|あんた)").replace(body, "あなた")
        // 連続する感嘆・句点の整理
        body = Regex("[!！]{2,}").replace(body, "。")
        body = Regex("[?？]{2,}").replace(body, "？")
        body = Regex("[。．]{3,}").replace(body, "…")
        body = body.trim()

        // 相手の気持ちの受け止めを先頭に添える
        val ack = when {
            flags.any { it.category == SelfCheckCategory.DENY_FEELINGS } ->
                "そう感じさせてたらごめん。あなたがそう思うのは分かる。"
            other.intensity >= 2 ->
                "まず、いやな思いをさせてたらごめん。"
            else -> null
        }

        val softened = listOfNotNull(ack, body).joinToString(" ").trim()

        val suggestions = mutableListOf<RephraseSuggestion>()
        if (softened.isNotEmpty() && softened != text) {
            suggestions.add(
                RephraseSuggestion(
                    text = softened,
                    note = "皮肉・断定・詰問をやわらげ、相手の気持ちを受け止める一言を足したたたき台です（要調整）。"
                )
            )
        }

        // I(アイ)メッセージのヒント（責任転嫁・反論が出たとき）
        if (flags.any {
                it.category == SelfCheckCategory.BLAME_SHIFT ||
                    it.category == SelfCheckCategory.REBUTTAL
            }
        ) {
            suggestions.add(
                RephraseSuggestion(
                    text = "「あなたが〜」ではなく「私は〜と感じた／〜してくれると助かる」の形に置き換えると、非難に聞こえにくくなります。",
                    note = "アイ・メッセージのヒント。"
                )
            )
        }

        return suggestions.take(2)
    }
}
