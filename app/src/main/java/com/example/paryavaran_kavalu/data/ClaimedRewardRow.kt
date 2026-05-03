package com.example.paryavaran_kavalu.data

/**
 * Join row for “Claimed rewards” — matches Room column aliases in [RedemptionDao.observeClaimedRewards].
 */
data class ClaimedRewardRow(
    val itemId: Long,
    val category: String,
    val title: String,
    val subtitle: String,
    val costPoints: Int,
    val iconName: String,
    val timesRedeemed: Int,
)
