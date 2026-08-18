package com.example.notificationlog.data

import com.example.notificationlog.data.db.MessageDao
import com.example.notificationlog.data.db.SelfCheckDao
import com.example.notificationlog.data.db.SelfCheckEntity
import com.example.notificationlog.selfcheck.SelfCheckAnalyzer
import com.example.notificationlog.selfcheck.SelfCheckCategory
import com.example.notificationlog.selfcheck.SelfCheckResult
import kotlinx.coroutines.flow.Flow

/** 傾向レポートの集計結果。 */
data class TrendReport(
    val total: Int,
    val highRiskCount: Int,
    val otherAgitatedCount: Int,
    val topFlags: List<Pair<SelfCheckCategory, Int>>,
    val feedback: List<String>
)

/**
 * セルフチェックのデータ操作。会話文脈の取得・解析・履歴保存・傾向集計をまとめる。
 * すべて端末内で完結。
 */
class SelfCheckRepository(
    private val messageDao: MessageDao,
    private val selfCheckDao: SelfCheckDao,
    val analyzer: SelfCheckAnalyzer
) {

    /** 指定会話の直近メッセージ本文（古い→新しい）。手動チェックで未指定なら空。 */
    suspend fun recentContext(conversationKey: String, limit: Int = 12): List<String> {
        if (conversationKey.isBlank()) return emptyList()
        return messageDao.getRecent(conversationKey, limit)
            .sortedBy { it.timestamp }
            .map { it.text }
    }

    fun analyze(draft: String, context: List<String>): SelfCheckResult =
        analyzer.analyze(draft, context)

    suspend fun record(
        conversationKey: String,
        packageName: String,
        result: SelfCheckResult
    ) {
        selfCheckDao.insert(
            SelfCheckEntity(
                conversationKey = conversationKey,
                packageName = packageName,
                draft = result.draft,
                riskScore = result.riskLevel.score,
                flagsCsv = result.flagsCsv(),
                otherIntensity = result.otherParty.intensity,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun observeCount(): Flow<Int> = selfCheckDao.observeCount()

    suspend fun clearHistory() = selfCheckDao.clearAll()

    /** 過去のチェックから、自分が悪化させやすいパターンを集計する。 */
    suspend fun buildTrend(): TrendReport {
        val total = selfCheckDao.totalCount()
        val highRisk = selfCheckDao.highRiskCount()
        val otherAgitated = selfCheckDao.otherAgitatedCount()
        val recent = selfCheckDao.recent(500)

        val counts = HashMap<SelfCheckCategory, Int>()
        for (row in recent) {
            for (name in row.flagsCsv.split(",")) {
                val cat = runCatching { SelfCheckCategory.valueOf(name.trim()) }.getOrNull() ?: continue
                counts[cat] = (counts[cat] ?: 0) + 1
            }
        }
        val topFlags = counts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }

        return TrendReport(
            total = total,
            highRiskCount = highRisk,
            otherAgitatedCount = otherAgitated,
            topFlags = topFlags,
            feedback = buildFeedback(total, highRisk, otherAgitated, topFlags)
        )
    }

    private fun buildFeedback(
        total: Int,
        highRisk: Int,
        otherAgitated: Int,
        topFlags: List<Pair<SelfCheckCategory, Int>>
    ): List<String> {
        if (total == 0) {
            return listOf("まだデータがありません。送る前のチェックを重ねると、あなたの傾向が見えてきます。")
        }
        val out = mutableListOf<String>()

        topFlags.firstOrNull()?.let { (cat, _) ->
            val tip = when (cat) {
                SelfCheckCategory.SARCASM ->
                    "皮肉・煽りから入りやすい傾向。まず事実だけを短く伝えると悪化しにくいです。"
                SelfCheckCategory.REBUTTAL ->
                    "『でも／だって』の反論から入りやすい傾向。一度受け止めてから伝えると届きやすくなります。"
                SelfCheckCategory.BLAME_SHIFT ->
                    "責任転嫁になりやすい傾向。『あなたが』を『私は〜と感じた』に置き換えるのが効果的。"
                SelfCheckCategory.DENY_FEELINGS ->
                    "相手の感情を否定しがちな傾向。まず気持ちを認める一言が関係を守ります。"
                SelfCheckCategory.INTERROGATION ->
                    "詰問になりやすい傾向。質問を1つに絞り、責める調子を外すと圧迫感が減ります。"
                SelfCheckCategory.PROVE_RIGHT ->
                    "正しさの証明に向かいやすい傾向。勝ち負けより、気持ちの共有を先にすると良いです。"
                SelfCheckCategory.SELF_JUSTIFY ->
                    "自己弁護（俺は〜してる）が出やすい傾向。自分の努力の前に、相手の話を受け止めると伝わりやすいです。"
                SelfCheckCategory.SELF_DEFENSE ->
                    "保身が出やすい傾向。弁明の前にひと言認めるだけで印象が変わります。"
                SelfCheckCategory.DEFLECTION ->
                    "論点ずらしになりやすい傾向。今の話を1つずつ扱うと逃げた印象を与えません。"
                SelfCheckCategory.OVER_DEMAND ->
                    "解決を急ぎやすい傾向。まず気持ちに寄り添ってから提案すると受け入れられやすいです。"
            }
            out.add("あなたが最も出やすいのは「${cat.label}」です。$tip")
        }

        if (otherAgitated > 0 && otherAgitated * 2 >= total) {
            out.add("相手が感情的なときにチェックしていることが多いようです。その時ほど、一呼吸置くのが効果的です。")
        }
        if (highRisk > 0) {
            out.add("これまで $total 回中 $highRisk 回が危険度「高」以上でした。送る前に気づけているのは良い一歩です。")
        }
        return out
    }
}
