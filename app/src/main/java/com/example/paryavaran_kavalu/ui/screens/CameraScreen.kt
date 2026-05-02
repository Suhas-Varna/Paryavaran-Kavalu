package com.example.paryavaran_kavalu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onImageCaptured: (Uri) -> Unit,
    title: String = "Capture waste photo",
    sessionKey: Any? = null,
    autoOpenCamera: Boolean = false,
) {
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val photoFile = remember(sessionKey) {
        File.createTempFile("photo_", ".jpg", context.cacheDir)
    }

    val uri = remember(sessionKey) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile,
        )
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            imageUri = uri
        }
    }

    var didAutoOpen by remember { mutableStateOf(false) }
    LaunchedEffect(autoOpenCamera, cameraPermissionGranted) {
        if (autoOpenCamera && cameraPermissionGranted && !didAutoOpen) {
            didAutoOpen = true
            cameraLauncher.launch(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )

        imageUri?.let { captured ->
            AsyncImage(
                model = captured,
                contentDescription = "Photo preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Crop,
            )
        }

        if (imageUri == null) {
            Button(
                onClick = {
                    if (cameraPermissionGranted) {
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open camera")
            }
        } else {
            Text("Photo captured")

            Button(
                onClick = {
                    photoFile.delete()
                    photoFile.createNewFile()
                    imageUri = null
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Retake photo")
            }

            Button(
                onClick = { onImageCaptured(imageUri!!) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use this photo")
            }
        }
    }
}
