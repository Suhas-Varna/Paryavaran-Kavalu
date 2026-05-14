package com.example.paryavaran_kavalu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReportEntity?

    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Update
    suspend fun update(report: ReportEntity)

    @Query("SELECT COUNT(*) FROM reports WHERE reporterNickname = :nickname")
    suspend fun countByReporterNickname(nickname: String): Int

    @Query("DELETE FROM reports WHERE reporterNickname = :nickname")
    suspend fun deleteByReporterNickname(nickname: String)

    /**
     * Seeded “Demo patrol” rows verified clean by [cleanerUserId] — used to adjust eco points when
     * replacing the demo cluster.
     */
    @Query(
        """
        SELECT COUNT(*) FROM reports
        WHERE reporterNickname = :demoReporterNick
        AND LOWER(TRIM(status)) = 'cleaned'
        AND cleanerUserId = :cleanerUserId
        """,
    )
    suspend fun countDemoCleanupsForCleaner(demoReporterNick: String, cleanerUserId: Int): Int

    @Query("DELETE FROM reports")
    suspend fun deleteAllReports()

    /**
     * Keeps [ReportEntity.cleanerNickname] aligned with the single local profile for all cleanups
     * attributed to [cleanerUserId] 1 (offline single-user model).
     */
    @Query(
        """
        UPDATE reports SET cleanerNickname = :nickname
        WHERE cleanerUserId = 1 AND LOWER(TRIM(status)) = 'cleaned'
        """,
    )
    suspend fun syncCleanerNicknamesForLocalUser(nickname: String)

    /**
     * Updates reporter display names for rows filed by the local user, excluding the seeded
     * “Demo patrol” demo cluster when [excludeReporterNickname] matches.
     */
    @Query(
        """
        UPDATE reports SET reporterNickname = :nickname
        WHERE reporterUserId = 1 AND reporterNickname != :excludeReporterNickname
        """,
    )
    suspend fun syncReporterNicknamesForLocalUser(nickname: String, excludeReporterNickname: String)
}
