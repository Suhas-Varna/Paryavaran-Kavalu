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
}
