package com.example.paryavaran_kavalu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val latitude: Double,
    val longitude: Double,
    val wasteType: String,
    val description: String,
    val status: String,
    val timestamp: Long,
    val cleanedImageUri: String? = null,
    val cleanedAt: Long? = null,
)
