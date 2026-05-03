package com.example.paryavaran_kavalu.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.PartyPopperBurst
import com.example.paryavaran_kavalu.ui.components.ParyavaranAppBarTitle
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.components.RoomDebugBottomSheet
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Deep green — success / nature */
private val ImpactGreen = Color(0xFF1B5E20)
private val MintWash = Color(0xFFE8F5E9)
private val LeafAccent = Color(0xFF43A047)

@Composable
fun CleanupSuccessScreen(
    pointsEarned: Int,
    onDone: () -> Unit,
    onViewLeaderboard: () -> Unit,
    viewModel: WasteReportViewModel,
    onOpenProfile: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val reports by viewModel.reports.collectAsStateWithLifecycle(lifecycleOwner = activity)
    var showRoomDebug by remember { mutableStateOf(false) }

    val iconScale = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    val pointsShown = remember { Animatable(0f) }

    LaunchedEffect(pointsEarned) {
        coroutineScope {
            launch {
                iconScale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            launch { ringAlpha.animateTo(1f, tween(420)) }
            launch { pointsShown.animateTo(pointsEarned.toFloat(), tween(900)) }
        }
    }

    val headlineAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, delayMillis = 120),
        label = "headlineAlpha",
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ParyavaranPrimaryAppBar(
                navigation = AppBarNavigation.None,
                onNavigationClick = {},
                onDebugClick = { showRoomDebug = true },
                onEcoKarmaClick = onViewLeaderboard,
                onProfileClick = onOpenProfile,
                profileContentDescription = "Profile — ${profile?.nickname ?: "you"} (${profile?.userType ?: "Reporter"})",
                title = {
                    ParyavaranAppBarTitle(
                        text = "Success",
                        subtitle = "Eco‑karma · +$pointsEarned pts",
                    )
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MintWash,
                                Color.White,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
            ) {
                PartyPopperBurst(
                    modifier = Modifier.fillMaxSize(),
                    triggerKey = pointsEarned,
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .alpha(ringAlpha.value)
                            .background(
                                color = LeafAccent.copy(alpha = 0.18f),
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .scale(iconScale.value)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(LeafAccent, ImpactGreen),
                                ),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Spot verified clean",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(headlineAlpha),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You helped your neighbourhood breathe easier. Keep going — every cleanup counts.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(headlineAlpha),
                )

                Spacer(Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RowAlignedIconTitle(
                            icon = Icons.Outlined.EmojiEvents,
                            title = "Eco‑karma earned",
                        )
                        Text(
                            text = "+${pointsShown.value.toInt()} points",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ImpactGreen,
                        )
                        Text(
                            text = "For verifying this cleanup with your photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Your total: ",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${profile?.ecoPoints ?: 0} pts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "(Totals update from your profile — tune rewards anytime.)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                TextButton(
                    onClick = onViewLeaderboard,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "See where you stand on the leaderboard",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImpactGreen,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                }
            }
        }
    }
    RoomDebugBottomSheet(
        visible = showRoomDebug,
        onDismiss = { showRoomDebug = false },
        user = profile,
        reports = reports,
    )
}

@Composable
private fun RowAlignedIconTitle(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LeafAccent,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
