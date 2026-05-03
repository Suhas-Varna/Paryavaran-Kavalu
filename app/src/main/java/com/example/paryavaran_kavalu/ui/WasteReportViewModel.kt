package com.example.paryavaran_kavalu.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paryavaran_kavalu.ParyavaranApplication
import com.example.paryavaran_kavalu.data.MockReportsSeed
import com.example.paryavaran_kavalu.data.ReportEntity
import com.example.paryavaran_kavalu.data.UserTypes
import com.example.paryavaran_kavalu.data.WasteTypeCsv
import com.example.paryavaran_kavalu.util.isProbablyEmulator
import com.example.paryavaran_kavalu.util.offsetLatLon
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WasteReportViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ParyavaranApplication).database
    private val reportDao = db.reportDao()
    private val userDao = db.userDao()

    /**
     * Stays active while navigating Camera → Report so the map still receives new rows
     * when you pop back to the map.
     */
    val reports = reportDao.getAllReports()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    private val _reportWriteGeneration = MutableStateFlow(0L)
    val reportWriteGeneration: StateFlow<Long> = _reportWriteGeneration.asStateFlow()

    private fun bumpReportWrites() {
        _reportWriteGeneration.update { it + 1L }
    }

    val userProfile = userDao.observeUser()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    var capturedImageUri by mutableStateOf<String?>(null)
        private set

    fun updateCapturedImageUri(uriString: String?) {
        capturedImageUri = uriString
    }

    /** Attempts to spend Eco‑karma for a reward; [onComplete] is true if the DB deduction succeeded. */
    fun redeemEcoPoints(cost: Int, onComplete: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            if (cost <= 0) {
                onComplete(false)
                return@launch
            }
            val updated = userDao.tryDeductEcoPoints(cost)
            onComplete(updated > 0)
        }
    }

    fun updateProfile(nickname: String, userType: String, bio: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val n = nickname.trim()
            if (n.isEmpty()) return@launch
            val t = userType.trim().takeIf { it in UserTypes.all } ?: UserTypes.REPORTER
            userDao.updateProfile(nickname = n, userType = t, bio = bio.trim())
            onDone()
        }
    }

    suspend fun submitReport(
        imageUri: String,
        latitude: Double,
        longitude: Double,
        wasteType: String,
        description: String,
        status: String = "Pending",
    ) {
        val user = userDao.getUser()
        val nick = user?.nickname?.trim().orEmpty().ifEmpty { "Anonymous" }
        val uid = user?.userId ?: 1
        val (lat, lon) = applyEmulatorDebugLocationOffset(latitude, longitude)
        val wasteStored = WasteTypeCsv.normalize(WasteTypeCsv.parseStored(wasteType))
        reportDao.insert(
            ReportEntity(
                imageUri = imageUri,
                latitude = lat,
                longitude = lon,
                wasteType = wasteStored,
                description = description,
                status = status,
                timestamp = System.currentTimeMillis(),
                reporterUserId = uid,
                reporterNickname = nick,
            ),
        )
        userDao.addEcoPoints(EcoKarma.SUBMIT_REPORT)
        bumpReportWrites()
    }

    private val demoSeedMutex = Mutex()

    private val seedPrefs =
        application.getSharedPreferences("paryavaran_map_seed", Context.MODE_PRIVATE)

    /**
     * First launch after install (or cleared app data): clears [reports] and inserts a nearby demo
     * cluster around [centerLat]/[centerLon] (same categories as the report form / map chips).
     * Later launches only top up demo rows if needed, without wiping user-submitted reports.
     */
    fun seedDemoReportsIfNeeded(centerLat: Double, centerLon: Double) {
        viewModelScope.launch {
            demoSeedMutex.withLock {
                val pkg = getApplication<Application>().packageName
                val demoClusterDone = seedPrefs.getBoolean(PREF_DEMO_CLUSTER_APPLIED, false)
                if (!demoClusterDone) {
                    seedPrefs.edit().putBoolean(PREF_DEMO_CLUSTER_APPLIED, true).apply()
                    reportDao.deleteAllReports()
                    MockReportsSeed.buildEntities(centerLat, centerLon, pkg)
                        .forEach { reportDao.insert(it) }
                    bumpReportWrites()
                    return@withLock
                }
                val nick = MockReportsSeed.DEMO_REPORTER_NICKNAME
                if (reportDao.countByReporterNickname(nick) >= MockReportsSeed.DEMO_ROW_COUNT) {
                    return@withLock
                }
                if (reportDao.countByReporterNickname(nick) > 0) {
                    reportDao.deleteByReporterNickname(nick)
                }
                MockReportsSeed.buildEntities(centerLat, centerLon, pkg).forEach { reportDao.insert(it) }
                bumpReportWrites()
            }
        }
    }

    /**
     * On emulators, GPS is often a fixed point; nudge stored coordinates by a random 1–25 km so
     * pins stay in a debuggable band near the “current” fix without stacking on one spot.
     */
    private fun applyEmulatorDebugLocationOffset(latitude: Double, longitude: Double): Pair<Double, Double> {
        if (!isProbablyEmulator()) return latitude to longitude
        val km = Random.nextInt(1, 26)
        val distanceM = km * 1000.0
        val bearingDeg = Random.nextDouble(360.0)
        return offsetLatLon(latitude, longitude, distanceM, bearingDeg)
    }

    private companion object {
        const val PREF_DEMO_CLUSTER_APPLIED = "demo_cluster_near_center_applied_v1"
    }

    /**
     * @param onComplete Invoked on the main thread after DB work. Passes [EcoKarma.MARK_CLEANED]
     * when the row was updated and points were awarded; `null` if the report was missing or
     * already cleaned (no duplicate reward).
     */
    fun markReportCleaned(
        reportId: Long,
        cleanedImageUri: String,
        onComplete: (pointsEarned: Int?) -> Unit = {},
    ) {
        viewModelScope.launch {
            val existing = reportDao.getById(reportId)
            if (existing == null) {
                onComplete(null)
                return@launch
            }
            if (existing.status.trim().equals("Cleaned", ignoreCase = true)) {
                onComplete(null)
                return@launch
            }
            val now = System.currentTimeMillis()
            reportDao.update(
                existing.copy(
                    status = "Cleaned",
                    cleanedImageUri = cleanedImageUri.trim(),
                    cleanedAt = now,
                ),
            )
            userDao.addEcoPoints(EcoKarma.MARK_CLEANED)
            bumpReportWrites()
            onComplete(EcoKarma.MARK_CLEANED)
        }
    }
}

object EcoKarma {
    const val SUBMIT_REPORT = 10
    const val MARK_CLEANED = 25
}
