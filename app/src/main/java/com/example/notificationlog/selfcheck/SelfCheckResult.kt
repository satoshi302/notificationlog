package com.example.notificationlog.selfcheck

/** 会話を悪化させやすい言動のカテゴリ。intent=自分の意図、reception=相手の受け取られ方。 */
enum class SelfCheckCategory(
    val label: String,
    val intent: String,
    val reception: String,
    val weight: Int
) {
    REBUTTAL(
        "反論",
        "事実や誤解を正したい",
        "話を聞いてもらえない・否定された、と感じさせやすい",
        1
    ),
    BLAME_SHIFT(
        "責任転嫁",
        "自分ばかり責められたくない",
        "『こっちが悪いのか』と、責められたように感じさせやすい",
        2
    ),
    DEFLECTION(
        "論点ずらし",
        "他にも大事な点を伝えたい",
        "『今の話から逃げた・はぐらかされた』と感じさせやすい",
        1
    ),
    DENY_FEELINGS(
        "相手の感情の否定",
        "落ち着いてほしい／大げさに考えないでほしい",
        "『気持ちを否定された・軽く扱われた』と強く傷つけやすい",
        3
    ),
    PROVE_RIGHT(
        "正しさの証明",
        "自分の正しさを分かってほしい",
        "『論破された・勝ち負けにされた』と感じさせやすい",
        2
    ),
    INTERROGATION(
        "詰問",
        "理由を知りたい",
        "『責め立てられている』と圧迫感を与えやすい",
        2
    ),
    SARCASM(
        "煽り・皮肉",
        "感情を軽く表したつもり",
        "『バカにされた・見下された』と、最も強い反発を招きやすい",
        3
    ),
    SELF_JUSTIFY(
        "自己弁護（俺は〜してる）",
        "自分の努力を分かってほしい",
        "『言い訳・自分の話ばかり』と受け取られやすい",
        1
    ),
    SELF_DEFENSE(
        "保身",
        "誤解を解きたい",
        "『反省していない』と感じさせやすい",
        1
    ),
    OVER_DEMAND(
        "要求・解決の求めすぎ",
        "早く解決したい",
        "『気持ちより解決を急がれた』と、置いていかれた感を与えやすい",
        1
    );
}

/** 危険度。数値が大きいほど会話が悪化しやすい。 */
enum class RiskLevel(val label: String, val score: Int) {
    LOW("低", 0),
    MEDIUM("中", 1),
    HIGH("高", 2),
    CRITICAL("危険", 3)
}

/** 検出された1カテゴリと、その根拠になった語句。 */
data class DetectedFlag(
    val category: SelfCheckCategory,
    val evidence: List<String>
)

/** 相手の感情状態の推定（受信済みの相手メッセージから）。 */
data class OtherPartyState(
    /** 0=落ち着いている 〜 3=強く感情的 */
    val intensity: Int,
    val note: String
)

/** 言い換え候補（そのまま送れる完成文ではなく“たたき台”）。 */
data class RephraseSuggestion(
    val text: String,
    val note: String
)

/** セルフチェックの結果一式。 */
data class SelfCheckResult(
    val draft: String,
    val riskLevel: RiskLevel,
    val flags: List<DetectedFlag>,
    val otherParty: OtherPartyState,
    val warning: String?,
    val cooldownSuggested: Boolean,
    val cooldownReason: String?,
    val rephrases: List<RephraseSuggestion>
) {
    /** 保存用: 検出カテゴリを CSV 化。 */
    fun flagsCsv(): String = flags.joinToString(",") { it.category.name }
}
