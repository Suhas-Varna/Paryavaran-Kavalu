package com.example.paryavaran_kavalu.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paryavaran_kavalu.data.UserTypes
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.example.paryavaran_kavalu.ui.userTypeIcon
import com.example.paryavaran_kavalu.ui.userTypeShortDescription

@Composable
fun ProfileScreen(
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)

    var nickname by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf(UserTypes.REPORTER) }
    var bio by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    /** False until we’ve copied Room profile into fields — avoids “dirty” on first frame. */
    var hydratedFromProfile by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        val p = profile ?: return@LaunchedEffect
        nickname = p.nickname
        userType = p.userType.takeIf { it in UserTypes.all } ?: UserTypes.REPORTER
        bio = p.bio
        hydratedFromProfile = true
    }

    val roomProfile = profile
    val baselineType = roomProfile?.userType?.takeIf { it in UserTypes.all } ?: UserTypes.REPORTER
    val hasUnsavedChanges =
        hydratedFromProfile &&
            roomProfile != null &&
            (
                nickname.trim() != roomProfile.nickname.trim() ||
                    userType != baselineType ||
                    bio.trim() != roomProfile.bio.trim()
                )

    val canSave = hasUnsavedChanges && nickname.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Text("Your profile", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Stored on this device with Room. Nickname is saved on each waste report.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = {
                nickname = it
                errorMessage = null
            },
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("Required — shown on the eco‑karma screen and attached to new reports.")
            },
        )

        Text("How you participate", style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            UserTypes.all.forEach { type ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = type == userType,
                            onClick = { userType = type },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = type == userType, onClick = null)
                    Icon(
                        imageVector = userTypeIcon(type),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(type, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            userTypeShortDescription(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("More about you (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            supportingText = {
                Text("Neighbourhood, languages, best times to help — whatever helps coordination later.")
            },
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (hydratedFromProfile && !hasUnsavedChanges) {
            Text(
                text = "Edit a field above to save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = {
                val n = nickname.trim()
                if (n.isEmpty()) {
                    errorMessage = "Please enter a nickname."
                    return@Button
                }
                errorMessage = null
                viewModel.updateProfile(nickname = n, userType = userType, bio = bio) {
                    onBack()
                }
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save changes")
        }
    }
}
