package com.example.paryavaran_kavalu.ui.screens

import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.ParyavaranAppBarTitle
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.WasteReportViewModel

private fun profileEcoTierLabel(points: Int): String = when {
    points >= 200 -> "Forest Guardian"
    points >= 75 -> "Green Champion"
    points >= 25 -> "Sprout"
    else -> "Seedling"
}

private fun profileTierProgress(points: Int): Pair<Float, String?> {
    val tiers = listOf(0, 25, 75, 200)
    val nextThreshold = tiers.firstOrNull { it > points }
        ?: return 1f to null
    val prevThreshold = tiers.lastOrNull { it <= points } ?: 0
    val span = (nextThreshold - prevThreshold).coerceAtLeast(1)
    val through = (points - prevThreshold).coerceIn(0, span)
    val fraction = through.toFloat() / span
    val label = "Next tier at $nextThreshold pts"
    return fraction to label
}

@Composable
fun ProfileScreen(
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)
    var nickname by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var hydratedFromProfile by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        val p = profile ?: return@LaunchedEffect
        nickname = p.nickname
        bio = p.bio
        hydratedFromProfile = true
    }

    val roomProfile = profile
    val hasUnsavedChanges =
        hydratedFromProfile &&
            roomProfile != null &&
            (
                nickname.trim() != roomProfile.nickname.trim() ||
                    bio.trim() != roomProfile.bio.trim()
                )

    val canSave = hasUnsavedChanges && nickname.trim().isNotEmpty()

    val points = profile?.ecoPoints ?: 0
    val tierLabel = profileEcoTierLabel(points)
    val (tierFraction, nextTierLabel) = remember(points) { profileTierProgress(points) }

    Column(Modifier.fillMaxSize()) {
        ParyavaranPrimaryAppBar(
            navigation = AppBarNavigation.Back,
            onNavigationClick = onBack,
            navigationContentDescription = "Back",
            onEcoKarmaClick = onOpenLeaderboard,
            onProfileClick = onOpenProfile,
            profileContentDescription = "Profile — ${profile?.nickname ?: "you"}",
            title = { ParyavaranAppBarTitle(text = "Your profile") },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ),
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val displayNick = nickname.trim().ifBlank { profile?.nickname?.trim().orEmpty() }
                    val initial = displayNick.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = displayNick.ifBlank { "Your eco name" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EnergySavingsLeaf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = tierLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "$points eco‑karma pts",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (nextTierLabel != null) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { tierFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = nextTierLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "You’ve reached the top tier — thank you for showing up for your neighbourhood.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(
                text = "Your nickname appears on the leaderboard and is stored with reports you file or clean up. Everyone can report and verify cleanups — pick what fits your day.",
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
                    Text("Required — shown on eco‑karma and attached to new activity.")
                },
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("About you (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                supportingText = {
                    Text("Neighbourhood, languages, best times to help — whatever feels right.")
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
                    viewModel.updateProfile(nickname = n, bio = bio) {
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
}
