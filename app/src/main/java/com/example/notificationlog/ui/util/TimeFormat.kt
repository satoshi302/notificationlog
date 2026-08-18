package com.example.notificationlog.ui.util

import java.util.Calendar
import java.util.Locale

private fun cal(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

/** 会話一覧向け：今日は時刻、昨日は「昨日」、それ以外は月/日 */
fun formatListTime(millis: Long): String {
    if (millis <= 0) return ""
    val now = Calendar.getInstance()
    val t = cal(millis)
    return when {
        isSameDay(now, t) -> String.format(
            Locale.JAPAN, "%02d:%02d",
            t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE)
        )
        isSameDay(cal(now.timeInMillis - 86_400_000L), t) -> "昨日"
        else -> String.format(
            Locale.JAPAN, "%d/%d",
            t.get(Calendar.MONTH) + 1, t.get(Calendar.DAY_OF_MONTH)
        )
    }
}

/** チャット吹き出しの時刻（HH:mm） */
fun formatBubbleTime(millis: Long): String {
    if (millis <= 0) return ""
    val t = cal(millis)
    return String.format(
        Locale.JAPAN, "%02d:%02d",
        t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE)
    )
}

/** 日付区切りラベル（例: 2026年7月30日(水)） */
fun formatDateSeparator(millis: Long): String {
    val t = cal(millis)
    val dow = arrayOf("日", "月", "火", "水", "木", "金", "土")[t.get(Calendar.DAY_OF_WEEK) - 1]
    return String.format(
        Locale.JAPAN, "%d年%d月%d日(%s)",
        t.get(Calendar.YEAR), t.get(Calendar.MONTH) + 1, t.get(Calendar.DAY_OF_MONTH), dow
    )
}

/** 2つの時刻が同じ日か（日付区切り判定用） */
fun isDifferentDay(a: Long, b: Long): Boolean = !isSameDay(cal(a), cal(b))
