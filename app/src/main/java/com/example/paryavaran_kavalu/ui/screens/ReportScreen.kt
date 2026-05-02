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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val wasteTypes = listOf("Plastic", "Organic", "Glass", "Metal", "Electronic", "Other")

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
    if (!locationPermissionGranted()) {
        onError("Location permission is required to submit.")
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

@Composable
fun ReportScreen(
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()

    var description by remember { mutableStateOf("") }
    var wasteType by remember { mutableStateOf(wasteTypes.first()) }
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
                    submitReportIfReady(
                        context = context,
                        viewModel = viewModel,
                        imageUri = viewModel.capturedImageUri,
                        wasteType = wasteType,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Report details",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "On submit we store GPS latitude & longitude with your photo in the local database (same coordinates shown on the map pin).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Waste type", style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            wasteTypes.forEach { type ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (type == wasteType),
                            onClick = { wasteType = type },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (type == wasteType),
                        onClick = null,
                    )
                    Text(type, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
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
                if (!locationPermissionGranted) {
                    pendingSubmitAfterPermission = true
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    errorMessage = "Allow location to submit. We’ll continue after you grant permission."
                    return@Button
                }

                errorMessage = null
                scope.launch {
                    submitReportIfReady(
                        context = context,
                        viewModel = viewModel,
                        imageUri = uri,
                        wasteType = wasteType,
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
