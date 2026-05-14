package com.example.paryavaran_kavalu.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.paryavaran_kavalu.data.WasteMenu
import com.example.paryavaran_kavalu.data.WasteTypeCsv
import com.example.paryavaran_kavalu.util.isProbablyEmulator
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.ParyavaranAppBarTitle
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
private suspend fun awaitBestLocation(context: android.content.Context): Location? {
    val fused = LocationServices.getFusedLocationProviderClient(context)
    var loc = fused.lastLocation.await()
    if (loc == null) {
        loc = fused.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token,
        ).await()
    }
    return loc
}

@SuppressLint("MissingPermission")
private suspend fun submitReportIfReady(
    context: android.content.Context,
    viewModel: WasteReportViewModel,
    imageUri: String?,
    wasteType: String,
    description: String,
    locationPermissionGranted: () -> Boolean,
    onError: (String) -> Unit,
    onSubmitted: () -> Unit,
) {
    val uri = imageUri
    if (uri.isNullOrBlank()) {
        onError("Please capture a photo first.")
        return
    }
    if (wasteType.isBlank() || WasteTypeCsv.parseStored(wasteType).isEmpty()) {
        onError("Select at least one waste type.")
        return
    }
    if (!locationPermissionGranted()) {
        onError("Location permission is required to submit.")
        return
    }
    if (WasteTypeCsv.containsCategory(wasteType, "Other") && description.trim().isEmpty()) {
        onError("Please describe the waste when “Other” is included.")
        return
    }
    try {
        val location: Location? = awaitBestLocation(context)
        if (location == null) {
            onError("Could not read location. Try again outdoors.")
            return
        }
        viewModel.submitReport(
            imageUri = uri,
            latitude = location.latitude,
            longitude = location.longitude,
            wasteType = wasteType,
            description = description,
            status = "Pending",
        )
        onSubmitted()
    } catch (e: SecurityException) {
        onError("Location permission denied.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitted: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)
    var description by remember { mutableStateOf("") }
    var selectedWasteTypes by remember { mutableStateOf(setOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imageUri = viewModel.capturedImageUri

    // Same pattern as MapScreen: mutable state + launcher callback (not a one-shot checkSelfPermission).
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    /** When true, run submit again after ACCESS_FINE_LOCATION is granted (same tap flow). */
    var pendingSubmitAfterPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            errorMessage = null
            if (pendingSubmitAfterPermission) {
                pendingSubmitAfterPermission = false
                scope.launch {
                    val wasteCsv = WasteTypeCsv.normalize(selectedWasteTypes.toList())
                    submitReportIfReady(
                        context = context,
                        viewModel = viewModel,
                        imageUri = viewModel.capturedImageUri,
                        wasteType = wasteCsv,
                        description = description,
                        locationPermissionGranted = { locationPermissionGranted },
                        onError = { errorMessage = it },
                        onSubmitted = {
                            viewModel.updateCapturedImageUri(null)
                            onSubmitted()
                        },
                    )
                }
            }
        } else {
            pendingSubmitAfterPermission = false
            errorMessage = "Location permission is required to submit."
        }
    }

    DisposableEffect(activity, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize()) {
        ParyavaranPrimaryAppBar(
            navigation = AppBarNavigation.Back,
            onNavigationClick = onBack,
            navigationContentDescription = "Back",
            onEcoKarmaClick = onOpenLeaderboard,
            onProfileClick = onOpenProfile,
            profileContentDescription = "Profile — ${profile?.nickname ?: "you"}",
            title = { ParyavaranAppBarTitle(text = "Report") },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Text(
            text = "On submit we store GPS latitude & longitude with your photo in the local database (same coordinates shown on the map pin).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isProbablyEmulator()) {
            Text(
                text = "Emulator: stored location is offset by a random 1–25 km from the fix so pins spread near you for easier debugging.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Text("Waste types", style = MaterialTheme.typography.labelLarge)
        Text(
            text = "Select one or more. Reports are saved with all selected categories so map filters (Plastic, Glass, …) can match any of them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WasteMenu.types.forEach { type ->
                val selected = type in selectedWasteTypes
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedWasteTypes =
                            if (selected) selectedWasteTypes - type else selectedWasteTypes + type
                    },
                    label = { Text(type) },
                )
            }
        }
        if ("Other" in selectedWasteTypes) {
            Text(
                text = "Include “Other” when not everything fits the chips. Describe what you see below — required if Other is selected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val isOther = "Other" in selectedWasteTypes
        val descError = isOther && description.isBlank()
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = {
                Text(
                    if (isOther) "Description (required for Other)" else "Description",
                )
            },
            supportingText = if (isOther) {
                {
                    Text(
                        text = if (descError) {
                            "Required — say what kind of waste this is."
                        } else {
                            "Examples: medical wrappers, mixed tyres, construction foam…"
                        },
                        color = if (descError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                null
            },
            isError = descError,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Captured waste photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Button(
            onClick = onOpenCamera,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Capture / retake photo")
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = {
                val uri = imageUri
                if (uri.isNullOrBlank()) {
                    errorMessage = "Please capture a photo first."
                    return@Button
                }
                if (selectedWasteTypes.isEmpty()) {
                    errorMessage = "Select at least one waste type."
                    return@Button
                }
                if ("Other" in selectedWasteTypes && description.trim().isEmpty()) {
                    errorMessage = "Please describe the waste when “Other” is selected."
                    return@Button
                }
                if (!locationPermissionGranted) {
                    pendingSubmitAfterPermission = true
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    errorMessage = "Allow location to submit. We’ll continue after you grant permission."
                    return@Button
                }

                val wasteCsv = WasteTypeCsv.normalize(selectedWasteTypes.toList())

                errorMessage = null
                scope.launch {
                    submitReportIfReady(
                        context = context,
                        viewModel = viewModel,
                        imageUri = uri,
                        wasteType = wasteCsv,
                        description = description,
                        locationPermissionGranted = { locationPermissionGranted },
                        onError = { errorMessage = it },
                        onSubmitted = {
                            viewModel.updateCapturedImageUri(null)
                            onSubmitted()
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Submit report")
        }
        }
    }
}
