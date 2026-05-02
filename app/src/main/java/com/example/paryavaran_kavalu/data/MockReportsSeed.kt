package com.example.paryavaran_kavalu.data

import com.example.paryavaran_kavalu.util.offsetLatLon

/** Offline demo pins around [centerLat]/[centerLon]; tagged so they don’t block real user rows. */
object MockReportsSeed {

    const val DEMO_REPORTER_NICKNAME = "Demo patrol"

    const val DEMO_ROW_COUNT = 10

    private val demoRows =
        listOf(
            Triple(420.0, 12.0, "Plastic" to "Demo: roadside bags near junction."),
            Triple(890.0, 58.0, "Organic" to "Demo: vegetable waste pile."),
            Triple(1350.0, 105.0, "Glass" to "Demo: broken bottles."),
            Triple(1780.0, 155.0, "Metal" to "Demo: scrap metal dumped."),
            Triple(2100.0, 205.0, "Electronic" to "Demo: old cables and devices."),
            Triple(2650.0, 258.0, "Other" to "Demo: mixed construction debris."),
            Triple(3100.0, 305.0, "Plastic" to "Demo: bottles near drain."),
            Triple(3550.0, 352.0, "Organic" to "Demo: food waste."),
            Triple(4100.0, 22.0, "Plastic" to "Demo: cleaned earlier (mock)."),
            Triple(4750.0, 88.0, "Glass" to "Demo: far edge of neighbourhood."),
        )

    fun buildEntities(centerLat: Double, centerLon: Double, packageName: String): List<ReportEntity> {
        val imageUri = "android.resource://$packageName/drawable/ic_launcher_foreground"
        val now = System.currentTimeMillis()
        return demoRows.mapIndexed { index, triple ->
            val (distM, bearing, pair) = triple
            val (wasteType, desc) = pair
            val (lat, lon) = offsetLatLon(centerLat, centerLon, distM, bearing)
            val cleaned = index == 8
            ReportEntity(
                imageUri = imageUri,
                latitude = lat,
                longitude = lon,
                wasteType = wasteType,
                description = desc,
                status = if (cleaned) "Cleaned" else "Pending",
                timestamp = now - index * 3_600_000L,
                cleanedImageUri = if (cleaned) imageUri else null,
                cleanedAt = if (cleaned) now - 1_800_000L else null,
                reporterUserId = 1,
                reporterNickname = DEMO_REPORTER_NICKNAME,
            )
        }
    }
}
