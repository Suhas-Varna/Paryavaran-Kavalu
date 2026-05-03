package com.example.paryavaran_kavalu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paryavaran_kavalu.data.ReportEntity
import com.example.paryavaran_kavalu.data.UserEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDebugBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    user: UserEntity?,
    reports: List<ReportEntity>,
) {
    if (!visible) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        RoomDebugSheetContent(
            user = user,
            reports = reports,
            onClose = onDismiss,
        )
    }
}

@Composable
fun RoomDebugSheetContent(
    user: UserEntity?,
    reports: List<ReportEntity>,
    onClose: () -> Unit,
) {
    val tableScroll = rememberScrollState()
    val bodyScroll = rememberScrollState()
    val dateFmt = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(bodyScroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Room (debug)", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tables user_profile and reports — verify submits and cleanup fields (cleanerUserId, cleanerNickname, cleanedAt).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("user_profile", style = MaterialTheme.typography.titleMedium)
        UserProfileDebugTable(user)

        Spacer(Modifier.height(4.dp))
        Text("reports (${reports.size})", style = MaterialTheme.typography.titleMedium)
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(tableScroll),
        ) {
            Column {
                ReportsDebugTableHeader()
                HorizontalDivider()
                if (reports.isEmpty()) {
                    Text(
                        "No rows yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    reports.forEach { report ->
                        ReportsDebugTableRow(report, dateFmt)
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun UserProfileDebugTable(user: UserEntity?) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            DebugCell("userId", 40, isHeader = true)
            DebugCell("nickname", 100, isHeader = true)
            DebugCell("type", 80, isHeader = true)
            DebugCell("eco", 52, isHeader = true)
        }
        HorizontalDivider()
        if (user == null) {
            Text(
                "No user row (open Profile to create one).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            ) {
                DebugCell(user.userId.toString(), 40, isHeader = false)
                DebugCell(user.nickname, 100, isHeader = false)
                DebugCell(user.userType, 80, isHeader = false)
                DebugCell(user.ecoPoints.toString(), 52, isHeader = false)
            }
            Text(
                text = "bio: ${user.bio.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun ReportsDebugTableHeader() {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        DebugCell("id", 44, isHeader = true)
        DebugCell("lat", 72, isHeader = true)
        DebugCell("lon", 72, isHeader = true)
        DebugCell("waste", 64, isHeader = true)
        DebugCell("description", 160, isHeader = true)
        DebugCell("status", 64, isHeader = true)
        DebugCell("reporter", 88, isHeader = true)
        DebugCell("time", 108, isHeader = true)
        DebugCell("imageUri (trunc.)", 120, isHeader = true)
        DebugCell("cleanedAt", 108, isHeader = true)
        DebugCell("cleanerUserId", 88, isHeader = true)
        DebugCell("cleanerNickname", 104, isHeader = true)
        DebugCell("cleanedImg (trunc.)", 120, isHeader = true)
    }
}

@Composable
private fun ReportsDebugTableRow(
    report: ReportEntity,
    dateFmt: SimpleDateFormat,
) {
    val uriShort = report.imageUri.let { u ->
        if (u.length <= 28) u else u.take(12) + "…" + u.takeLast(12)
    }
    val cleanedUriShort = report.cleanedImageUri?.let { u ->
        if (u.length <= 28) u else u.take(12) + "…" + u.takeLast(12)
    } ?: "—"
    val cleanedAtStr = report.cleanedAt?.let { dateFmt.format(Date(it)) } ?: "—"
    Row(Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
        DebugCell(report.id.toString(), 44, isHeader = false)
        DebugCell(
            String.format(Locale.US, "%.4f", report.latitude),
            72,
            isHeader = false,
        )
        DebugCell(
            String.format(Locale.US, "%.4f", report.longitude),
            72,
            isHeader = false,
        )
        DebugCell(report.wasteType, 64, isHeader = false)
        DebugCell(
            report.description.trim().ifBlank { "—" },
            160,
            isHeader = false,
        )
        DebugCell(report.status, 64, isHeader = false)
        DebugCell(report.reporterNickname.ifBlank { "—" }, 88, isHeader = false)
        DebugCell(dateFmt.format(Date(report.timestamp)), 108, isHeader = false)
        DebugCell(uriShort, 120, isHeader = false)
        DebugCell(cleanedAtStr, 108, isHeader = false)
        DebugCell(report.cleanerUserId?.toString() ?: "—", 88, isHeader = false)
        DebugCell(report.cleanerNickname.trim().ifBlank { "—" }, 104, isHeader = false)
        DebugCell(cleanedUriShort, 120, isHeader = false)
    }
}

@Composable
private fun DebugCell(
    text: String,
    widthDp: Int,
    isHeader: Boolean,
) {
    Text(
        text = text,
        style = if (isHeader) {
            MaterialTheme.typography.labelMedium
        } else {
            MaterialTheme.typography.bodySmall
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(min = widthDp.dp)
            .width(widthDp.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}
