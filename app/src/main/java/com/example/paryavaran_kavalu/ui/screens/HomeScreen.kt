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

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                ParyavaranPrimaryAppBar(
                    navigation = AppBarNavigation.None,
                    onNavigationClick = {},
                    onDebugClick = { showRoomDebug = true },
                    onEcoKarmaClick = onOpenLeaderboard,
                    onProfileClick = onOpenProfile,
                    profileContentDescription = "Profile — ${profile?.nickname ?: "you"} (${profile?.userType ?: "Reporter"})",
                    title = {
                        ParyavaranAppBarTitle(
                            text = "Welcome",
                            subtitle = "Quick guide · ${pagerState.currentPage + 1}/${guideSlides.size}",
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
                    GuideSlideContent(slide = guideSlides[page])
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PagerDots(
                        pageCount = guideSlides.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.padding(vertical = 8.dp),
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
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
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
private fun GuideSlideContent(slide: GuideSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = slide.emoji,
            fontSize = 96.sp,
            lineHeight = 104.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        },
                    ),
            )
        }
    }
}
