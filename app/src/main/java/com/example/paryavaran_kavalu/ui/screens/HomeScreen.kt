package com.example.paryavaran_kavalu.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.ParyavaranAppBarTitle
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.components.RoomDebugBottomSheet

private data class GuideSlide(
    val emoji: String,
    val title: String,
    val body: String,
)

private val guideSlides = listOf(
    GuideSlide(
        emoji = "🌍",
        title = "Why this app?",
        body = "Paryavaran Kavalu helps neighbours report dumped waste with a photo and location so everyone can see what needs attention and act together for cleaner streets and public spaces.",
    ),
    GuideSlide(
        emoji = "🗺️",
        title = "How it works",
        body = "Open the map to see incidents near you. Each pin is a report—tap it for details. You can submit a new report after taking a picture; your GPS coordinates are saved with it.",
    ),
    GuideSlide(
        emoji = "📸",
        title = "Report in seconds",
        body = "Choose the waste type, add a short description, and submit. Everything is stored on your phone with Room first—so you can use the app in the field even when the network is weak.",
    ),
    GuideSlide(
        emoji = "🔄",
        title = "Track cleanup",
        body = "Pending sites show as red pins. When someone verifies cleanup with an “after” photo, the report is marked cleaned and the pin turns green—clear progress for the whole community.",
    ),
    GuideSlide(
        emoji = "🌿",
        title = "Benefits for you",
        body = "Earn eco‑karma points for reporting and for confirming cleanups. Build a transparent local picture of waste hotspots and celebrate impact right where you live.",
    ),
)

/** One gradient per slide — clearer color so it reads as a single panel (no “double frame”). */
@Composable
private fun guideSlideGradientBrush(page: Int): Brush {
    val scheme = MaterialTheme.colorScheme
    val i = page % guideSlides.size
    return when (i) {
        0 -> Brush.linearGradient(
            colors = listOf(
                scheme.surfaceContainerLow,
                scheme.primary.copy(alpha = 0.16f),
                scheme.primaryContainer.copy(alpha = 0.58f),
            ),
            start = Offset(0f, 0f),
            end = Offset(480f, 680f),
        )
        1 -> Brush.linearGradient(
            colors = listOf(
                scheme.surfaceContainerLow,
                scheme.secondary.copy(alpha = 0.14f),
                scheme.secondaryContainer.copy(alpha = 0.52f),
            ),
            start = Offset(400f, 0f),
            end = Offset(0f, 620f),
        )
        2 -> Brush.linearGradient(
            colors = listOf(
                scheme.surfaceContainerLow,
                scheme.tertiary.copy(alpha = 0.14f),
                scheme.tertiaryContainer.copy(alpha = 0.52f),
            ),
            start = Offset(0f, 0f),
            end = Offset(520f, 720f),
        )
        3 -> Brush.linearGradient(
            colors = listOf(
                scheme.surfaceContainerLow,
                scheme.primary.copy(alpha = 0.12f),
                scheme.tertiaryContainer.copy(alpha = 0.48f),
            ),
            start = Offset(0f, 420f),
            end = Offset(520f, 0f),
        )
        else -> Brush.linearGradient(
            colors = listOf(
                scheme.surfaceContainerLow,
                scheme.secondary.copy(alpha = 0.12f),
                scheme.primaryContainer.copy(alpha = 0.5f),
            ),
            start = Offset(0f, 0f),
            end = Offset(460f, 700f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: WasteReportViewModel,
    onGetStarted: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val reports by viewModel.reports.collectAsStateWithLifecycle(lifecycleOwner = activity)
    var showRoomDebug by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { guideSlides.size },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(6_000)
            val next = (pagerState.currentPage + 1) % guideSlides.size
            pagerState.animateScrollToPage(next)
        }
    }

    val screenBg = MaterialTheme.colorScheme.surface

    Box(
        Modifier
            .fillMaxSize()
            .background(screenBg),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBg,
            topBar = {
                ParyavaranPrimaryAppBar(
                    navigation = AppBarNavigation.None,
                    onNavigationClick = {},
                    onDebugClick = { showRoomDebug = true },
                    onEcoKarmaClick = null,
                    onProfileClick = null,
                    title = {
                        ParyavaranAppBarTitle(
                            text = "Welcome",
                            subtitle = "${pagerState.currentPage + 1} of ${guideSlides.size}",
                        )
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    GuideSlideContent(
                        slide = guideSlides[page],
                        pageIndex = page,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PagerDots(
                        pageCount = guideSlides.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val prev =
                                        (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(prev)
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Previous",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Previous")
                        }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    val next =
                                        (pagerState.currentPage + 1).coerceAtMost(guideSlides.lastIndex)
                                    pagerState.animateScrollToPage(next)
                                }
                            },
                            enabled = pagerState.currentPage < guideSlides.lastIndex,
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    ) {
                        Text(
                            text = "Get started",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    Spacer(Modifier.height(8.dp))
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
}

@Composable
private fun GuideSlideContent(
    slide: GuideSlide,
    pageIndex: Int,
) {
    val scheme = MaterialTheme.colorScheme
    val brush = guideSlideGradientBrush(pageIndex)
    val cardShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .clip(cardShape)
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = slide.emoji,
                fontSize = 52.sp,
                lineHeight = 56.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = slide.body,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
            )
        }
    }
}

@Composable
private fun PagerDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        },
                    ),
            )
        }
    }
}
