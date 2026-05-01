package com.example.paryavaran_kavalu.ui.screens

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {

    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // create temp file
    val photoFile = remember {
        File.createTempFile("photo_", ".jpg", context.cacheDir)
    }

    // create uri
    val uri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    // camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Button(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Capture Waste Photo",
            style = MaterialTheme.typography.headlineSmall
        )

        if (imageUri == null) {

            Button(
                onClick = { cameraLauncher.launch(uri) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Camera")
            }

        } else {

            Text("Photo Captured ✔")

            Button(
                onClick = { cameraLauncher.launch(uri) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retake Photo")
            }

            Button(
                onClick = { onImageCaptured(imageUri!!) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use This Photo")
            }
        }
    }
}
