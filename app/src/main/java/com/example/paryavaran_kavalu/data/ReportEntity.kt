package com.example.paryavaran_kavalu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val latitude: Double,
    val longitude: Double,
    /** Comma-separated categories (see [WasteTypeCsv]). */
    val wasteType: String,
    /**
     * Reporter notes for this incident — required when “Other” is among waste types; optional
     * otherwise but always stored (plastic + notes, mixed types, etc.).
     */
    val description: String,
    val status: String,
    val timestamp: Long,
    val cleanedImageUri: String? = null,
    val cleanedAt: Long? = null,
    /** Local profile id at submit time (single-user device; reserved for future sync). */
    val reporterUserId: Int = 1,
    /** Snapshot of [UserEntity.nickname] when the report was created. */
    val reporterNickname: String = "",
)
