package com.example.paryavaran_kavalu.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paryavaran_kavalu.ParyavaranApplication
import com.example.paryavaran_kavalu.data.ReportEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WasteReportViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as ParyavaranApplication).database
    private val reportDao = db.reportDao()
    private val userDao = db.userDao()

    val reports = reportDao.getAllReports()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

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

    suspend fun submitReport(
        imageUri: String,
        latitude: Double,
        longitude: Double,
        wasteType: String,
        description: String,
        status: String = "Pending",
    ) {
        reportDao.insert(
            ReportEntity(
                imageUri = imageUri,
                latitude = latitude,
                longitude = longitude,
                wasteType = wasteType,
                description = description,
                status = status,
                timestamp = System.currentTimeMillis(),
            ),
        )
        userDao.addEcoPoints(EcoKarma.SUBMIT_REPORT)
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
            onDone()
        }
    }
}

object EcoKarma {
    const val SUBMIT_REPORT = 10
    const val MARK_CLEANED = 25
}
