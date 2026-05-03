package com.example.paryavaran_kavalu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RedeemItemDao {
    @Query("SELECT * FROM redeem_items ORDER BY id ASC")
    fun observeAll(): Flow<List<RedeemItemEntity>>

    @Query("SELECT * FROM redeem_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RedeemItemEntity?

    @Query("SELECT COUNT(*) FROM redeem_items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RedeemItemEntity>)
}
