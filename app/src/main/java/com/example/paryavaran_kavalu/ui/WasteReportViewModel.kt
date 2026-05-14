package com.example.paryavaran_kavalu.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paryavaran_kavalu.ParyavaranApplication
import androidx.room.withTransaction
import com.example.paryavaran_kavalu.data.MockReportsSeed
import com.example.paryavaran_kavalu.data.RedemptionTransactionEntity
import com.example.paryavaran_kavalu.data.ReportEntity
import com.example.paryavaran_kavalu.data.UserTypes
import com.example.paryavaran_kavalu.data.WasteTypeCsv
import com.example.paryavaran_kavalu.util.isProbablyEmulator
import com.example.paryavaran_kavalu.util.distanceMeters
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
    private val redeemItemDao = db.redeemItemDao()
    private val redemptionDao = db.redemptionDao()

    init {
        viewModelScope.launch {
            syncReportNicknamesWithProfile()
        }
    }

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

    val redeemCatalog = redeemItemDao.observeAll()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val claimedRewards = redemptionDao.observeClaimedRewards(userId = 1)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    var capturedImageUri by mutableStateOf<String?>(null)
        private set

    fun updateCapturedImageUri(uriString: String?) {
        capturedImageUri = uriString
    }

    /**
     * Atomically deducts [RedeemItemEntity.costPoints] for [itemId], increments redemption count,
     * and returns success only if the user could afford the item.
     */
    fun redeemReward(itemId: Long, onComplete: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val item = redeemItemDao.getById(itemId)
            if (item == null || item.costPoints <= 0) {
                onComplete(false)
                return@launch
            }
            val uid = userDao.getUser()?.userId ?: 1
            val cost = item.costPoints
            val ok = try {
                db.withTransaction {
                    if (userDao.tryDeductEcoPoints(cost) == 0) return@withTransaction false
                    val existing = redemptionDao.getTransaction(uid, itemId)
                    if (existing == null) {
                        redemptionDao.insert(
                            RedemptionTransactionEntity(
                                userId = uid,
                                itemId = itemId,
                                timesRedeemed = 1,
                            ),
                        )
                    } else {
                        redemptionDao.update(
                            existing.copy(timesRedeemed = existing.timesRedeemed + 1),
                        )
                    }
                    true
                }
            } catch (_: Exception) {
                false
            }
            onComplete(ok)
        }
    }

    /** Single role on disk: everyone can report and clean; no role picker in the UI. */
    fun updateProfile(nickname: String, bio: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val n = nickname.trim()
            if (n.isEmpty()) return@launch
            userDao.updateProfile(nickname = n, userType = UserTypes.BOTH, bio = bio.trim())
            syncReportNicknamesWithProfile()
            onDone()
        }
    }

    /**
     * Copies [UserEntity.nickname] into report rows for the local user so DB mirrors profile and
     * metadata screens stay consistent after renames. Demo “Demo patrol” reporter rows are skipped.
     */
    private suspend fun syncReportNicknamesWithProfile() {
        val user = userDao.getUser() ?: return
        val nick = user.nickname.trim().ifEmpty { "Anonymous" }
        reportDao.syncCleanerNicknamesForLocalUser(nick)
        reportDao.syncReporterNicknamesForLocalUser(nick, MockReportsSeed.DEMO_REPORTER_NICKNAME)
        bumpReportWrites()
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
                val user = userDao.getUser()
                val profileNick = user?.nickname?.trim().orEmpty().ifEmpty { "Anonymous" }
                val uid = user?.userId ?: 1
                val demoReporterNick = MockReportsSeed.DEMO_REPORTER_NICKNAME

                if (!seedPrefs.getBoolean(PREF_DEMO_RESEED_SPREAD_V2, false)) {
                    if (reportDao.countByReporterNickname(demoReporterNick) > 0) {
                        val priorSpread = reportDao.countDemoCleanupsForCleaner(demoReporterNick, uid)
                        reportDao.deleteByReporterNickname(demoReporterNick)
                        val spreadBuilt = MockReportsSeed.buildEntities(
                            centerLat,
                            centerLon,
                            pkg,
                            uid,
                            profileNick,
                        )
                        spreadBuilt.forEach { reportDao.insert(it) }
                        applyDemoCleanupEcoDelta(priorSpread, spreadBuilt, uid)
                        bumpReportWrites()
                        syncReportNicknamesWithProfile()
                    }
                    seedPrefs.edit().putBoolean(PREF_DEMO_RESEED_SPREAD_V2, true).apply()
                    if (reportDao.countByReporterNickname(demoReporterNick) >= MockReportsSeed.DEMO_ROW_COUNT) {
                        return@withLock
                    }
                }

                if (!seedPrefs.getBoolean(PREF_DEMO_NEAR_RING_V3, false)) {
                    if (reportDao.countByReporterNickname(demoReporterNick) > 0) {
                        val priorNear = reportDao.countDemoCleanupsForCleaner(demoReporterNick, uid)
                        reportDao.deleteByReporterNickname(demoReporterNick)
                        val nearBuilt = MockReportsSeed.buildEntities(
                            centerLat,
                            centerLon,
                            pkg,
                            uid,
                            profileNick,
                        )
                        nearBuilt.forEach { reportDao.insert(it) }
                        applyDemoCleanupEcoDelta(priorNear, nearBuilt, uid)
                        bumpReportWrites()
                        syncReportNicknamesWithProfile()
                    }
                    seedPrefs.edit().putBoolean(PREF_DEMO_NEAR_RING_V3, true).apply()
                    if (reportDao.countByReporterNickname(demoReporterNick) >= MockReportsSeed.DEMO_ROW_COUNT) {
                        return@withLock
                    }
                }

                val demoClusterDone = seedPrefs.getBoolean(PREF_DEMO_CLUSTER_APPLIED, false)
                if (!demoClusterDone) {
                    seedPrefs.edit().putBoolean(PREF_DEMO_CLUSTER_APPLIED, true).apply()
                    val priorDemoCleanups = reportDao.countDemoCleanupsForCleaner(demoReporterNick, uid)
                    reportDao.deleteAllReports()
                    val built = MockReportsSeed.buildEntities(
                        centerLat,
                        centerLon,
                        pkg,
                        uid,
                        profileNick,
                    )
                    built.forEach { reportDao.insert(it) }
                    applyDemoCleanupEcoDelta(priorDemoCleanups, built, uid)
                    bumpReportWrites()
                    syncReportNicknamesWithProfile()
                    return@withLock
                }
                val existingDemoRows = reports.value.filter { it.reporterNickname == demoReporterNick }
                if (existingDemoRows.size >= MockReportsSeed.DEMO_ROW_COUNT) {
                    val avgLat = existingDemoRows.sumOf { it.latitude } / existingDemoRows.size
                    val avgLon = existingDemoRows.sumOf { it.longitude } / existingDemoRows.size
                    val centerDistanceM = distanceMeters(centerLat, centerLon, avgLat, avgLon)
                    if (centerDistanceM <= DEMO_RESEED_DISTANCE_METERS) {
                        return@withLock
                    }
                }
                val priorDemoCleanups = reportDao.countDemoCleanupsForCleaner(demoReporterNick, uid)
                if (reportDao.countByReporterNickname(demoReporterNick) > 0) {
                    reportDao.deleteByReporterNickname(demoReporterNick)
                }
                val built = MockReportsSeed.buildEntities(
                    centerLat,
                    centerLon,
                    pkg,
                    uid,
                    profileNick,
                )
                built.forEach { reportDao.insert(it) }
                applyDemoCleanupEcoDelta(priorDemoCleanups, built, uid)
                bumpReportWrites()
                syncReportNicknamesWithProfile()
            }
        }
    }

    private suspend fun applyDemoCleanupEcoDelta(
        previousUserDemoCleanups: Int,
        newRows: List<ReportEntity>,
        userId: Int,
    ) {
        val newUserCleanups = newRows.count { row ->
            row.cleanerUserId == userId &&
                row.status.trim().equals("Cleaned", ignoreCase = true)
        }
        val delta = newUserCleanups - previousUserDemoCleanups
        if (delta != 0) {
            userDao.addEcoPoints(delta * EcoKarma.MARK_CLEANED)
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
        const val PREF_DEMO_RESEED_SPREAD_V2 = "demo_reseed_spread_v2_done"
        const val PREF_DEMO_NEAR_RING_V3 = "demo_near_ring_v3_done"
        const val DEMO_RESEED_DISTANCE_METERS = 8_000.0
    }

    /**
     * @param onComplete Invoked on the main thread after DB work. Passes [EcoKarma.MARK_CLEANED] points
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
            val user = userDao.getUser()
            val uid = user?.userId ?: 1
            val cleanNick = user?.nickname?.trim().orEmpty().ifEmpty { "Anonymous" }
            reportDao.update(
                existing.copy(
                    status = "Cleaned",
                    cleanedImageUri = cleanedImageUri.trim(),
                    cleanedAt = now,
                    cleanerUserId = uid,
                    cleanerNickname = cleanNick,
                ),
            )
            userDao.addEcoPoints(EcoKarma.MARK_CLEANED)
            bumpReportWrites()
            onComplete(EcoKarma.MARK_CLEANED)
        }
    }
}

object EcoKarma {
    const val SUBMIT_REPORT = 20
    const val MARK_CLEANED = 30
}
