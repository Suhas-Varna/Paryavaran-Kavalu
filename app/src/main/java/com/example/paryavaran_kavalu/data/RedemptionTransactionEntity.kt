package com.example.paryavaran_kavalu.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "redemption_transactions",
    primaryKeys = ["userId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RedeemItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("userId"),
        Index("itemId"),
    ],
)
data class RedemptionTransactionEntity(
    val userId: Int,
    val itemId: Long,
    /** How many times this user has redeemed this catalogue item. */
    val timesRedeemed: Int,
)
