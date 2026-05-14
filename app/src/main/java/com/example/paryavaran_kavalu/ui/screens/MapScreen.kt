package com.example.paryavaran_kavalu.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.example.paryavaran_kavalu.util.isProbablyEmulator
import com.example.paryavaran_kavalu.R
import com.example.paryavaran_kavalu.data.ReportEntity
import com.example.paryavaran_kavalu.data.WasteMenu
import com.example.paryavaran_kavalu.data.WasteTypeCsv
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.ParyavaranAppBarTitle
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.EcoKarma
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.example.paryavaran_kavalu.util.distanceMeters
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Leaderboard “user map”: reports filed or cleanups verified **by this player only**.
 *
 * When matching by user id, we also require the stored nickname on the row to match the
 * leaderboard name. Otherwise every row with `reporterUserId == 1` would appear for user 1 —
 * including demo pins filed as “Demo patrol”, etc.
 */
private fun matchesReporterOnUserMap(
    r: ReportEntity,
    userIdFilter: Int?,
    filterNick: String,
): Boolean {
    val nick = filterNick.trim()
    val byUserId = userIdFilter != null && userIdFilter >= 1
    return if (byUserId) {
        nick.isNotEmpty() &&
            r.reporterUserId == userIdFilter &&
            r.reporterNickname.trim().equals(nick, ignoreCase = true)
    } else {
        nick.isNotEmpty() && r.reporterNickname.trim().equals(nick, ignoreCase = true)
    }
}

private fun matchesCleanerOnUserMap(
    r: ReportEntity,
    userIdFilter: Int?,
    filterNick: String,
): Boolean {
    if (!r.status.trim().equals("Cleaned", ignoreCase = true)) return false
    val nick = filterNick.trim()
    val byUserId = userIdFilter != null && userIdFilter >= 1
    return if (byUserId) {
        nick.isNotEmpty() &&
            r.cleanerUserId == userIdFilter &&
            r.cleanerNickname.trim().equals(nick, ignoreCase = true)
    } else {
        nick.isNotEmpty() && r.cleanerNickname.trim().equals(nick, ignoreCase = true)
    }
}

private fun matchesUserMapHistory(
    r: ReportEntity,
    userIdFilter: Int?,
    filterNick: String,
): Boolean =
    matchesReporterOnUserMap(r, userIdFilter, filterNick) ||
        matchesCleanerOnUserMap(r, userIdFilter, filterNick)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    viewModel: WasteReportViewModel,
    modifier: Modifier = Modifier,
    /**
     * When non-null, the map is read-only: pins **filed or cleaned under this leaderboard name**
     * (ids plus nickname disambiguate shared local user ids). Use `-1` with
     * [cleanerNicknameFilter] for nickname-only matching.
     */
    cleanerUserIdFilter: Int? = null,
    /** Display name in the header; with user id `-1`, used to match reporter/cleaner nicknames. */
    cleanerNicknameFilter: String? = null,
    onReportIncident: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onRequestCleanPhoto: (Long) -> Unit = {},
    onBackToHome: () -> Unit = {},
    onOpenIncidentDetail: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val reports by viewModel.reports.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val mapRefreshGen by viewModel.reportWriteGeneration.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)

    var currentLocation by remember {
        mutableStateOf(GeoPoint(12.9716, 77.5946))
    }

    val radiusOptions = remember {
        listOf(
            "3 km" to 3f,
            "5 km" to 5f,
            "10 km" to 10f,
            "All" to Float.MAX_VALUE,
        )
    }
    var radiusIndex by remember { mutableIntStateOf(radiusOptions.lastIndex) }
    /** Empty = show every waste type. Otherwise pins match only if the report lists **all** selected types. */
    var selectedWasteFilters by remember { mutableStateOf(emptySet<String>()) }
    val radiusChipScroll = rememberScrollState()
    val wasteChipScroll = rememberScrollState()

    val userHistoryMode = cleanerUserIdFilter != null
    val filterNick = cleanerNicknameFilter?.trim().orEmpty()

    val filteredReports = remember(
        reports,
        currentLocation,
        radiusIndex,
        selectedWasteFilters,
        radiusOptions,
        cleanerUserIdFilter,
        cleanerNicknameFilter,
    ) {
        if (cleanerUserIdFilter != null) {
            return@remember reports
                .filter { r ->
                    matchesUserMapHistory(r, cleanerUserIdFilter, filterNick)
                }
                .sortedByDescending { max(it.cleanedAt ?: 0L, it.timestamp) }
        }
        val capKm = radiusOptions[radiusIndex].second
        val distanceLimited = radiusIndex != radiusOptions.lastIndex
        val lat = currentLocation.latitude
        val lon = currentLocation.longitude
        reports
            .map { r -> r to distanceMeters(lat, lon, r.latitude, r.longitude) }
            .filter { (r, dM) ->
                if (distanceLimited && dM > capKm * 1000.0) return@filter false
                if (selectedWasteFilters.isNotEmpty()) {
                    if (!WasteTypeCsv.containsAllCategories(r.wasteType, selectedWasteFilters)) {
                        return@filter false
                    }
                }
                true
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    /** Any Room change to reports (or explicit refresh gen) — drives OSMDroid overlay updates. */
    val mapSyncToken = remember(reports, mapRefreshGen) {
        buildString {
            append(mapRefreshGen)
            append('|')
            reports.forEach { r ->
                append(r.id)
                append(':')
                append(r.status)
                append(':')
                append(r.latitude)
                append(':')
                append(r.longitude)
                append(':')
                append(r.wasteType)
                append(':')
                append(r.description)
                append(':')
                append(r.timestamp)
                append(':')
                append(r.cleanedAt ?: 0L)
                append(':')
                append(r.cleanerNickname)
                append(':')
                append(r.cleanerUserId ?: -1)
                append(';')
            }
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    /**
     * Bumps after each location pass (or once when fine location is denied) so the map camera
     * recenters only after [currentLocation] matches that pass. Without this, the first center
     * locks onto the default Bangalore point while Fused Location later moves pins (and the user
     * marker) to the emulator/device fix — pins look "missing".
     */
    var locationSettlementVersion by remember { mutableIntStateOf(0) }
    var lastSettlementUsedForCamera by remember { mutableIntStateOf(-1) }

    LaunchedEffect(mapViewRef, filteredReports, currentLocation, mapSyncToken) {
        val map = mapViewRef ?: return@LaunchedEffect
        map.applyReportOverlays(
            context = context,
            filteredReports = filteredReports,
            userLocation = currentLocation,
        ) { tapped -> onOpenIncidentDetail(tapped.id) }
    }

    LaunchedEffect(mapViewRef, filteredReports, userHistoryMode) {
        if (!userHistoryMode) return@LaunchedEffect
        val map = mapViewRef ?: return@LaunchedEffect
        val pts = filteredReports
        if (pts.isEmpty()) return@LaunchedEffect
        val avgLat = pts.sumOf { it.latitude } / pts.size
        val avgLon = pts.sumOf { it.longitude } / pts.size
        map.controller.animateTo(GeoPoint(avgLat, avgLon))
        map.controller.setZoom(13.0)
    }

    LaunchedEffect(mapViewRef, currentLocation, userHistoryMode, locationSettlementVersion) {
        if (userHistoryMode) return@LaunchedEffect
        val map = mapViewRef ?: return@LaunchedEffect
        val v = locationSettlementVersion
        if (v <= 0 || v == lastSettlementUsedForCamera) return@LaunchedEffect
        lastSettlementUsedForCamera = v
        map.controller.animateTo(currentLocation)
        map.controller.setZoom(16.5)
    }

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        locationPermissionGranted = granted
    }

    // Show UI first, then create MapView after transition — avoids race with OSMDroid + Nav.
    var mapReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        mapReady = true
    }

    // Request location only after the map view is in place (fewer ANRs on real devices).
    LaunchedEffect(mapReady) {
        if (!mapReady) return@LaunchedEffect
        delay(300)
        if (!locationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var locationRefreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(activity, locationPermissionGranted) {
        if (!locationPermissionGranted) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) locationRefreshTick++
            }
            activity.lifecycle.addObserver(observer)
            onDispose { activity.lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(locationPermissionGranted, locationRefreshTick) {
        if (!locationPermissionGranted) {
            // Let the nearby map use the default centre when location is unavailable.
            if (locationSettlementVersion == 0) {
                locationSettlementVersion = 1
            }
            return@LaunchedEffect
        }
        val fused = LocationServices.getFusedLocationProviderClient(context)
        try {
            var loc = fused.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            ).await()
            if (loc == null) {
                loc = fused.lastLocation.await()
            }
            loc?.let {
                currentLocation = GeoPoint(it.latitude, it.longitude)
                if (!userHistoryMode) {
                    viewModel.seedDemoReportsIfNeeded(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } finally {
            locationSettlementVersion++
        }
    }

    /** If fused location stays null (some emulators), still seed once around the map centre. */
    LaunchedEffect(mapReady, userHistoryMode, locationPermissionGranted) {
        if (!mapReady || userHistoryMode || !locationPermissionGranted || !isProbablyEmulator()) return@LaunchedEffect
        delay(4500)
        viewModel.seedDemoReportsIfNeeded(currentLocation.latitude, currentLocation.longitude)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        ParyavaranPrimaryAppBar(
            navigation = AppBarNavigation.Back,
            onNavigationClick = onBackToHome,
            navigationContentDescription = "Back to guide",
            onEcoKarmaClick = onOpenLeaderboard,
            onProfileClick = onOpenProfile,
            profileContentDescription = "Profile — ${userProfile?.nickname ?: "you"}",
            title = {
                ParyavaranAppBarTitle(
                    text = if (userHistoryMode) {
                        "Eco‑karma map"
                    } else {
                        "Map"
                    },
                    subtitle = if (userHistoryMode) cleanerNicknameFilter?.trim() else null,
                )
            },
        )

        if (!userHistoryMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "Nearby ${filteredReports.size}/${reports.size} · nearest first",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Radius",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.widthIn(min = 42.dp),
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(radiusChipScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            radiusOptions.forEachIndexed { i, (label, _) ->
                                FilterChip(
                                    selected = radiusIndex == i,
                                    onClick = { radiusIndex = i },
                                    label = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                        )
                                    },
                                    modifier = Modifier.height(34.dp),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Types",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.widthIn(min = 42.dp),
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(wasteChipScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = selectedWasteFilters.isEmpty(),
                                onClick = { selectedWasteFilters = emptySet() },
                                label = {
                                    Text(
                                        "All",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.height(34.dp),
                            )
                            WasteMenu.types.forEach { w ->
                                FilterChip(
                                    selected = w in selectedWasteFilters,
                                    onClick = {
                                        selectedWasteFilters =
                                            if (w in selectedWasteFilters) {
                                                selectedWasteFilters - w
                                            } else {
                                                selectedWasteFilters + w
                                            }
                                    },
                                    label = {
                                        Text(
                                            w,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    modifier = Modifier.height(34.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "Tap All to clear. Stricter match: every selected type must appear on the report.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val uid = cleanerUserIdFilter
                    val reportedCount = filteredReports.count {
                        matchesReporterOnUserMap(it, uid, filterNick)
                    }
                    val cleanedCount = filteredReports.count {
                        matchesCleanerOnUserMap(it, uid, filterNick)
                    }
                    val reportPts = reportedCount * EcoKarma.SUBMIT_REPORT
                    val cleanupPts = cleanedCount * EcoKarma.MARK_CLEANED
                    val totalPts = reportPts + cleanupPts
                    Text(
                        text = buildString {
                            append(filterNick)
                            append(" — ")
                            append(filteredReports.size)
                            append(
                                if (filteredReports.size == 1) {
                                    " pin (read-only)"
                                } else {
                                    " pins (read-only)"
                                },
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Reports: $reportedCount × ${EcoKarma.SUBMIT_REPORT} → $reportPts pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Cleanups: $cleanedCount × ${EcoKarma.MARK_CLEANED} → $cleanupPts pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Total: $totalPts eco‑karma",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Red = they reported · Green = they verified cleanup",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }

        if (!mapReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(48.dp))
                    Text(
                        text = "Loading map…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setMultiTouchControls(true)
                            controller.setZoom(16.5)
                            // AVD: reduces "Unable to match the desired swap behavior" / Gralloc glitches
                            // with OSMDroid + OpenGL; real phones keep default (HW) layer.
                            if (isProbablyEmulator()) {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            }
                            bindOsmdroidToLifecycle(this, activity)
                        }
                    },
                    update = { map -> mapViewRef = map },
                )
                if (userHistoryMode && filteredReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No reports or verified cleanups are stored for this player on this device yet.\n" +
                                "(Activity uses reporter name / user id and cleaner name when you finish a cleanup.)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
        }

        if (!userHistoryMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                Button(
                    onClick = onReportIncident,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 8.dp,
                        hoveredElevation = 4.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Report waste",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Create the [MapView] only inside [AndroidView] factory (Compose owns the view), and tie
 * [onResume]/[onPause]/[onDetach] to the window + activity lifecycle. External pre-built
 * MapViews or calling [MapView.onDetach] from [DisposableEffect] can crash when the view
 * is still hosted by Compose.
 */
private fun MapView.applyReportOverlays(
    context: android.content.Context,
    filteredReports: List<ReportEntity>,
    userLocation: GeoPoint,
    onReportTap: (ReportEntity) -> Unit,
) {
    overlays.clear()
    filteredReports.forEach { report ->
        addReportMarker(context, report, onReportTap)
    }
    val userMarker = Marker(this).apply {
        position = userLocation
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = "You"
        ContextCompat.getDrawable(context, R.drawable.blue_marker)?.let { icon = it }
    }
    overlays.add(userMarker)
    invalidate()
    postInvalidate()
}

private fun bindOsmdroidToLifecycle(map: MapView, lifecycleOwner: LifecycleOwner) {
    map.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        private var observer: LifecycleEventObserver? = null

        override fun onViewAttachedToWindow(v: View) {
            val obs = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> map.onResume()
                    Lifecycle.Event.ON_PAUSE -> map.onPause()
                    else -> {}
                }
            }
            observer = obs
            lifecycleOwner.lifecycle.addObserver(obs)
            // Do not call map.onResume() here: adding the observer syncs to current state and can
            // already dispatch ON_RESUME — a second call crashes some OSMDroid builds.
        }

        override fun onViewDetachedFromWindow(v: View) {
            observer?.let { lifecycleOwner.lifecycle.removeObserver(it) }
            observer = null
            try {
                map.onPause()
                map.onDetach()
            } catch (_: Throwable) {
                // Ignore teardown races when Compose removes the view during navigation.
            }
        }
    })
}

private fun markerSnippet(report: ReportEntity): String {
    val d = report.description.trim()
    return buildString {
        append(report.status.trim().ifEmpty { "—" })
        if (d.isNotEmpty()) {
            append(" · ")
            append(if (d.length > 64) d.take(64) + "…" else d)
        }
    }
}

private fun MapView.addReportMarker(
    context: android.content.Context,
    report: ReportEntity,
    onReportTap: (ReportEntity) -> Unit,
) {
    val marker = Marker(this).apply {
        position = GeoPoint(report.latitude, report.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = WasteTypeCsv.formatShort(report.wasteType)
        snippet = markerSnippet(report)
        val iconRes = when (report.status.trim()) {
            "Cleaned" -> R.drawable.green_marker
            else -> R.drawable.red_marker
        }
        ContextCompat.getDrawable(context, iconRes)?.let { icon = it }
        setOnMarkerClickListener { _, mapView ->
            mapView.controller.animateTo(GeoPoint(report.latitude, report.longitude))
            mapView.controller.setZoom(17.0)
            onReportTap(report)
            true
        }
    }
    overlays.add(marker)
}
