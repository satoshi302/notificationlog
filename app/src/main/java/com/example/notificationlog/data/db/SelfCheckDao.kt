package com.example.notificationlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 傾向レポート用の、カテゴリ別集計行。 */
data class FlagCount(
    val flag: String,
    val count: Int
)

@Dao
interface SelfCheckDao {

    @Insert
    suspend fun insert(entity: SelfCheckEntity): Long

    @Query("SELECT COUNT(*) FROM self_checks")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM self_checks ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SelfCheckEntity>

    /** 危険度が高め（HIGH=2以上）だった回数。 */
    @Query("SELECT COUNT(*) FROM self_checks WHERE riskScore >= 2")
    suspend fun highRiskCount(): Int

    @Query("SELECT COUNT(*) FROM self_checks")
    suspend fun totalCount(): Int

    /** 相手が感情的（intensity>=2）なときのチェック回数。 */
    @Query("SELECT COUNT(*) FROM self_checks WHERE otherIntensity >= 2")
    suspend fun otherAgitatedCount(): Int

    @Query("DELETE FROM self_checks")
    suspend fun clearAll()
}
