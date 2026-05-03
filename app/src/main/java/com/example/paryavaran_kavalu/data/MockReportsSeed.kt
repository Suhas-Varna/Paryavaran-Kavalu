package com.example.paryavaran_kavalu.data

import com.example.paryavaran_kavalu.util.offsetLatLon

/** Offline demo pins around [centerLat]/[centerLon]; tagged so they don’t block real user rows. */
object MockReportsSeed {

    const val DEMO_REPORTER_NICKNAME = "Demo patrol"

    /** Single-user demo: cleaned pins use local profile id 1 so leaderboard → map can filter by user. */
    const val DEMO_CLEANER_USER_ID = 1

    /** Shown in Room debug / maps when filtering by nickname fallback (seeded “already cleaned” pins). */
    const val DEMO_CLEANER_NICKNAME = "Seeded cleanup"

    const val DEMO_ROW_COUNT = 10

    /**
     * Nearby cluster (~180–950 m) so pins stay visible around the current map centre (e.g. 12.9716, 77.5946).
     * Triple: distanceM, bearingDeg, description (waste type is chosen by index into [WasteMenu.types]).
     */
    private val demoRows =
        listOf(
            Triple(180.0, 5.0, "Demo: bags near footpath."),
            Triple(240.0, 42.0, "Demo: vegetable market scraps."),
            Triple(310.0, 88.0, "Demo: broken glass heap."),
            Triple(410.0, 125.0, "Demo: scrap drums."),
            Triple(480.0, 162.0, "Demo: e‑waste pile."),
            Triple(560.0, 205.0, "Demo: mixed debris."),
            Triple(640.0, 242.0, "Demo: bottles by drain."),
            Triple(720.0, 285.0, "Demo: food waste bin overflow."),
            Triple(820.0, 322.0, "Demo: cleared earlier (mock)."),
            Triple(920.0, 358.0, "Demo: glass bits by crossing."),
        )

    /** Rows shown as Cleaned (green pin) on the map — rest are Pending. */
    private val cleanedRowIndices = setOf(3, 8)

    fun buildEntities(centerLat: Double, centerLon: Double, packageName: String): List<ReportEntity> {
        val imageUri = "android.resource://$packageName/drawable/ic_launcher_foreground"
        val now = System.currentTimeMillis()
        return demoRows.mapIndexed { index, triple ->
            val (distM, bearing, desc) = triple
            val wasteType = WasteTypeCsv.normalize(
                listOf(WasteMenu.types[index % WasteMenu.types.size]),
            )
            val (lat, lon) = offsetLatLon(centerLat, centerLon, distM, bearing)
            val cleaned = index in cleanedRowIndices
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
                cleanerUserId = if (cleaned) DEMO_CLEANER_USER_ID else null,
                cleanerNickname = if (cleaned) DEMO_CLEANER_NICKNAME else "",
                reporterUserId = 1,
                reporterNickname = DEMO_REPORTER_NICKNAME,
            )
        }
    }
}
