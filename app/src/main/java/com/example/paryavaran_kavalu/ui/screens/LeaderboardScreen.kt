package com.example.paryavaran_kavalu.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paryavaran_kavalu.data.ClaimedRewardRow
import com.example.paryavaran_kavalu.data.RedeemItemEntity
import com.example.paryavaran_kavalu.data.ReportEntity
import com.example.paryavaran_kavalu.data.UserEntity
import com.example.paryavaran_kavalu.ui.EcoKarma
import com.example.paryavaran_kavalu.ui.WasteReportViewModel
import com.example.paryavaran_kavalu.ui.components.AppBarNavigation
import com.example.paryavaran_kavalu.ui.components.PartyPopperBurst
import com.example.paryavaran_kavalu.ui.components.ParyavaranAppBarTitle
import com.example.paryavaran_kavalu.ui.components.ParyavaranPrimaryAppBar
import com.example.paryavaran_kavalu.ui.components.RoomDebugBottomSheet
import kotlinx.coroutines.launch

private data class LeaderboardEntry(
    val nickname: String,
    val points: Int,
    val isYou: Boolean,
    /**
     * [ReportEntity.cleanerUserId] for this player when known (for map). Null for nicknames
     * with no stored cleanups on this device.
     */
    val cleanerUserId: Int?,
)

private fun redeemIconFor(iconName: String): ImageVector = when (iconName) {
    "LocalCafe" -> Icons.Outlined.LocalCafe
    "Park" -> Icons.Outlined.Park
    "ShoppingBag" -> Icons.Outlined.ShoppingBag
    "Storefront" -> Icons.Outlined.Storefront
    "Groups" -> Icons.Outlined.Groups
    "CardGiftcard" -> Icons.Outlined.CardGiftcard
    else -> Icons.Outlined.CardGiftcard
}

/** Resolves [ReportEntity.cleanerUserId] from rows this nickname verified as cleaned (single-user DB). */
private fun cleanerUserIdForNickname(
    reports: List<ReportEntity>,
    nickname: String,
): Int? {
    val n = nickname.trim()
    val ids = reports
        .filter { r ->
            r.status.trim().equals("Cleaned", ignoreCase = true) &&
                r.cleanerNickname.trim().equals(n, ignoreCase = true) &&
                r.cleanerUserId != null
        }
        .mapNotNull { it.cleanerUserId }
        .distinct()
    return when {
        ids.isEmpty() -> null
        ids.size == 1 -> ids.first()
        else -> ids.first()
    }
}

/**
 * Merges Room profile totals with report-derived nicknames so demo neighbours from seeded pins show up.
 * Your row always uses [UserEntity.ecoPoints] so cleanup bonuses stay accurate.
 */
private fun buildLeaderboardEntries(
    profile: UserEntity?,
    reports: List<ReportEntity>,
): List<LeaderboardEntry> {
    val youLabel = profile?.nickname?.trim()?.takeIf { it.isNotEmpty() } ?: "You"
    val byNick = reports.groupBy { r ->
        r.reporterNickname.trim().ifEmpty { "Anonymous" }
    }
    val map = mutableMapOf<String, Int>()
    for ((nick, reps) in byNick) {
        map[nick] = reps.size * EcoKarma.SUBMIT_REPORT
    }
    map[youLabel] = profile?.ecoPoints ?: map[youLabel] ?: 0
    return map.entries
        .map { (nickname, points) ->
            val isYou = nickname.equals(youLabel, ignoreCase = true)
            val cleanerUid = when {
                isYou && profile != null -> profile.userId
                else -> cleanerUserIdForNickname(reports, nickname)
            }
            LeaderboardEntry(
                nickname = nickname,
                points = points,
                isYou = isYou,
                cleanerUserId = cleanerUid,
            )
        }
        .sortedWith(
            compareByDescending<LeaderboardEntry> { it.points }
                .thenBy { it.nickname.lowercase() },
        )
}

private fun ecoTierLabel(points: Int): String = when {
    points >= 200 -> "Forest Guardian"
    points >= 75 -> "Green Champion"
    points >= 25 -> "Sprout"
    else -> "Seedling"
}

private fun tierProgress(points: Int): Pair<Float, String?> {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeaderboardScreen(
    viewModel: WasteReportViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenUserCleanupsMap: (userId: Int?, nickname: String) -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val profile by viewModel.userProfile.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val reports by viewModel.reports.collectAsStateWithLifecycle(lifecycleOwner = activity)
    var showRoomDebug by remember { mutableStateOf(false) }
    var showPointsHelp by remember { mutableStateOf(false) }
    var redeemCelebration by remember { mutableStateOf<RedeemItemEntity?>(null) }
    var redeemConfettiKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 3 },
    )

    val redeemItems by viewModel.redeemCatalog.collectAsStateWithLifecycle(lifecycleOwner = activity)
    val claimedRows by viewModel.claimedRewards.collectAsStateWithLifecycle(lifecycleOwner = activity)

    val yourPoints = profile?.ecoPoints ?: 0
    val tier = ecoTierLabel(yourPoints)
    val (progress, nextLabel) = tierProgress(yourPoints)
    val entries = remember(profile, reports) { buildLeaderboardEntries(profile, reports) }

    val tabTitles = listOf("Leaderboard", "Redeem", "Claimed")
    val tabIcons = listOf(
        Icons.Outlined.Leaderboard,
        Icons.Outlined.CardGiftcard,
        Icons.Outlined.TaskAlt,
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ParyavaranPrimaryAppBar(
                navigation = AppBarNavigation.Back,
                onNavigationClick = onBack,
                navigationContentDescription = "Back",
                onDebugClick = { showRoomDebug = true },
                onEcoKarmaClick = null,
                onProfileClick = onOpenProfile,
                profileContentDescription = "Profile — ${profile?.nickname ?: "you"}",
                onHelpClick = { showPointsHelp = true },
                title = {
                    ParyavaranAppBarTitle(text = "Eco‑karma")
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                EcoKarmaGameCard(
                    points = yourPoints,
                    tierLabel = tier,
                    progress = progress,
                    nextTierHint = nextLabel,
                )
                Spacer(Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { page ->
                    when (page) {
                        0 -> LeaderboardTable(
                            entries = entries,
                            youLabel = profile?.nickname?.trim()?.takeIf { it.isNotEmpty() } ?: "You",
                            onOpenUserCleanupsMap = onOpenUserCleanupsMap,
                        )
                        1 -> RedeemPlaceholder(
                            catalogue = redeemItems,
                            currentEcoPoints = yourPoints,
                            onRedeemReward = { item ->
                                viewModel.redeemReward(item.id) { ok ->
                                    if (ok) {
                                        redeemConfettiKey++
                                        redeemCelebration = item
                                    }
                                }
                            },
                        )
                        else -> ClaimedRewardsSection(
                            rows = claimedRows,
                        )
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

        if (showPointsHelp) {
            EcoKarmaPointsDialog(onDismiss = { showPointsHelp = false })
        }

        redeemCelebration?.let { item ->
            RedeemSuccessOverlay(
                reward = item,
                confettiKey = redeemConfettiKey,
                onDismiss = { redeemCelebration = null },
            )
        }
    }
}

@Composable
private fun EcoKarmaGameCard(
    points: Int,
    tierLabel: String,
    progress: Float,
    nextTierHint: String?,
) {
    val deep = Color(0xFF0D47A1)
    val mid = Color(0xFF1976D2)
    val edge = Color(0xFF64B5F6)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(deep, mid, edge),
                    ),
                )
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Your Eco‑karma",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = points.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp),
                        )
                        Text(
                            text = tierLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EnergySavingsLeaf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            if (nextTierHint != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = nextTierHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF69F0AE),
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Composable
private fun LeaderboardTable(
    entries: List<LeaderboardEntry>,
    youLabel: String,
    onOpenUserCleanupsMap: (userId: Int?, nickname: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                )
                Text(
                    text = "Player",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
            }
            HorizontalDivider()
        }
        itemsIndexed(entries, key = { i, e -> "${e.nickname}-$i" }) { index, entry ->
            val rank = index + 1
            val initial = entry.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val highlight = entry.nickname == youLabel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUserCleanupsMap(entry.cleanerUserId, entry.nickname) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (highlight) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (highlight) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.nickname,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (highlight) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.points}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun RedeemPlaceholder(
    catalogue: List<RedeemItemEntity>,
    currentEcoPoints: Int,
    onRedeemReward: (RedeemItemEntity) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Redeem",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose a reward. Point costs are stored in the catalogue on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (catalogue.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Loading catalogue…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            lazyGridItems(catalogue, key = { it.id }) { reward ->
                RedeemRewardCard(
                    reward = reward,
                    currentEcoPoints = currentEcoPoints,
                    canAfford = currentEcoPoints >= reward.costPoints,
                    onRedeem = { onRedeemReward(reward) },
                )
            }
        }
    }
}

@Composable
private fun RedeemRewardCard(
    reward: RedeemItemEntity,
    currentEcoPoints: Int,
    canAfford: Boolean,
    onRedeem: () -> Unit,
) {
    val ptsShort = (reward.costPoints - currentEcoPoints).coerceAtLeast(0)
    val icon = redeemIconFor(reward.iconName)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text = reward.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = reward.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = reward.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EnergySavingsLeaf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${reward.costPoints} pts",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRedeem,
                enabled = canAfford,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        canAfford -> "Redeem"
                        ptsShort > 0 -> "$ptsShort pts short"
                        else -> "Need ${reward.costPoints} pts"
                    },
                )
            }
        }
    }
}

@Composable
private fun ClaimedRewardsSection(
    rows: List<ClaimedRewardRow>,
) {
    if (rows.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No claimed rewards yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "When you redeem from the Redeem tab, your choices appear here with how many times you have claimed each one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Claimed rewards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Item ID, category, and how many times you redeemed each reward.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
            items(rows, key = { it.itemId }) { row ->
                ClaimedRewardRowCard(row = row)
            }
        }
    }
}

@Composable
private fun ClaimedRewardRowCard(row: ClaimedRewardRow) {
    val icon = redeemIconFor(row.iconName)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ID ${row.itemId} · ${row.category}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "×${row.timesRedeemed}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "redeemed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val RedeemSuccessGreen = Color(0xFF1B5E20)

@Composable
private fun RedeemSuccessOverlay(
    reward: RedeemItemEntity,
    confettiKey: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
        ) {
            PartyPopperBurst(
                modifier = Modifier.fillMaxSize(),
                triggerKey = confettiKey,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Redeemed!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = reward.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = reward.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "−${reward.costPoints} Eco‑karma",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedeemSuccessGreen,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Awesome")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EcoKarmaPointsDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "How Eco‑karma works",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Every action below adds to your total. Bonuses for cleanup go to the person who verifies the “after” photo on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SlabRow(
                    title = "Submit a waste report",
                    detail = "Photo, location, and types saved to the map",
                    points = EcoKarma.SUBMIT_REPORT,
                )
                SlabRow(
                    title = "Verify a cleanup",
                    detail = "After photo proves the spot is clean (one reward per report)",
                    points = EcoKarma.MARK_CLEANED,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tiers: Seedling → Sprout (25) → Green Champion (75) → Forest Guardian (200+).",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
}

@Composable
private fun SlabRow(
    title: String,
    detail: String,
    points: Int,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "+$points",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
