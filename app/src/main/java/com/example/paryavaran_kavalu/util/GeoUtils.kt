package com.example.paryavaran_kavalu.util

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle distance in metres (Haversine). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dp = Math.toRadians(lat2 - lat1)
    val dl = Math.toRadians(lon2 - lon1)
    val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/** Destination point: start [lat]/[lon], distance [metres], bearing [degrees from north]. */
fun offsetLatLon(lat: Double, lon: Double, distanceM: Double, bearingDeg: Double): Pair<Double, Double> {
    val R = 6371000.0
    val br = Math.toRadians(bearingDeg)
    val lat1 = Math.toRadians(lat)
    val lon1 = Math.toRadians(lon)
    val lat2 = asin(
        sin(lat1) * cos(distanceM / R) +
            cos(lat1) * sin(distanceM / R) * cos(br),
    )
    val lon2 = lon1 + atan2(
        sin(br) * sin(distanceM / R) * cos(lat1),
        cos(distanceM / R) - sin(lat1) * sin(lat2),
    )
    return Math.toDegrees(lat2) to Math.toDegrees(lon2)
}
