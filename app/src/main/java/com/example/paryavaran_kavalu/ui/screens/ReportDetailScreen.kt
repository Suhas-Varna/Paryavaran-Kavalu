package com.example.paryavaran_kavalu.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.components.RoomDebugBottomSheet
import com.example.paryavaran_kavalu.data.WasteTypeCsv
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.example.paryavaran_kavalu.util.MapsTravelMode
import com.example.paryavaran_kavalu.util.launchMapsDirections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Material green 800 — clear “success / complete” affordance. */
private val MarkCompleteGreen = Color(0xFF2E7D32)

@Composable
fun ReportDetailScreen(
    reportId: Long,
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
    onRequestCleanPhoto: (Long) -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val reports by viewModel.reports.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val report = reports.find { it.id == reportId }
    var showRoomDebug by remember { mutableStateOf(false) }

    val dateFmt = rememberDateFormatter()

    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(Modifier.fillMaxSize()) {
        ParyavaranPrimaryAppBar(
            navigation = AppBarNavigation.Back,
            onNavigationClick = onBack,
            navigationContentDescription = "Back to map",
            onDebugClick = { showRoomDebug = true },
            onEcoKarmaClick = onOpenLeaderboard,
            onProfileClick = onOpenProfile,
            profileContentDescription = "Profile — ${userProfile?.nickname ?: "you"}",
            title = {
                val r = report
                if (r == null) {
                    Text(
                        text = "Incident",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        color = onPrimary,
                    )
                } else {
                    val cleaned = r.status.trim().equals("Cleaned", ignoreCase = true)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = WasteTypeCsv.formatDisplay(r.wasteType).ifBlank { "Incident" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            color = onPrimary,
                        )
                        if (cleaned) {
                            Text(
                                text = "Cleaned",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFB9F6CA),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            },
        )
        if (report == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "This report is no longer available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val statusRaw = report.status.trim()
            val isCleaned = statusRaw.equals("Cleaned", ignoreCase = true)
            val showMarkCleanedButton = !isCleaned
            val statusDisplay = when {
                isCleaned -> "Cleaned"
                statusRaw.isEmpty() || statusRaw.equals("Pending", ignoreCase = true) -> "Pending pickup"
                else -> statusRaw
            }
            val statusRowIcon: ImageVector = if (isCleaned) {
                Icons.Outlined.CheckCircle
            } else {
                Icons.Outlined.HourglassEmpty
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(288.dp)
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
                ) {
                    AsyncImage(
                        model = report.imageUri,
                        contentDescription = "Before: reported waste photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.45f),
                                    0.5f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.35f),
                                ),
                            ),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = WasteTypeCsv.formatDisplay(report.wasteType),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                            )
                            Text(
                                text = "Report #${report.id}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                            if (report.description.isNotBlank()) {
                                Text(
                                    text = report.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                        StatusChip(isCleaned = isCleaned)
                    }
                }

                if (isCleaned) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MarkCompleteGreen.copy(alpha = 0.14f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MarkCompleteGreen,
                                modifier = Modifier.size(28.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Cleanup verified",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Status is cleaned. The after photo is stored for this report.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    SectionTitle(text = "Details")

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            val cleanedByName: String? = if (isCleaned) {
                                report.cleanerNickname.trim().ifBlank {
                                    userProfile?.nickname?.trim()?.takeIf { it.isNotEmpty() } ?: "Anonymous"
                                }
                            } else {
                                null
                            }
                            val metaRows = listOfNotNull(
                                Triple(statusRowIcon, "Status", statusDisplay),
                                Triple(
                                    Icons.Outlined.CoPresent,
                                    "Reported by",
                                    report.reporterNickname.ifBlank { "Anonymous" },
                                ),
                                cleanedByName?.let { who ->
                                    Triple(
                                        Icons.Outlined.VerifiedUser,
                                        "Cleaned by",
                                        who,
                                    )
                                },
                                Triple(
                                    Icons.Outlined.Category,
                                    "Waste category",
                                    WasteTypeCsv.formatDisplay(report.wasteType),
                                ),
                                Triple(
                                    Icons.Outlined.Notes,
                                    "Notes / description",
                                    report.description.ifBlank { "—" },
                                ),
                                Triple(
                                    Icons.Outlined.CalendarMonth,
                                    "Reported on",
                                    dateFmt.format(Date(report.timestamp)),
                                ),
                                Triple(
                                    Icons.Outlined.LocationOn,
                                    "Coordinates (stored)",
                                    String.format(Locale.US, "%.5f, %.5f", report.latitude, report.longitude),
                                ),
                            )
                            metaRows.forEachIndexed { index, (icon, label, value) ->
                                MetaRow(icon = icon, label = label, value = value)
                                if (index < metaRows.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    )
                                }
                            }
                        }
                    }

                    if (isCleaned) {
                        SectionTitle(text = "Before & after")
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "Before: the photo at the top of this screen is the original report.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                report.cleanedAt?.let { cleanedAt ->
                                    Text(
                                        text = "Marked clean on ${dateFmt.format(Date(cleanedAt))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                report.cleanedImageUri?.let { uri ->
                                    Text(
                                        text = "After (verification photo)",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "After cleaning verification photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Text(
                                        text = "Stored locally with this report for future comparison (e.g. AI before/after checks).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    SectionTitle(text = "Get directions")
                    Text(
                        text = "Pick a travel mode, then open turn-by-turn navigation in Maps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Navigation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "Navigate to this location",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MapsTravelMode.entries.forEach { mode ->
                            Surface(
                                onClick = {
                                    context.launchMapsDirections(
                                        report.latitude,
                                        report.longitude,
                                        mode,
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 1.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Icon(
                                        imageVector = modeIcon(mode),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    NavigationStartBadge()
                                }
                            }
                        }
                    }

                    if (showMarkCleanedButton) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onRequestCleanPhoto(report.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MarkCompleteGreen,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Mark as cleaned",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(
                            text = "You’ll take a quick photo to confirm the spot is cleaned.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
    RoomDebugBottomSheet(
        visible = showRoomDebug,
        onDismiss = { showRoomDebug = false },
        user = userProfile,
        reports = reports,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun NavigationStartBadge() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Navigation,
            contentDescription = "Start navigation",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("EEEE, dd MMM yyyy · HH:mm", Locale.getDefault())
    }
}

@Composable
private fun StatusChip(isCleaned: Boolean) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(if (isCleaned) "Cleaned" else "Pending pickup")
        },
        leadingIcon = {
            Icon(
                imageVector = if (isCleaned) Icons.Outlined.CheckCircle else Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = if (isCleaned) {
                Color(0xFF1B5E20).copy(alpha = 0.85f)
            } else {
                Color(0xFFE65100).copy(alpha = 0.9f)
            },
            disabledLabelColor = Color.White,
            disabledLeadingIconContentColor = Color.White,
        ),
        border = null,
    )
}

@Composable
private fun MetaRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun modeIcon(mode: MapsTravelMode): ImageVector {
    return when (mode) {
        MapsTravelMode.WALKING -> Icons.Outlined.DirectionsWalk
        MapsTravelMode.DRIVING -> Icons.Outlined.DirectionsCar
        MapsTravelMode.TRANSIT -> Icons.Outlined.DirectionsTransit
        MapsTravelMode.BICYCLING -> Icons.Outlined.DirectionsBike
        MapsTravelMode.TWO_WHEELER -> Icons.Outlined.TwoWheeler
    }
}
