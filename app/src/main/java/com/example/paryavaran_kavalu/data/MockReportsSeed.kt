package com.example.paryavaran_kavalu.data

import com.example.paryavaran_kavalu.util.distanceMeters
import com.example.paryavaran_kavalu.util.offsetLatLon
import kotlin.random.Random

/** Offline demo pins around [centerLat]/[centerLon]; tagged so they don’t block real user rows. */
object MockReportsSeed {

    const val DEMO_REPORTER_NICKNAME = "Demo patrol"

    const val DEMO_ROW_COUNT = 26

    /** Visible when the map first opens at default zoom — random ring close to “you”. */
    private const val NEARBY_DEMO_PIN_COUNT = 6

    /** Remaining pins in the outer annulus (2.2–9.8 km) for 3 / 5 / 10 km filters. */
    private const val FAR_DEMO_PIN_COUNT = DEMO_ROW_COUNT - NEARBY_DEMO_PIN_COUNT

    private const val NEARBY_INNER_M = 140.0
    private const val NEARBY_OUTER_M = 920.0
    private const val FAR_INNER_M = 2_200.0
    private const val FAR_OUTER_M = 9_800.0

    private val demoDescriptions = listOf(
        "Demo: bags near footpath.",
        "Demo: vegetable market scraps.",
        "Demo: broken glass heap.",
        "Demo: scrap drums.",
        "Demo: e‑waste pile.",
        "Demo: mixed debris.",
        "Demo: bottles by drain.",
        "Demo: food waste bin overflow.",
        "Demo: cleared earlier (mock).",
        "Demo: glass bits by crossing.",
        "Demo: roadside plastic pile.",
        "Demo: construction rubble.",
        "Demo: tyre dump corner.",
        "Demo: paper/cardboard windblown.",
        "Demo: metal scrap heap.",
        "Demo: sanitary waste spot.",
        "Demo: paint cans discarded.",
        "Demo: styrofoam chunks.",
        "Demo: textile / cloth bundle.",
        "Demo: oil stains patch.",
        "Demo: hedge clippings pile.",
        "Demo: illegal dump corner.",
        "Demo: foam packaging heap.",
        "Demo: cable / wire tangle.",
        "Demo: rotting wood stack.",
        "Demo: medical waste box (mock).",
    )

    /**
     * Random points in [minRadiusM]..[maxRadiusM] from centre, at least [minSeparationM] apart
     * (best-effort; relaxes if the annulus is tight).
     */
    private fun randomPinLocationsInAnnulus(
        centerLat: Double,
        centerLon: Double,
        count: Int,
        minRadiusM: Double,
        maxRadiusM: Double,
        minSeparationM: Double,
        rng: Random,
    ): List<Pair<Double, Double>> {
        val out = ArrayList<Pair<Double, Double>>(count)
        var attempts = 0
        val maxAttempts = count * 120
        while (out.size < count && attempts < maxAttempts) {
            attempts++
            val dist = rng.nextDouble(minRadiusM, maxRadiusM)
            val bearing = rng.nextDouble(0.0, 360.0)
            val (lat, lon) = offsetLatLon(centerLat, centerLon, dist, bearing)
            if (out.any { distanceMeters(lat, lon, it.first, it.second) < minSeparationM }) continue
            out.add(lat to lon)
        }
        while (out.size < count) {
            val dist = rng.nextDouble(minRadiusM, maxRadiusM)
            val bearing = rng.nextDouble(0.0, 360.0)
            out.add(offsetLatLon(centerLat, centerLon, dist, bearing))
        }
        return out
    }

    fun buildEntities(
        centerLat: Double,
        centerLon: Double,
        packageName: String,
        localUserId: Int,
        localNickname: String,
    ): List<ReportEntity> {
        require(demoDescriptions.size >= DEMO_ROW_COUNT) { "demoDescriptions must cover DEMO_ROW_COUNT" }
        val rng = Random(
            (System.nanoTime() xor centerLat.toBits() xor centerLon.toBits() xor DEMO_ROW_COUNT.toLong()),
        )
        val imageUri = "android.resource://$packageName/drawable/ic_launcher_foreground"
        val now = System.currentTimeMillis()
        val nearby = randomPinLocationsInAnnulus(
            centerLat = centerLat,
            centerLon = centerLon,
            count = NEARBY_DEMO_PIN_COUNT,
            minRadiusM = NEARBY_INNER_M,
            maxRadiusM = NEARBY_OUTER_M,
            minSeparationM = 55.0,
            rng = rng,
        )
        val far = randomPinLocationsInAnnulus(
            centerLat = centerLat,
            centerLon = centerLon,
            count = FAR_DEMO_PIN_COUNT,
            minRadiusM = FAR_INNER_M,
            maxRadiusM = FAR_OUTER_M,
            minSeparationM = 95.0,
            rng = rng,
        )
        val positions = nearby + far
        val cleanedCount = rng.nextInt(7, 11)
        val cleanedIndices = (0 until DEMO_ROW_COUNT).shuffled(rng).take(cleanedCount).toSet()
        val cleanNick = localNickname.trim().ifEmpty { "Anonymous" }
        return positions.mapIndexed { index, (lat, lon) ->
            val cleaned = index in cleanedIndices
            val desc = demoDescriptions[index]
            val wasteType = WasteTypeCsv.normalize(
                listOf(WasteMenu.types[index % WasteMenu.types.size]),
            )
            ReportEntity(
                imageUri = imageUri,
                latitude = lat,
                longitude = lon,
                wasteType = wasteType,
                description = desc,
                status = if (cleaned) "Cleaned" else "Pending",
                timestamp = now - index * 3_600_000L,
                cleanedImageUri = if (cleaned) imageUri else null,
                cleanedAt = if (cleaned) now - 1_800_000L + index * 60_000L else null,
                cleanerUserId = if (cleaned) localUserId else null,
                cleanerNickname = if (cleaned) cleanNick else "",
                reporterUserId = 1,
                reporterNickname = DEMO_REPORTER_NICKNAME,
            )
        }
    }
}
