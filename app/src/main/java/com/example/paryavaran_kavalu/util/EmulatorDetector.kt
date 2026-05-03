package com.example.paryavaran_kavalu.util

import android.os.Build

/** Heuristic match for AVD / emulator images (same checks as OSMDroid layer workaround on MapScreen). */
fun isProbablyEmulator(): Boolean {
    if (Build.FINGERPRINT?.startsWith("generic") == true) return true
    if (Build.FINGERPRINT?.startsWith("unknown") == true) return true
    if (Build.MODEL.contains("Emulator", ignoreCase = true)) return true
    if (Build.MODEL.contains("google_sdk", ignoreCase = true)) return true
    if (Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)) return true
    if (Build.HARDWARE.contains("goldfish", ignoreCase = true)) return true
    if (Build.HARDWARE.contains("ranchu", ignoreCase = true)) return true
    if (Build.PRODUCT.contains("sdk_gphone", ignoreCase = true)) return true
    if (Build.PRODUCT.contains("emulator", ignoreCase = true)) return true
    if (Build.PRODUCT.contains("simulator", ignoreCase = true)) return true
    return false
}
