package com.example.paryavaran_kavalu.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens turn-by-turn **walking** navigation in an external app (no Maps SDK / billing).
 *
 * 1. [Google Maps app](https://developers.google.com/maps/documentation/urls/android-intents)
 *    `google.navigation:q=lat,lng&mode=w` — same idea as Flutter’s `google.navigation:...&mode=w`.
 * 2. If Google Maps is not installed: generic `geo:` intent (user’s default maps app).
 * 3. Last resort: OpenStreetMap website centered on the point (free, browser).
 */
fun Context.launchWalkingNavigation(latitude: Double, longitude: Double) {
    val googleNav = Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
    val googleIntent = Intent(Intent.ACTION_VIEW, googleNav).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        startActivity(googleIntent)
        return
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, googleNav))
            return
        } catch (_: ActivityNotFoundException) {
            // continue to fallbacks
        }
    }

    val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    try {
        startActivity(Intent(Intent.ACTION_VIEW, geo))
        return
    } catch (_: ActivityNotFoundException) {
        // ignore
    }

    val osmWeb = Uri.parse("https://www.openstreetmap.org/#map=18/$latitude/$longitude")
    startActivity(Intent(Intent.ACTION_VIEW, osmWeb))
}
