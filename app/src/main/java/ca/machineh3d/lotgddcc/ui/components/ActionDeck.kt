package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.NavGroup
import ca.machineh3d.lotgddcc.model.NavItem
import ca.machineh3d.lotgddcc.model.PlayerState
import ca.machineh3d.lotgddcc.ui.theme.DccColors

private val HOTKEY_PREFIX_REGEX = Regex("""^\([A-Za-z0-9?/,]\)\s*""")

private fun cleanNavLabel(raw: String): String =
    raw.replace(HOTKEY_PREFIX_REGEX, "").trim().ifBlank { raw.trim() }

private fun compactButtonLabel(raw: String): String {
    val clean = cleanNavLabel(raw)
    return when {
        clean.startsWith("Return to ", ignoreCase = true) && clean.length > 18 -> "Return"
        clean.equals("Look for something to kill", ignoreCase = true) -> "Look for a Fight"
        else -> clean
    }
}

private enum class HubSheetTab { SHOPS, SAFE_ROOM, MENU }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionDeck(
    pageType: String,
    badnav: Boolean,
    navGroups: List<NavGroup>,
    player: PlayerState?,
    autoHealEnabled: Boolean,
    autoHealThresholdPercent: Int,
    enabled: Boolean,
    onNavigate: (String) -> Unit,
    onRecoverBadNav: () -> Unit,
) {
    var activeHubSheet by remember(navGroups) { mutableStateOf<HubSheetTab?>(null) }
    var showSubpageSheet by remember(navGroups) { mutableStateOf(false) }
    var showCombatSkillsSheet by remember(navGroups) { mutableStateOf(false) }

    val allCombatItems = remember(navGroups) { navGroups.flatMap { it.items } }
    val combatFightBtn = remember(allCombatItems) {
        allCombatItems.firstOrNull {
            val u = it.url.lowercase()
            u.contains("op=fight") && !u.contains("skill=") && !u.contains("auto=")
        } ?: allCombatItems.firstOrNull { it.url.contains("op=fight", ignoreCase = true) }
    }
    val combatFleeBtn = remember(allCombatItems) {
        allCombatItems.firstOrNull { it.url.contains("op=run", ignoreCase = true) }
    }

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
                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                        text = "Resync Syndicate Feed",
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

            // 1. Combat Mode: slim 1-row Fight / Run / Skills deck
            if (pageType == "combat") {
                val specialItems = allCombatItems.filter { it != combatFightBtn && it != combatFleeBtn }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (combatFightBtn != null) {
                        Button(
                            enabled = enabled,
                            onClick = { onNavigate(combatFightBtn.url) },
                            colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 11.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = cleanNavLabel(combatFightBtn.label),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (combatFleeBtn != null) {
                        OutlinedButton(
                            enabled = enabled,
                            onClick = { onNavigate(combatFleeBtn.url) },
                            border = BorderStroke(1.dp, DccColors.Gold),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 11.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(
                                text = cleanNavLabel(combatFleeBtn.label),
                                color = DccColors.Gold,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (specialItems.size == 1) {
                        val single = specialItems.first()
                        OutlinedButton(
                            enabled = enabled,
                            onClick = { onNavigate(single.url) },
                            border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cleanNavLabel(single.label),
                                color = DccColors.Cyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else if (specialItems.size > 1) {
                        OutlinedButton(
                            enabled = enabled,
                            onClick = { showCombatSkillsSheet = true },
                            border = BorderStroke(1.dp, DccColors.Cyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = DccColors.Cyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Skills (${specialItems.size})",
                                color = DccColors.Cyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
                return@Column
            }

            // 2. Safe Room Hub Mode: sleek 4-tab bottom bar
            val allHubItems = navGroups.flatMap { it.items }
            val corridorsItem = allHubItems.firstOrNull {
                it.url.startsWith("forest.php", ignoreCase = true) ||
                    it.url.contains("/forest.php", ignoreCase = true)
            }
            if (corridorsItem != null && (pageType == "village" || allHubItems.size >= 12)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HubBarTab(
                        icon = Icons.Filled.Explore,
                        label = "Corridors",
                        tint = DccColors.Crimson,
                        enabled = enabled,
                        onClick = { onNavigate(corridorsItem.url) },
                        modifier = Modifier.weight(1f)
                    )
                    HubBarTab(
                        icon = Icons.Filled.Storefront,
                        label = "Shops",
                        tint = DccColors.Gold,
                        enabled = enabled,
                        onClick = { activeHubSheet = HubSheetTab.SHOPS },
                        modifier = Modifier.weight(1f)
                    )
                    HubBarTab(
                        icon = Icons.Filled.Home,
                        label = "Safe Room",
                        tint = DccColors.Emerald,
                        enabled = enabled,
                        onClick = { activeHubSheet = HubSheetTab.SAFE_ROOM },
                        modifier = Modifier.weight(1f)
                    )
                    HubBarTab(
                        icon = Icons.Filled.Menu,
                        label = "Menu",
                        tint = DccColors.Cyan,
                        enabled = enabled,
                        onClick = { activeHubSheet = HubSheetTab.MENU },
                        modifier = Modifier.weight(1f)
                    )
                }
                return@Column
            }

            // 3. Subpages: slim 1-row bar with smart Corridors primary button (Hunt vs Auto-Heal)
            val allItems = navGroups.flatMap { it.items }
            if (allItems.size <= 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allItems.forEachIndexed { idx, item ->
                        val isReturn = isReturnLink(item)
                        if (idx == 0 && !isReturn) {
                            Button(
                                enabled = enabled,
                                onClick = { onNavigate(item.url) },
                                colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = compactButtonLabel(item.label),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        } else {
                            OutlinedButton(
                                enabled = enabled,
                                onClick = { onNavigate(item.url) },
                                border = BorderStroke(1.dp, DccColors.Outline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DccColors.SurfaceVariant,
                                    contentColor = DccColors.TextPrimary,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isReturn) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = cleanNavLabel(item.label),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            } else {
                val returnItem = allItems.firstOrNull { isReturnLink(it) }
                val searchFightItem = allItems.firstOrNull { isHuntSearchLink(it) }
                val healerItem = allItems.firstOrNull { isHealerLink(it) }

                val hpPct = if (player != null && player.maxHp > 0) {
                    (player.hp * 100) / player.maxHp
                } else {
                    100
                }
                val shouldAutoHeal = autoHealEnabled &&
                    player != null &&
                    player.alive &&
                    hpPct <= autoHealThresholdPercent &&
                    healerItem != null

                val primaryItem = when {
                    shouldAutoHeal -> healerItem
                    searchFightItem != null -> searchFightItem
                    else -> null
                }
                val isAutoHealButton = shouldAutoHeal && primaryItem == healerItem

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (returnItem != null) {
                        OutlinedButton(
                            enabled = enabled,
                            onClick = { onNavigate(returnItem.url) },
                            border = BorderStroke(1.dp, DccColors.Outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DccColors.SurfaceVariant,
                                contentColor = DccColors.TextPrimary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = compactButtonLabel(returnItem.label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (primaryItem != null) {
                        Button(
                            enabled = enabled,
                            onClick = { onNavigate(primaryItem.url) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAutoHealButton) DccColors.Emerald else DccColors.Crimson,
                                contentColor = if (isAutoHealButton) DccColors.Background else Color.White,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                            modifier = Modifier.weight(1.35f)
                        ) {
                            if (isAutoHealButton) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = compactButtonLabel(primaryItem.label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    OutlinedButton(
                        enabled = enabled,
                        onClick = { showSubpageSheet = true },
                        border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = null,
                            tint = DccColors.Cyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (primaryItem == null) "All Options (${allItems.size})" else "All (${allItems.size})",
                            color = DccColors.Cyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    // Hub Bottom Sheets (Shops, Safe Room, Menu)
    activeHubSheet?.let { tab ->
        val (sheetTitle, groups) = partitionHubGroups(navGroups, tab)
        ModalBottomSheet(
            onDismissRequest = { activeHubSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DccColors.Surface,
        ) {
            ActionSheetContent(
                title = sheetTitle,
                groups = groups.ifEmpty { navGroups },
                onSelect = { url ->
                    activeHubSheet = null
                    onNavigate(url)
                }
            )
        }
    }

    // Subpage All-Actions Bottom Sheet
    if (showSubpageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubpageSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DccColors.Surface,
        ) {
            ActionSheetContent(
                title = "Available Actions",
                groups = navGroups,
                onSelect = { url ->
                    showSubpageSheet = false
                    onNavigate(url)
                }
            )
        }
    }

    // Combat Skills Bottom Sheet — exclude ONLY the primary Fight and Run buttons (keep all op=fight&skill=... & auto=...)
    if (showCombatSkillsSheet) {
        val skillGroups = navGroups.mapNotNull { g ->
            val filtered = g.items.filter { it != combatFightBtn && it != combatFleeBtn }
            if (filtered.isEmpty()) null else g.copy(items = filtered)
        }
        ModalBottomSheet(
            onDismissRequest = { showCombatSkillsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DccColors.Surface,
        ) {
            ActionSheetContent(
                title = "Combat Skills & Options",
                groups = skillGroups.ifEmpty { navGroups },
                onSelect = { url ->
                    showCombatSkillsSheet = false
                    onNavigate(url)
                }
            )
        }
    }
}

@Composable
private fun HubBarTab(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else DccColors.TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (enabled) DccColors.TextPrimary else DccColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionSheetContent(
    title: String,
    groups: List<NavGroup>,
    onSelect: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = title,
            color = DccColors.Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        for (group in groups) {
            if (group.items.isEmpty()) continue
            Surface(
                color = DccColors.SurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        text = group.header.uppercase(),
                        color = DccColors.Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    group.items.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = DccColors.Outline.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item.url) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = cleanNavLabel(item.label),
                                color = DccColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = DccColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isReturnLink(item: NavItem): Boolean {
    val lbl = cleanNavLabel(item.label).lowercase()
    val url = item.url.lowercase()
    return lbl.startsWith("return") ||
        lbl.startsWith("back") ||
        url == "village.php" ||
        url.startsWith("village.php?")
}

private fun isHuntSearchLink(item: NavItem): Boolean {
    val lbl = cleanNavLabel(item.label).lowercase()
    val url = item.url.lowercase()
    return (url.contains("forest.php") && url.contains("op=search") && !url.contains("type=slum") && !url.contains("type=thrill")) ||
        lbl.contains("look for something to kill")
}

private fun isHealerLink(item: NavItem): Boolean {
    val lbl = cleanNavLabel(item.label).lowercase()
    val url = item.url.lowercase()
    return url.contains("healer.php") || lbl.contains("healer")
}

private fun partitionHubGroups(
    navGroups: List<NavGroup>,
    tab: HubSheetTab,
): Pair<String, List<NavGroup>> {
    fun isShopGroup(g: NavGroup): Boolean {
        val h = g.header.lowercase()
        return h.contains("trade") || h.contains("market") || h.contains("tactical") ||
            h.contains("guild") || h.contains("blades") ||
            g.items.any {
                val u = it.url.lowercase()
                u.contains("weapons.php") || u.contains("armor.php") ||
                    u.contains("train.php") || u.contains("bank.php") ||
                    u.contains("stables.php") || u.contains("gypsy.php")
            }
    }

    fun isSafeRoomGroup(g: NavGroup): Boolean {
        val h = g.header.lowercase()
        return h.contains("safe room") || h.contains("lounge") || h.contains("tavern") ||
            h.contains("dungeon") || h.contains("gates") || h.contains("boss") ||
            g.items.any {
                val u = it.url.lowercase()
                u.contains("inn.php") || u.contains("gardens.php") ||
                    u.contains("clan.php") || u.contains("personalspace") ||
                    u.contains("bopcaprotector") || u.contains("forestcasino") ||
                    u.contains("systemai") || u.contains("sponsors") ||
                    u.contains("clubvanquisher") || u.contains("turnbank") ||
                    u.contains("dungeonbosses") || u.contains("factionwars")
            }
    }

    return when (tab) {
        HubSheetTab.SHOPS -> "Shops & Training" to navGroups.filter { isShopGroup(it) }
        HubSheetTab.SAFE_ROOM -> "Safe Room & Dungeon" to navGroups.filter { !isShopGroup(it) && isSafeRoomGroup(it) }
        HubSheetTab.MENU -> {
            val remaining = navGroups.filter { !isShopGroup(it) && !isSafeRoomGroup(it) }
            "Broadcast & Menu" to (if (remaining.isNotEmpty()) remaining else navGroups)
        }
    }
}

/**
 * Renders all choices directly inline below the narrative on multi-option selection screens
 * (starting class/specialty, race selection, random encounters, room menus) so players
 * immediately see every option instead of only one arbitrary button in the bottom bar.
 */
@Composable
fun InlineChoiceDeck(
    pageType: String,
    badnav: Boolean,
    navGroups: List<NavGroup>,
    enabled: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (badnav || pageType == "combat") return
    val allItems = remember(navGroups) { navGroups.flatMap { it.items } }
    if (allItems.size !in 3..14) return

    val hasCorridorsHub = allItems.any {
        it.url.startsWith("forest.php", ignoreCase = true) ||
            it.url.contains("/forest.php", ignoreCase = true)
    }
    if (hasCorridorsHub && (pageType == "village" || allItems.size >= 12)) return
    if (allItems.any { isHuntSearchLink(it) }) return

    val choiceItems = allItems.filterNot { isReturnLink(it) }
    if (choiceItems.size < 2) return

    Surface(
        color = DccColors.Surface,
        border = BorderStroke(1.dp, DccColors.Cyan.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            val isMordecaiHandbook = allItems.any { it.url.contains("dcctutor", ignoreCase = true) }
            Text(
                text = if (isMordecaiHandbook) {
                    "SURVIVAL HANDBOOK (${choiceItems.size})"
                } else {
                    "CHOOSE AN OPTION (${choiceItems.size})"
                },
                color = if (isMordecaiHandbook) DccColors.Emerald else DccColors.Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
            )

            for (item in choiceItems) {
                Surface(
                    color = DccColors.SurfaceVariant,
                    border = BorderStroke(1.dp, DccColors.Outline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onNavigate(item.url) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = cleanNavLabel(item.label),
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = DccColors.Cyan,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
