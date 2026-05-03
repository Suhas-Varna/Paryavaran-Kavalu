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
import androidx.compose.ui.platform.LocalContext
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
import com.example.paryavaran_kavalu.ui.components.RoomDebugBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    viewModel: WasteReportViewModel,
    modifier: Modifier = Modifier,
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

    var showRoomDebug by remember { mutableStateOf(false) }

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

    val filteredReports = remember(reports, currentLocation, radiusIndex, selectedWasteFilters, radiusOptions) {
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
                append(';')
            }
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(mapViewRef, filteredReports, currentLocation, mapSyncToken) {
        val map = mapViewRef ?: return@LaunchedEffect
        map.applyReportOverlays(
            context = context,
            filteredReports = filteredReports,
            userLocation = currentLocation,
        ) { tapped -> onOpenIncidentDetail(tapped.id) }
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
        if (!locationPermissionGranted) return@LaunchedEffect
        val fused = LocationServices.getFusedLocationProviderClient(context)
        try {
            var loc = fused.lastLocation.await()
            if (loc == null) {
                loc = fused.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
            }
            loc?.let {
                currentLocation = GeoPoint(it.latitude, it.longitude)
                viewModel.seedDemoReportsIfNeeded(it.latitude, it.longitude)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /** If fused location stays null (some emulators), still seed once around the map centre. */
    LaunchedEffect(mapReady) {
        if (!mapReady) return@LaunchedEffect
        delay(4500)
        viewModel.seedDemoReportsIfNeeded(currentLocation.latitude, currentLocation.longitude)
    }

    Column(modifier = modifier.fillMaxSize()) {
        ParyavaranPrimaryAppBar(
            navigation = AppBarNavigation.Back,
            onNavigationClick = onBackToHome,
            navigationContentDescription = "Back to guide",
            onDebugClick = { showRoomDebug = true },
            onEcoKarmaClick = onOpenLeaderboard,
            onProfileClick = onOpenProfile,
            profileContentDescription = "Profile — ${userProfile?.nickname ?: "you"} (${userProfile?.userType ?: "Reporter"})",
            title = {
                ParyavaranAppBarTitle(text = "Map")
            },
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Nearby (${filteredReports.size} of ${reports.size}) · nearest first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Distance", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(radiusChipScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    radiusOptions.forEachIndexed { i, (label, _) ->
                        FilterChip(
                            selected = radiusIndex == i,
                            onClick = { radiusIndex = i },
                            label = { Text(label) },
                        )
                    }
                }
                Text("Waste type", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Tap All to clear. Pick one or more types — a pin shows only if that report includes every selected category (stricter match).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(wasteChipScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedWasteFilters.isEmpty(),
                        onClick = { selectedWasteFilters = emptySet() },
                        label = { Text("All") },
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
                            label = { Text(w) },
                        )
                    }
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
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
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
        }

        Button(
            onClick = onReportIncident,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = "Report waste",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    RoomDebugBottomSheet(
        visible = showRoomDebug,
        onDismiss = { showRoomDebug = false },
        user = userProfile,
        reports = reports,
    )
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
    controller.setCenter(userLocation)
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
