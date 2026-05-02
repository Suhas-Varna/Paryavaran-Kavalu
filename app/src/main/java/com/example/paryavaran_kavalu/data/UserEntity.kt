package com.example.paryavaran_kavalu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val userId: Int = 1,
    /** Display name shown on leaderboard and stored on reports as attribution. */
    val nickname: String,
    /** One of [UserTypes]. */
    val userType: String,
    /** Short note (optional): locality, interests, contact preference text, etc. */
    val bio: String,
    val ecoPoints: Int,
)
