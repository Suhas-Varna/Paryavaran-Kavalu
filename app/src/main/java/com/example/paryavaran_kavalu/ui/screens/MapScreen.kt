package com.example.paryavaran_kavalu.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.example.paryavaran_kavalu.util.launchWalkingNavigation
import com.example.paryavaran_kavalu.R
import com.example.paryavaran_kavalu.data.ReportEntity
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
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val reports by viewModel.reports.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val mapRefreshGen by viewModel.reportWriteGeneration.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)

    var selectedReport by remember { mutableStateOf<ReportEntity?>(null) }

    LaunchedEffect(reports, selectedReport?.id) {
        val id = selectedReport?.id ?: return@LaunchedEffect
        selectedReport = reports.find { it.id == id } ?: selectedReport
    }

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
    val wasteFilterChoices = remember {
        listOf("All", "Plastic", "Organic", "Glass", "Metal", "Electronic", "Other")
    }
    var radiusIndex by remember { mutableIntStateOf(radiusOptions.lastIndex) }
    var wasteFilter by remember { mutableStateOf("All") }
    val radiusChipScroll = rememberScrollState()
    val wasteChipScroll = rememberScrollState()

    val filteredReports = remember(reports, currentLocation, radiusIndex, wasteFilter, radiusOptions) {
        val capKm = radiusOptions[radiusIndex].second
        val distanceLimited = radiusIndex != radiusOptions.lastIndex
        val lat = currentLocation.latitude
        val lon = currentLocation.longitude
        reports
            .map { r -> r to distanceMeters(lat, lon, r.latitude, r.longitude) }
            .filter { (r, dM) ->
                if (distanceLimited && dM > capKm * 1000.0) return@filter false
                if (wasteFilter != "All" && r.wasteType != wasteFilter) return@filter false
                true
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    LaunchedEffect(filteredReports, selectedReport?.id) {
        val id = selectedReport?.id ?: return@LaunchedEffect
        if (filteredReports.none { it.id == id }) {
            selectedReport = null
        }
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

    val dateFmt = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                TextButton(
                    onClick = onBackToHome,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Text("About", color = MaterialTheme.colorScheme.onPrimary)
                }
                Text(
                    text = "Map",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    TextButton(
                        onClick = onOpenLeaderboard,
                        modifier = Modifier.padding(end = 2.dp),
                    ) {
                        Text(
                            text = "Eco‑karma",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Profile — ${userProfile?.nickname ?: "you"} (${userProfile?.userType ?: "Reporter"})",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

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
                Row(
                    modifier = Modifier.horizontalScroll(wasteChipScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    wasteFilterChoices.forEach { w ->
                        FilterChip(
                            selected = wasteFilter == w,
                            onClick = { wasteFilter = w },
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
            key(
                mapRefreshGen,
                reports.size,
                reports.sumOf { it.id },
                radiusIndex,
                wasteFilter,
                filteredReports.size,
                filteredReports.sumOf { it.id },
            ) {
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
                    update = { map ->
                        map.controller.setCenter(currentLocation)
                        map.overlays.clear()
                        filteredReports.forEach { report ->
                            map.addReportMarker(context, report) { tapped ->
                                selectedReport = tapped
                            }
                        }
                        val userMarker = Marker(map).apply {
                            position = currentLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "You"
                            ContextCompat.getDrawable(context, R.drawable.blue_marker)?.let { icon = it }
                        }
                        map.overlays.add(userMarker)
                        map.invalidate()
                    },
                )
            }
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

    selectedReport?.let { report ->
        ModalBottomSheet(
            onDismissRequest = { selectedReport = null },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Report details", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Reported by: ${report.reporterNickname.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Stored coordinates (Room): " +
                        String.format(Locale.US, "%.5f, %.5f", report.latitude, report.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Captured: ${dateFmt.format(Date(report.timestamp))}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Type: ${report.wasteType}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = report.description.ifBlank { "No description" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Status: ${report.status}",
                    style = MaterialTheme.typography.labelLarge,
                )

                AsyncImage(
                    model = report.imageUri,
                    contentDescription = "Report photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop,
                )

                OutlinedButton(
                    onClick = {
                        context.launchWalkingNavigation(report.latitude, report.longitude)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Walking directions (Maps / OSM)")
                }

                if (report.status.equals("Cleaned", ignoreCase = true)) {
                    report.cleanedAt?.let {
                        Text(
                            "Marked clean: ${dateFmt.format(Date(it))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    report.cleanedImageUri?.let { cleanUri ->
                        Text("Cleaning photo", style = MaterialTheme.typography.labelMedium)
                        AsyncImage(
                            model = cleanUri,
                            contentDescription = "Cleaning proof",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                if (report.status.equals("Pending", ignoreCase = true)) {
                    Button(
                        onClick = {
                            val id = report.id
                            selectedReport = null
                            onRequestCleanPhoto(id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Mark as cleaned (take photo)")
                    }
                }

                Button(
                    onClick = { selectedReport = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Close")
                }
            }
        }
    }
}

private fun isProbablyEmulator(): Boolean {
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

/**
 * Create the [MapView] only inside [AndroidView] factory (Compose owns the view), and tie
 * [onResume]/[onPause]/[onDetach] to the window + activity lifecycle. External pre-built
 * MapViews or calling [MapView.onDetach] from [DisposableEffect] can crash when the view
 * is still hosted by Compose.
 */
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

private fun MapView.addReportMarker(
    context: android.content.Context,
    report: ReportEntity,
    onReportTap: (ReportEntity) -> Unit,
) {
    val marker = Marker(this).apply {
        position = GeoPoint(report.latitude, report.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = report.wasteType
        snippet = report.status
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
