package com.example.paryavaran_kavalu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Leading navigation slot — pairs with [MaterialTheme.colorScheme.primary] app bars (same style as
 * the map screen): respects status bar insets and avoids overlap with system bars.
 */
enum class AppBarNavigation {
    /** Standard left [ArrowBack]. */
    Back,
    /** Empty space matching icon width so centered titles stay aligned. */
    None,
}

/**
 * Standard primary app bar: **[Back or spacer] · Debug · Title · [Help?] · [Eco‑karma?] · Profile** — blue strip
 * matching the original map screen (`primary`, status-bar safe).
 */
@Composable
fun ParyavaranPrimaryAppBar(
    title: @Composable () -> Unit,
    navigation: AppBarNavigation,
    onNavigationClick: () -> Unit,
    onDebugClick: () -> Unit,
    /** When non-null, the Eco‑karma leaf action is shown before profile (omit on the Eco‑karma screen). */
    onEcoKarmaClick: (() -> Unit)? = null,
    /** When non-null, the profile action is shown (omit on the welcome / home guide screen). */
    onProfileClick: (() -> Unit)? = null,
    profileContentDescription: String = "",
    modifier: Modifier = Modifier,
    navigationContentDescription: String = "Back",
    /** When non-null, a help icon is shown before the Eco‑karma action (e.g. points guide). */
    onHelpClick: (() -> Unit)? = null,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 2.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (navigation) {
                AppBarNavigation.Back -> {
                    IconButton(
                        onClick = onNavigationClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = navigationContentDescription,
                            tint = onPrimary,
                        )
                    }
                }
                AppBarNavigation.None -> {
                    Spacer(Modifier.width(48.dp))
                }
            }
            IconButton(
                onClick = onDebugClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = "Room database debug",
                    tint = onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                title()
            }
            if (onHelpClick != null) {
                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "How Eco‑karma points work",
                        tint = onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            if (onEcoKarmaClick != null) {
                IconButton(
                    onClick = onEcoKarmaClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EnergySavingsLeaf,
                        contentDescription = "Eco‑karma",
                        tint = onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            if (onProfileClick != null) {
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = profileContentDescription.ifEmpty { "Profile" },
                        tint = onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            // Keep title visually centred: left has nav slot + debug (2×48dp); balance when fewer right actions.
            val rightIconCount =
                (if (onHelpClick != null) 1 else 0) +
                    (if (onEcoKarmaClick != null) 1 else 0) +
                    (if (onProfileClick != null) 1 else 0)
            val balanceEndDp = ((2 - rightIconCount) * 48).coerceAtLeast(0)
            if (balanceEndDp > 0) {
                Spacer(Modifier.width(balanceEndDp.dp))
            }
        }
    }
}

@Composable
fun ParyavaranAppBarTitle(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = onPrimary.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
