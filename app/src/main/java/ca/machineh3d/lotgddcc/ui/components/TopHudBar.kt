package ca.machineh3d.lotgddcc.ui.components

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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onOpenStats: () -> Unit,
    onOpenComms: () -> Unit,
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
            // Row 1: Floor/Level pill, Area Title, Comms & Stats icons
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
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "FL ${player.floor} · LV ${player.level}",
                            color = DccColors.Crimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = player != null, onClick = onOpenStats)
                ) {
                    Text(
                        text = title,
                        color = DccColors.TextPrimary,
                        fontSize = 15.sp,
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
                    IconButton(onClick = onOpenComms, modifier = Modifier.size(36.dp)) {
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
                            )
                        }
                    }

                    IconButton(onClick = onOpenStats, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Crawler Stats",
                            tint = DccColors.Cyan,
                        )
                    }
                }

                IconButton(onClick = onLogout, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log Out",
                        tint = DccColors.TextSecondary,
                    )
                }
            }

            // Row 2: Compact side-by-side HP bar + XP-to-Trainer bar + Stamina / Gold / Tokens
            if (player != null) {
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Side-by-side HP Bar
                    Column(modifier = Modifier.weight(1f)) {
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

                    // Side-by-side XP-to-Trainer Bar
                    Column(modifier = Modifier.weight(1f)) {
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
                            )
                            Text(
                                text = expRightLabel,
                                color = if (expReady) DccColors.Gold else DccColors.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
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
