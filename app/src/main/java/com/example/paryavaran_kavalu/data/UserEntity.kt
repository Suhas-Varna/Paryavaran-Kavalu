package com.example.paryavaran_kavalu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val userId: Int = 1,
    val name: String,
    val ecoPoints: Int,
)
