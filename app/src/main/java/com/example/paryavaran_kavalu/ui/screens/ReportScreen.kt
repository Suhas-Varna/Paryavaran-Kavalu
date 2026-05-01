package com.example.paryavaran_kavalu.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun ReportScreen(
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    capturedImage: Bitmap? = null
) {

    var message by remember { mutableStateOf("") }
    var wasteType by remember { mutableStateOf("Select Type") }

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
            text = "Report Waste",
            style = MaterialTheme.typography.headlineSmall
        )

        // Waste Type
        Button(onClick = {
            wasteType = "Plastic"
        }) {
            Text(wasteType)
        }

        // Description
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        // 📸 Show Image if available
        capturedImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        // Open Camera
        Button(
            onClick = onOpenCamera,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Capture Photo")
        }

        // Submit
        Button(
            onClick = { /* next step: save */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Report")
        }
    }
}