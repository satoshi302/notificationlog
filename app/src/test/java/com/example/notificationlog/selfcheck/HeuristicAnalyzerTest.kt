package com.example.notificationlog.selfcheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicAnalyzerTest {

    private val analyzer = HeuristicAnalyzer()

    @Test
    fun harshDraft_isHighRisk_andDetectsSarcasmAndBlame() {
        val draft = "どうせお前がいつも悪いんだろ。"
        val result = analyzer.analyze(draft, emptyList())

        assertTrue(
            "危険度は高以上のはず (${result.riskLevel})",
            result.riskLevel.score >= RiskLevel.HIGH.score
        )
        val cats = result.flags.map { it.category }
        assertTrue("皮肉を検出するはず", SelfCheckCategory.SARCASM in cats)
        assertTrue("責任転嫁を検出するはず", SelfCheckCategory.BLAME_SHIFT in cats)
        assertTrue("言い換え候補が出るはず", result.rephrases.isNotEmpty())
    }

    @Test
    fun kindDraft_isLowRisk_withNoFlags() {
        val draft = "教えてくれてありがとう。あとで確認するね。"
        val result = analyzer.analyze(draft, emptyList())

        assertEquals(RiskLevel.LOW, result.riskLevel)
        assertTrue("検出なしのはず", result.flags.isEmpty())
        assertTrue("言い換えは不要のはず", result.rephrases.isEmpty())
    }

    @Test
    fun agitatedOtherParty_raisesRisk() {
        val draft = "でも、それは違うと思う。"
        val calm = analyzer.analyze(draft, listOf("そうなんだね"))
        val agitated = analyzer.analyze(draft, listOf("もう無理！！", "最悪。ふざけるな"))

        assertTrue(
            "相手が感情的なときは危険度が同等以上になるはず",
            agitated.riskLevel.score >= calm.riskLevel.score
        )
        assertTrue("相手の感情強度が上がるはず", agitated.otherParty.intensity >= 2)
    }

    @Test
    fun interrogation_needsRepetitionOrStackedMarks() {
        val single = analyzer.analyze("なんで遅れたの", emptyList())
        val stacked = analyzer.analyze("なんで？？", emptyList())

        assertTrue(
            "単発のなんでは詰問扱いしない",
            single.flags.none { it.category == SelfCheckCategory.INTERROGATION }
        )
        assertTrue(
            "疑問符連続は詰問扱い",
            stacked.flags.any { it.category == SelfCheckCategory.INTERROGATION }
        )
    }
}
