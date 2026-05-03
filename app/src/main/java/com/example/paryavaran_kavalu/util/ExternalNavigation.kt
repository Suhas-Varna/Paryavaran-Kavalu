package com.example.paryavaran_kavalu.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Google Maps [travel modes](https://developers.google.com/maps/documentation/urls/get-started#directions-action)
 * via `travelmode=` so users pick walking, driving, transit, etc. — not only driving.
 */
enum class MapsTravelMode(
    val apiParam: String,
    val title: String,
    val description: String,
) {
    WALKING(
        "walking",
        "Walking",
        "Footpaths and walking routes",
    ),
    DRIVING(
        "driving",
        "Driving",
        "Car and on-road vehicle routing",
    ),
    TRANSIT(
        "transit",
        "Transit",
        "Bus, metro, and rail where available",
    ),
    BICYCLING(
        "bicycling",
        "Cycling",
        "Bicycle-friendly paths",
    ),
    TWO_WHEELER(
        "two_wheeler",
        "Two-wheeler",
        "Motorcycle or scooter where supported",
    ),
}

/**
 * Opens directions to [latitude]/[longitude] in an external maps app.
 *
 * Prefers the Google Maps app using the cross-platform Maps URLs API (`travelmode=`).
 * Falls back to `google.navigation:` where applicable, then `geo:`, then OpenStreetMap.
 */
fun Context.launchMapsDirections(latitude: Double, longitude: Double, mode: MapsTravelMode) {
    val directionsUri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=${mode.apiParam}",
    )

    val mapsExplicit = Intent(Intent.ACTION_VIEW, directionsUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        startActivity(mapsExplicit)
        return
    } catch (_: ActivityNotFoundException) {
        val mapsChooser = Intent(Intent.ACTION_VIEW, directionsUri)
        try {
            startActivity(mapsChooser)
            return
        } catch (_: ActivityNotFoundException) {
            // fall through
        }
    }

    val modeChar = when (mode) {
        MapsTravelMode.WALKING -> 'w'
        MapsTravelMode.DRIVING -> 'd'
        MapsTravelMode.BICYCLING -> 'b'
        MapsTravelMode.TRANSIT -> 'l'
        MapsTravelMode.TWO_WHEELER -> 't'
    }
    val googleNav = Uri.parse("google.navigation:q=$latitude,$longitude&mode=$modeChar")
    try {
        startActivity(Intent(Intent.ACTION_VIEW, googleNav).apply { setPackage("com.google.android.apps.maps") })
        return
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, googleNav))
            return
        } catch (_: ActivityNotFoundException) {
            // fall through
        }
    }

    val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    try {
        startActivity(Intent(Intent.ACTION_VIEW, geo))
        return
    } catch (_: ActivityNotFoundException) {
        // ignore
    }

    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.openstreetmap.org/#map=18/$latitude/$longitude"),
            ),
        )
    } catch (_: ActivityNotFoundException) {
        // No browser — nothing else to try.
    }
}

/** Same as [launchMapsDirections] with [MapsTravelMode.WALKING]. */
fun Context.launchWalkingNavigation(latitude: Double, longitude: Double) {
    launchMapsDirections(latitude, longitude, MapsTravelMode.WALKING)
}
