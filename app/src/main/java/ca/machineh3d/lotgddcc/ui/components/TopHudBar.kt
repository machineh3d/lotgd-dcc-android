package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.MailState
import ca.machineh3d.lotgddcc.model.PlayerState
import ca.machineh3d.lotgddcc.ui.theme.DccColors

@Composable
fun TopHudBar(
    title: String,
    player: PlayerState?,
    mail: MailState,
    friendsOnline: Int,
    friendsPending: Int,
    nextNewDaySeconds: Int,
    secondsPerGameDay: Int,
    pulseTrainIndicator: Boolean,
    onOpenStats: () -> Unit,
    onOpenComms: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenSettings: () -> Unit,
    onTapHpBar: () -> Unit,
    onTapXpBar: () -> Unit,
    onLogout: () -> Unit,
) {
    Surface(
        color = DccColors.Surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            // Row 1: Floor/Level pill, Area Title, Comms, Stats, Settings & Logout icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (player != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DccColors.Crimson.copy(alpha = 0.2f))
                            .clickable(onClick = onOpenStats)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FL ${player.floor} · LV ${player.level}",
                            color = DccColors.Crimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = player != null, onClick = onOpenStats)
                ) {
                    Text(
                        text = title,
                        color = DccColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (player != null) {
                        val subtitle = listOfNotNull(
                            player.title.takeIf { it.isNotBlank() },
                            player.name.takeIf { it.isNotBlank() }
                        ).joinToString(" ")
                        Text(
                            text = subtitle,
                            color = DccColors.Cyan,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (player != null) {
                    IconButton(onClick = onOpenFriends, modifier = Modifier.size(32.dp)) {
                        BadgedBox(
                            badge = {
                                if (friendsOnline > 0 || friendsPending > 0) {
                                    Badge(
                                        containerColor = if (friendsPending > 0) DccColors.Gold else DccColors.Emerald
                                    ) {
                                        Text(
                                            text = if (friendsOnline > 0) "$friendsOnline" else "!",
                                            color = DccColors.Background,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = "Squad & Friends ($friendsOnline online)",
                                tint = when {
                                    friendsPending > 0 -> DccColors.Gold
                                    friendsOnline > 0 -> DccColors.Emerald
                                    else -> DccColors.TextSecondary
                                },
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    IconButton(onClick = onOpenComms, modifier = Modifier.size(32.dp)) {
                        BadgedBox(
                            badge = {
                                if (mail.unread > 0) {
                                    Badge(containerColor = DccColors.Crimson) {
                                        Text(mail.unread.toString(), fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = "Syndicate Comms",
                                tint = if (mail.unread > 0) DccColors.Gold else DccColors.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    IconButton(onClick = onOpenStats, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Crawler Stats",
                            tint = DccColors.Cyan,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Client Settings",
                            tint = DccColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log Out",
                        tint = DccColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Row 2: Tap-to-act HP bar + XP-to-Trainer bar + Stamina / Gold / Tokens
            if (player != null) {
                Spacer(Modifier.height(6.dp))
                // Slim live-ticking Time-to-Next-Day progress bar
                if (nextNewDaySeconds > 0) {
                    var elapsedSec by remember(nextNewDaySeconds) { mutableIntStateOf(0) }
                    LaunchedEffect(nextNewDaySeconds) {
                        elapsedSec = 0
                        while (true) {
                            delay(1000L)
                            elapsedSec += 1
                        }
                    }

                    val cycleLen = if (secondsPerGameDay > 0) secondsPerGameDay else 21600
                    // Guard against servers/bridges returning a raw Unix timestamp instead of remaining seconds
                    val clampedInitial = if (nextNewDaySeconds > cycleLen) {
                        val mod = nextNewDaySeconds % cycleLen
                        if (mod == 0) cycleLen else mod
                    } else {
                        nextNewDaySeconds
                    }
                    val liveRem = (clampedInitial - elapsedSec).let {
                        if (it < 0) (cycleLen + (it % cycleLen)) % cycleLen else it
                    }
                    val dayProgress = ((cycleLen - liveRem).toFloat() / cycleLen.toFloat()).coerceIn(0f, 1f)
                    val soon = liveRem <= 900 // final 15 minutes
                    val dayAccent = if (soon) DccColors.Gold else DccColors.Cyan
                    val hrs = liveRem / 3600
                    val mins = (liveRem % 3600) / 60
                    val secs = liveRem % 60
                    val countdownText = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onOpenStats)
                    ) {
                        Text(
                            text = "DAY",
                            color = dayAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        )
                        LinearProgressIndicator(
                            progress = { dayProgress },
                            color = dayAccent,
                            trackColor = DccColors.SurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = countdownText,
                            color = if (soon) DccColors.Gold else DccColors.TextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                    Spacer(Modifier.height(6.dp))
                val hpFraction = (player.hp.toFloat() / player.maxHp.coerceAtLeast(1).toFloat())
                    .coerceIn(0f, 1f)
                val hpColor = when {
                    !player.alive -> Color(0xFF94A3B8)
                    hpFraction > 0.5f -> DccColors.Emerald
                    hpFraction > 0.25f -> DccColors.Gold
                    else -> DccColors.Crimson
                }

                val isMaxLevel = player.level >= player.maxLevel
                val expBase = player.expCurrentLevel.coerceAtLeast(0)
                val expTarget = player.expNextLevel.coerceAtLeast(expBase + 1)
                val expReady = !isMaxLevel && player.experience >= expTarget
                val expFraction = when {
                    isMaxLevel || expReady -> 1f
                    else -> ((player.experience - expBase).toFloat() / (expTarget - expBase).toFloat())
                        .coerceIn(0f, 1f)
                }
                val expColor = when {
                    isMaxLevel -> DccColors.Crimson
                    expReady -> DccColors.Gold
                    else -> DccColors.Violet
                }
                val expLeftLabel = when {
                    isMaxLevel -> "MAX"
                    expReady -> "TRAIN!"
                    else -> "XP"
                }
                val expRightLabel = when {
                    isMaxLevel -> formatCompact(player.experience)
                    else -> "${formatCompact(player.experience)}/${formatCompact(expTarget)}"
                }

                val trainPulseAlpha = if (expReady && pulseTrainIndicator) {
                    val transition = rememberInfiniteTransition(label = "trainPulse")
                    val animated by transition.animateFloat(
                        initialValue = 0.42f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "trainAlpha",
                    )
                    animated
                } else {
                    1f
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onTapHpBar)
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (player.alive) "HP" else "DEAD",
                                color = hpColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            Text(
                                text = "${player.hp}/${player.maxHp}",
                                color = DccColors.TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { hpFraction },
                            color = hpColor,
                            trackColor = DccColors.SurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (expReady && pulseTrainIndicator) {
                                    DccColors.Gold.copy(alpha = 0.14f * trainPulseAlpha)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable(onClick = onTapXpBar)
                            .padding(horizontal = if (expReady && pulseTrainIndicator) 3.dp else 0.dp, vertical = 2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = expLeftLabel,
                                color = expColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer { alpha = trainPulseAlpha },
                            )
                            Text(
                                text = expRightLabel,
                                color = if (expReady) DccColors.Gold else DccColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (expReady && pulseTrainIndicator) trainPulseAlpha else 1f
                                },
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { expFraction },
                            color = expColor,
                            trackColor = DccColors.SurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .graphicsLayer { alpha = trainPulseAlpha }
                        )
                    }

                    HudMetricPill(
                        icon = Icons.Filled.Bolt,
                        value = "${player.turns}",
                        tint = DccColors.Cyan,
                        contentDescription = "Stamina"
                    )
                    HudMetricPill(
                        icon = Icons.Filled.MonetizationOn,
                        value = formatCompact(player.gold),
                        tint = DccColors.Gold,
                        contentDescription = "Gold Credits"
                    )
                    HudMetricPill(
                        icon = Icons.Filled.Diamond,
                        value = "${player.gems}",
                        tint = DccColors.Violet,
                        contentDescription = "Sponsor Tokens"
                    )
                }
            }
        }
    }
}

@Composable
private fun HudMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color,
    contentDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(DccColors.SurfaceVariant)
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = value,
            color = DccColors.TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun formatCompact(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000f)
    n >= 10_000 -> String.format("%.1fk", n / 1_000f)
    else -> n.toString()
}
