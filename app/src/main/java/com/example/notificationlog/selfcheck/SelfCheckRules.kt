package com.example.notificationlog.selfcheck

/**
 * 日本語の手がかり語・正規表現。完全オンデバイスで動くルールベース検出の辞書。
 * すべて端末内で完結し、外部送信は一切しない。
 */
object SelfCheckRules {

    /** カテゴリごとの検出パターン（どれかにマッチしたら該当）。 */
    val patterns: Map<SelfCheckCategory, List<Regex>> = mapOf(
        SelfCheckCategory.REBUTTAL to regexes(
            "(^|[。、！？\\s])(でも|だって|いや、|いやいや|そうじゃなくて|とはいえ|けど、)"
        ),
        SelfCheckCategory.BLAME_SHIFT to regexes(
            "(君|きみ|お前|おまえ|あんた|そっち|そちら)が",
            "(の|が)せい",
            "あなたが.*(から|ので|のに)"
        ),
        SelfCheckCategory.DEFLECTION to regexes(
            "それより", "そもそも", "(前|以前|この前)も", "話をそらす", "今はその話"
        ),
        SelfCheckCategory.DENY_FEELINGS to regexes(
            "考えすぎ", "気にしすぎ", "大げさ", "そんなことで", "気のせい",
            "たいしたこと(ない|じゃない)", "被害妄想"
        ),
        SelfCheckCategory.PROVE_RIGHT to regexes(
            "(俺|私|こっち)が正しい", "証拠", "論理的", "普通は", "常識(的)?", "事実として", "客観的に"
        ),
        SelfCheckCategory.INTERROGATION to regexes(
            "なんで", "なぜ", "どうして", "説明して", "答えて", "\\?{2,}", "？{2,}"
        ),
        SelfCheckCategory.SARCASM to regexes(
            "どうせ", "はいはい", "へぇ+", "ふーん", "別に", "勝手に(すれば|して)",
            "（笑）", "\\(笑\\)", "ｗ{2,}", "www+", "。{3,}", "、、、+"
        ),
        SelfCheckCategory.SELF_JUSTIFY to regexes(
            "(俺|私|こっち)は.*(してる|している|やってる|やっている|頑張ってる|頑張っている)",
            "こっちだって", "俺だって", "私だって"
        ),
        SelfCheckCategory.SELF_DEFENSE to regexes(
            "悪くない", "(の|が)せいじゃない", "そんなつもり(はない|じゃない|ない)",
            "誤解", "言ってない", "やってない"
        ),
        SelfCheckCategory.OVER_DEMAND to regexes(
            "どうすればいい", "どうしたいの", "(結局|で、)どうする",
            "(して|しろ|やって|直して|変えて)(ほしい|くれ|よ)?$", "べき(だ|でしょ)?"
        )
    )

    /** リスクを増幅する断定・極端語。 */
    val amplifiers: List<Regex> = regexes(
        "絶対", "いつも", "毎回", "必ず", "全部", "一切", "二度と", "みんな", "誰も", "何も"
    )

    /** 相手が強い感情状態にあることを示す語（相手メッセージ側で見る）。 */
    val otherPartyIntensityCues: List<Regex> = regexes(
        "最悪", "嫌い", "むかつく", "ムカつく", "ふざけるな", "もういい", "もう無理",
        "勝手にして", "二度と", "許さない", "うざい", "きもい", "死ね", "！{2,}", "!{2,}"
    )

    /** 皮肉・煽りトークン（言い換え時に除去する）。 */
    val sarcasmStrip: List<Regex> = regexes(
        "どうせ", "はいはい", "へぇ+", "ふーん", "別に", "（笑）", "\\(笑\\)", "ｗ+", "www+"
    )

    /** 断定語の緩和マップ（言い換え時）。 */
    val absoluteSoften: List<Pair<Regex, String>> = listOf(
        re("絶対") to "なるべく",
        re("いつも") to "ときどき",
        re("毎回") to "ときどき",
        re("必ず") to "できれば",
        re("全部") to "いくつか",
        re("二度と") to "あまり"
    )

    private fun regexes(vararg p: String): List<Regex> = p.map { re(it) }
    private fun re(p: String): Regex = Regex(p, RegexOption.IGNORE_CASE)
}
