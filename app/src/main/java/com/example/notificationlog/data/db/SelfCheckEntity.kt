package com.example.notificationlog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * セルフチェック1回分の記録。傾向レポート（自分のパターン学習）の元データ。
 * ドラフト本文も端末内にのみ保存し、外部送信はしない。
 */
@Entity(
    tableName = "self_checks",
    indices = [Index(value = ["timestamp"]), Index(value = ["conversationKey"])]
)
data class SelfCheckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 対象の会話（手動チェックで会話未指定なら空文字可） */
    val conversationKey: String,

    /** どのアプリの会話か（不明なら空文字） */
    val packageName: String,

    /** チェックしたドラフト本文 */
    val draft: String,

    /** 危険度スコア（0低〜3危険） */
    val riskScore: Int,

    /** 検出カテゴリの CSV（SelfCheckCategory.name） */
    val flagsCsv: String,

    /** 相手の感情強度 0〜3 */
    val otherIntensity: Int,

    val timestamp: Long
)
