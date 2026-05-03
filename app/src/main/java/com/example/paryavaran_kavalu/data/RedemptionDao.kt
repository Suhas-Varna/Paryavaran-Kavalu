package com.example.paryavaran_kavalu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {
    @Query("SELECT * FROM redemption_transactions WHERE userId = :userId AND itemId = :itemId LIMIT 1")
    suspend fun getTransaction(userId: Int, itemId: Long): RedemptionTransactionEntity?

    @Insert
    suspend fun insert(entity: RedemptionTransactionEntity)

    @Update
    suspend fun update(entity: RedemptionTransactionEntity)

    @Query(
        """
        SELECT i.id AS itemId, i.category AS category, i.title AS title, i.subtitle AS subtitle,
               i.costPoints AS costPoints, i.iconName AS iconName, r.timesRedeemed AS timesRedeemed
        FROM redemption_transactions r
        INNER JOIN redeem_items i ON i.id = r.itemId
        WHERE r.userId = :userId AND r.timesRedeemed > 0
        ORDER BY i.id ASC
        """,
    )
    fun observeClaimedRewards(userId: Int): Flow<List<ClaimedRewardRow>>
}
