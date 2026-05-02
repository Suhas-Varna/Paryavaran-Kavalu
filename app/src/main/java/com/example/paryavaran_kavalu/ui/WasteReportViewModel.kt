package com.example.paryavaran_kavalu.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paryavaran_kavalu.ParyavaranApplication
import com.example.paryavaran_kavalu.data.MockReportsSeed
import com.example.paryavaran_kavalu.data.ReportEntity
import com.example.paryavaran_kavalu.data.UserTypes
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
        reportDao.insert(
            ReportEntity(
                imageUri = imageUri,
                latitude = latitude,
                longitude = longitude,
                wasteType = wasteType,
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

    /**
     * Inserts ~10 demo reports around [centerLat]/[centerLon] if demo rows are missing.
     * Real reports do not block this (only [MockReportsSeed.DEMO_REPORTER_NICKNAME] rows count).
     */
    fun seedDemoReportsIfNeeded(centerLat: Double, centerLon: Double) {
        viewModelScope.launch {
            demoSeedMutex.withLock {
                val nick = MockReportsSeed.DEMO_REPORTER_NICKNAME
                if (reportDao.countByReporterNickname(nick) >= MockReportsSeed.DEMO_ROW_COUNT) {
                    return@withLock
                }
                if (reportDao.countByReporterNickname(nick) > 0) {
                    reportDao.deleteByReporterNickname(nick)
                }
                val pkg = getApplication<Application>().packageName
                MockReportsSeed.buildEntities(centerLat, centerLon, pkg).forEach { reportDao.insert(it) }
                bumpReportWrites()
            }
        }
    }

    fun markReportCleaned(reportId: Long, cleanedImageUri: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val existing = reportDao.getById(reportId) ?: return@launch
            reportDao.update(
                existing.copy(
                    status = "Cleaned",
                    cleanedImageUri = cleanedImageUri,
                    cleanedAt = System.currentTimeMillis(),
                ),
            )
            userDao.addEcoPoints(EcoKarma.MARK_CLEANED)
            bumpReportWrites()
            onDone()
        }
    }
}

object EcoKarma {
    const val SUBMIT_REPORT = 10
    const val MARK_CLEANED = 25
}
