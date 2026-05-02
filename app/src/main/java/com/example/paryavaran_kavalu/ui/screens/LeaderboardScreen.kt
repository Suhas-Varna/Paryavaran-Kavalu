package com.example.paryavaran_kavalu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paryavaran_kavalu.ui.EcoKarma
import com.example.paryavaran_kavalu.ui.WasteReportViewModel

@Composable
fun LeaderboardScreen(
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Eco‑karma",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Earn points for reporting waste and for verifying cleanup. Same profile tracks both roles in this build.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = profile?.nickname ?: "Eco warrior",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "${profile?.ecoPoints ?: 0} points",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        Text(
            text = "• ${EcoKarma.SUBMIT_REPORT} pts — submit a new waste report\n" +
                "• ${EcoKarma.MARK_CLEANED} pts — mark a site cleaned with a photo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
