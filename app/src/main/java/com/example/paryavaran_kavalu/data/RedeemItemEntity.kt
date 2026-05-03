package com.example.paryavaran_kavalu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catalogue row for the Eco‑karma redeem grid — seeded with fixed [id] values (1…n).
 */
@Entity(tableName = "redeem_items")
data class RedeemItemEntity(
    @PrimaryKey val id: Long,
    val category: String,
    val title: String,
    val subtitle: String,
    val costPoints: Int,
    /** Key for [com.example.paryavaran_kavalu.ui.screens.redeemIconFor]. */
    val iconName: String,
)
