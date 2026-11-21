package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.NavGroup
import ca.machineh3d.lotgddcc.model.NavItem
import ca.machineh3d.lotgddcc.ui.theme.DccColors

@Composable
fun ActionDeck(
    pageType: String,
    badnav: Boolean,
    navGroups: List<NavGroup>,
    enabled: Boolean,
    onNavigate: (String) -> Unit,
    onRecoverBadNav: () -> Unit,
) {
    Surface(
        color = DccColors.Surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, DccColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (badnav) {
                Button(
                    enabled = enabled,
                    onClick = onRecoverBadNav,
                    colors = ButtonDefaults.buttonColors(containerColor = DccColors.Gold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = DccColors.Background)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Resync Syndicate Feed (BadNav Recovery)",
                        color = DccColors.Background,
                        fontWeight = FontWeight.Bold
                    )
                }
                return@Column
            }

            if (navGroups.isEmpty()) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = { onNavigate("village.php") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Return to Safe Room Hub")
                }
                return@Column
            }

            // Combat mode: highlight primary Fight / Auto / Flee buttons right at thumb level
            if (pageType == "combat") {
                val allItems = navGroups.flatMap { it.items }
                val fightBtn = allItems.firstOrNull { it.url.contains("op=fight", ignoreCase = true) }
                val fleeBtn = allItems.firstOrNull { it.url.contains("op=run", ignoreCase = true) }
                val specialItems = allItems.filter { it != fightBtn && it != fleeBtn }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (fightBtn != null) {
                        Button(
                            enabled = enabled,
                            onClick = { onNavigate(fightBtn.url) },
                            colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text(fightBtn.label, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    if (fleeBtn != null) {
                        OutlinedButton(
                            enabled = enabled,
                            onClick = { onNavigate(fleeBtn.url) },
                            border = BorderStroke(1.dp, DccColors.Gold),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fleeBtn.label, color = DccColors.Gold, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                if (specialItems.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        for (skill in specialItems) {
                            OutlinedButton(
                                enabled = enabled,
                                onClick = { onNavigate(skill.url) },
                                border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = skill.label,
                                    color = DccColors.Cyan,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                return@Column
            }

            // Standard & Safe Room Hub mode: category chips + clean 2-column action grid
            var selectedGroupIndex by remember(navGroups) { mutableIntStateOf(0) }
            val safeIndex = selectedGroupIndex.coerceIn(0, navGroups.lastIndex)

            if (navGroups.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    navGroups.forEachIndexed { idx, group ->
                        FilterChip(
                            selected = idx == safeIndex,
                            onClick = { selectedGroupIndex = idx },
                            label = {
                                Text(
                                    text = group.header,
                                    fontSize = 12.sp,
                                    fontWeight = if (idx == safeIndex) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DccColors.Crimson.copy(alpha = 0.25f),
                                selectedLabelColor = DccColors.TextPrimary,
                                containerColor = DccColors.SurfaceVariant,
                                labelColor = DccColors.TextSecondary,
                            )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            val items = navGroups[safeIndex].items
            ActionGrid(items = items, enabled = enabled, onNavigate = onNavigate)
        }
    }
}

@Composable
private fun ActionGrid(
    items: List<NavItem>,
    enabled: Boolean,
    onNavigate: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val rows = items.chunked(2)
        for (rowItems in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (item in rowItems) {
                    OutlinedButton(
                        enabled = enabled,
                        onClick = { onNavigate(item.url) },
                        border = BorderStroke(1.dp, DccColors.Outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DccColors.SurfaceVariant,
                            contentColor = DccColors.TextPrimary,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
