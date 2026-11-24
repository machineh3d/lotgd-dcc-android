package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.model.TextSpan
import ca.machineh3d.lotgddcc.ui.theme.DccColors

enum class HostileArchetype(val label: String, val accent: Color) {
    GOBLIN("GOBLIN / DOZER", Color(0xFF84CC16)),
    ARACHNID("ARACHNID", Color(0xFFC084FC)),
    DRONE("DRONE / BOT", Color(0xFF22D3EE)),
    ELEMENTAL("ELEMENTAL", Color(0xFFFB923C)),
    UNDEAD("UNDEAD", Color(0xFFA5F3FC)),
    BEAST("BEAST", Color(0xFFF59E0B)),
    SKYFOWL("SKYFOWL", Color(0xFFF472B6)),
    MAGE("MAGE / DEITY", Color(0xFFE879F9)),
    BOSS("CROWNED BOSS", Color(0xFFFACC15)),
    RETICLE("HOSTILE", Color(0xFFFF4D4D)),
}

private enum class BattleOutcome {
    ACTIVE,
    VICTORY,
    DEFEAT,
}

private data class ParsedBattleState(
    val outcome: BattleOutcome,
    val enemyName: String,
    val enemyLevel: Int?,
    val enemyWeapon: String?,
    val enemyHp: Int,
    val enemyMaxHpFromPage: Int?,
    val damageDealt: Int? = null,
    val isCritDealt: Boolean = false,
    val damageTaken: Int? = null,
)

private val ENCOUNTER_REGEX = Regex(
    """You have encountered\s+(.+?)\s+which lunges at you with\s+(.+?)!""",
    RegexOption.IGNORE_CASE,
)
private val ENEMY_HP_REGEX = Regex(
    """(.+?)'s\s+Hitpoints(?:\s*\(Level\s*(\d+)\))?\s*:\s*(-?\d+)""",
    RegexOption.IGNORE_CASE,
)
private val VICTORY_REGEX = Regex(
    """You have (?:defeated|slain)\s+(.+?)!""",
    RegexOption.IGNORE_CASE,
)
private val DEFEAT_REGEX = Regex(
    """You have been (?:slain|defeated) by\s+(.+?)!""",
    RegexOption.IGNORE_CASE,
)
private val SLASH_HP_REGEX = Regex("""(-?\d+)\s*/\s*(\d+)\s*HP""", RegexOption.IGNORE_CASE)
private val DMG_DEALT_REGEX = Regex(
    """(?:You hit .+? for|RIPOSTE for|You strike .+? for)\s+(\d+)\s+points? of damage""",
    RegexOption.IGNORE_CASE,
)
private val DMG_TAKEN_REGEX = Regex(
    """(?:hits you for|strikes you for|bites you for|blasts you for)\s+(\d+)\s+points? of damage""",
    RegexOption.IGNORE_CASE,
)

/**
 * Filters out redundant plain-text paragraphs emitted by the server-side web `battlehud.php`
 * module when the native Compose Battle HUD is active, without touching LotGD narrative lines.
 */
private val WEB_HUD_STAT_LINE_REGEX = Regex(
    """(?:Lv\s*\d+\s*/\s*\d+\s*HP|\d+\s*/\s*\d+\s*HP\s*\(DEFEATED\))""",
    RegexOption.IGNORE_CASE,
)

fun filterRedundantWebBattleHudParagraphs(paragraphs: List<List<TextSpan>>): List<List<TextSpan>> {
    return paragraphs.filterNot { line ->
        val plain = line.joinToString("") { it.text }.trim()
        plain.startsWith("TACTICAL COMBAT HUD", ignoreCase = true) ||
            plain.startsWith("SYNDICATE COMBAT TELEMETRY", ignoreCase = true) ||
            plain.contains("LIVE CORRIDOR COMBAT TELEMETRY", ignoreCase = true) ||
            plain.contains("SYNDICATE DRONE FEED", ignoreCase = true) ||
            plain.equals("HOSTILE ELIMINATED", ignoreCase = true) ||
            plain.equals("CRITICAL BIOMETRIC FAILURE", ignoreCase = true) ||
            plain.startsWith("Equipped:", ignoreCase = true) ||
            plain.startsWith("Weapon:", ignoreCase = true) ||
            WEB_HUD_STAT_LINE_REGEX.containsMatchIn(plain)
    }
}

@Composable
fun BattleHudCard(
    page: DccPageResponse,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(page.currentUrl, page.paragraphs, page.player, page.pageType) {
        parseBattleState(page)
    }

    // Track persistent encounter metadata across rounds (max HP, weapon, level)
    var trackedEnemyName by remember { mutableStateOf("") }
    var trackedEnemyMaxHp by remember { mutableIntStateOf(0) }
    var trackedEnemyWeapon by remember { mutableStateOf<String?>(null) }
    var trackedEnemyLevel by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(parsed?.enemyName, parsed?.enemyHp, parsed?.enemyMaxHpFromPage, parsed?.enemyWeapon, parsed?.enemyLevel) {
        if (parsed == null) {
            if (page.pageType != "combat") {
                trackedEnemyName = ""
                trackedEnemyMaxHp = 0
                trackedEnemyWeapon = null
                trackedEnemyLevel = null
            }
            return@LaunchedEffect
        }

        val isNewEnemy = parsed.enemyName.isNotBlank() &&
            !parsed.enemyName.equals(trackedEnemyName, ignoreCase = true)

        if (isNewEnemy) {
            trackedEnemyName = parsed.enemyName
            trackedEnemyMaxHp = parsed.enemyMaxHpFromPage ?: parsed.enemyHp.coerceAtLeast(1)
            trackedEnemyWeapon = parsed.enemyWeapon
            trackedEnemyLevel = parsed.enemyLevel
        } else {
            val candidateMax = parsed.enemyMaxHpFromPage ?: parsed.enemyHp
            if (candidateMax > trackedEnemyMaxHp) {
                trackedEnemyMaxHp = candidateMax
            }
            if (!parsed.enemyWeapon.isNullOrBlank()) {
                trackedEnemyWeapon = parsed.enemyWeapon
            }
            if (parsed.enemyLevel != null) {
                trackedEnemyLevel = parsed.enemyLevel
            }
        }
    }

    if (parsed == null) return

    val enemyName = parsed.enemyName.ifBlank { trackedEnemyName.ifBlank { "Unknown Hostile" } }
    val enemyLevel = parsed.enemyLevel ?: trackedEnemyLevel
    val enemyWeapon = parsed.enemyWeapon ?: trackedEnemyWeapon
    val enemyMaxHp = maxOf(
        parsed.enemyMaxHpFromPage ?: 0,
        if (enemyName.equals(trackedEnemyName, ignoreCase = true)) trackedEnemyMaxHp else 0,
        parsed.enemyHp,
        1,
    )
    val archetype = remember(enemyName) { classifyHostileArchetype(enemyName) }

    when (parsed.outcome) {
        BattleOutcome.VICTORY -> VictoryBanner(
            enemyName = enemyName,
            enemyLevel = enemyLevel,
            archetype = archetype,
            modifier = modifier,
        )
        BattleOutcome.DEFEAT -> DefeatBanner(
            enemyName = enemyName,
            crawlerName = page.player?.name?.ifBlank { "Crawler" } ?: "Crawler",
            modifier = modifier,
        )
        BattleOutcome.ACTIVE -> ActiveCombatHud(
            crawlerName = page.player?.name?.ifBlank { "Crawler" } ?: "Crawler",
            crawlerLevel = page.player?.level ?: 1,
            crawlerHp = (page.player?.hp ?: 0).coerceAtLeast(0),
            crawlerMaxHp = (page.player?.maxHp ?: 1).coerceAtLeast(1),
            enemyName = enemyName,
            enemyLevel = enemyLevel,
            enemyWeapon = enemyWeapon,
            enemyHp = parsed.enemyHp.coerceAtLeast(0),
            enemyMaxHp = enemyMaxHp,
            damageDealt = parsed.damageDealt,
            isCritDealt = parsed.isCritDealt,
            damageTaken = parsed.damageTaken,
            archetype = archetype,
            modifier = modifier,
        )
    }
}

@Composable
private fun ActiveCombatHud(
    crawlerName: String,
    crawlerLevel: Int,
    crawlerHp: Int,
    crawlerMaxHp: Int,
    enemyName: String,
    enemyLevel: Int?,
    enemyWeapon: String?,
    enemyHp: Int,
    enemyMaxHp: Int,
    damageDealt: Int?,
    isCritDealt: Boolean,
    damageTaken: Int?,
    archetype: HostileArchetype,
    modifier: Modifier = Modifier,
) {
    val crawlerFrac by animateFloatAsState(
        targetValue = (crawlerHp.toFloat() / crawlerMaxHp.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(320),
        label = "crawlerHpBar",
    )
    val enemyFrac by animateFloatAsState(
        targetValue = (enemyHp.toFloat() / enemyMaxHp.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(320),
        label = "enemyHpBar",
    )

    Surface(
        color = DccColors.Surface,
        border = BorderStroke(1.dp, archetype.accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            archetype.accent.copy(alpha = 0.08f),
                            DccColors.Surface,
                        )
                    )
                )
                .padding(12.dp)
        ) {
            // Header telemetry row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(DccColors.Crimson)
                    )
                    Text(
                        text = "TACTICAL COMBAT HUD",
                        color = DccColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                }
                Surface(
                    color = archetype.accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, archetype.accent.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = archetype.label,
                        color = archetype.accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Side-by-side Crawler vs Hostile bars
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Crawler column
                CombatantColumn(
                    name = crawlerName,
                    levelLabel = "LV $crawlerLevel",
                    levelColor = DccColors.Cyan,
                    hp = crawlerHp,
                    maxHp = crawlerMaxHp,
                    fraction = crawlerFrac,
                    hitBadge = damageTaken?.let { "-$it" },
                    hitBadgeColor = DccColors.Crimson,
                    icon = {
                        CrawlerShieldIcon(size = 38.dp)
                    },
                    modifier = Modifier.weight(1f),
                )

                // Center VS divider badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(DccColors.SurfaceVariant)
                        .border(1.dp, DccColors.Outline, CircleShape)
                ) {
                    Text(
                        text = "VS",
                        color = DccColors.Gold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    )
                }

                // Hostile column
                CombatantColumn(
                    name = enemyName,
                    levelLabel = enemyLevel?.let { "LV $it" } ?: "HOSTILE",
                    levelColor = archetype.accent,
                    hp = enemyHp,
                    maxHp = enemyMaxHp,
                    fraction = enemyFrac,
                    hitBadge = damageDealt?.let { if (isCritDealt) "CRIT -$it!" else "-$it" },
                    hitBadgeColor = if (isCritDealt) DccColors.Gold else DccColors.Cyan,
                    icon = {
                        HostileArchetypeIcon(archetype = archetype, size = 38.dp)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Enemy Weapon bar
            if (!enemyWeapon.isNullOrBlank()) {
                Surface(
                    color = DccColors.Background.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, DccColors.Outline.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "WEAPON",
                            color = DccColors.Gold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp,
                        )
                        Text(
                            text = enemyWeapon,
                            color = DccColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatantColumn(
    name: String,
    levelLabel: String,
    levelColor: Color,
    hp: Int,
    maxHp: Int,
    fraction: Float,
    hitBadge: String? = null,
    hitBadgeColor: Color = DccColors.Crimson,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = hpBarColor(fraction)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = DccColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = levelLabel,
                    color = levelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        // Animated color-shifting HP bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DccColors.Background)
                .border(0.5.dp, DccColors.Outline, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            if (!hitBadge.isNullOrBlank()) {
                Surface(
                    color = hitBadgeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, hitBadgeColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(5.dp),
                ) {
                    Text(
                        text = hitBadge,
                        color = hitBadgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "HP",
                    color = DccColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "$hp / $maxHp",
                color = barColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun VictoryBanner(
    enemyName: String,
    enemyLevel: Int?,
    archetype: HostileArchetype,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = DccColors.Emerald.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, DccColors.Emerald.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            HostileArchetypeIcon(
                archetype = archetype,
                size = 42.dp,
                dimmed = true,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "HOSTILE ELIMINATED",
                    color = DccColors.Emerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                )
                val suffix = enemyLevel?.let { " (LV $it)" } ?: ""
                Text(
                    text = "$enemyName$suffix neutralized.",
                    color = DccColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = DccColors.Gold.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, DccColors.Gold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "WIN",
                    color = DccColors.Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DefeatBanner(
    enemyName: String,
    crawlerName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = DccColors.Crimson.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, DccColors.Crimson.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            CrawlerShieldIcon(
                size = 42.dp,
                accentColor = DccColors.Crimson,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CRITICAL BIOMETRIC FAILURE",
                    color = DccColors.Crimson,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                )
                val detail = if (enemyName.isNotBlank() && enemyName != "Unknown Hostile") {
                    "$crawlerName flatlined by $enemyName."
                } else {
                    "$crawlerName vitals lost. Emergency respawn protocol active."
                }
                Text(
                    text = detail,
                    color = DccColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun hpBarColor(fraction: Float): Color = when {
    fraction >= 0.55f -> DccColors.Emerald
    fraction >= 0.28f -> DccColors.Gold
    else -> DccColors.Crimson
}

/**
 * Maps any enemy name to one of the 10 Syndicate neon combat archetypes.
 */
fun classifyHostileArchetype(enemyName: String): HostileArchetype {
    val n = enemyName.lowercase()
    return when {
        listOf(
            "boss", "guardian", "stairwell", "neighborhood", "borough", "city boss",
            "chieftain", "king", "queen", "lord", "divider", "overlord", "warlord",
        ).any { n.contains(it) } -> HostileArchetype.BOSS

        listOf(
            "goblin", "hobgoblin", "dozer", "shamanker", "bomber", "murder dozer",
        ).any { n.contains(it) } -> HostileArchetype.GOBLIN

        listOf(
            "spider", "arachnid", "scolopendra", "centipede", "bug", "beetle",
            "scorpion", "mantis", "roach", "wasp", "ant",
        ).any { n.contains(it) } -> HostileArchetype.ARACHNID

        listOf(
            "drone", "bot", "robot", "turret", "mimic", "cleaner", "construct",
            "mech", "automaton", "android", "cyborg", "golem", "sentinel",
        ).any { n.contains(it) } -> HostileArchetype.DRONE

        listOf(
            "fire", "flame", "ember", "lava", "elemental", "inferno", "acid",
            "plasma", "slime", "ooze", "magma", "frost", "ice", "lightning",
        ).any { n.contains(it) } -> HostileArchetype.ELEMENTAL

        listOf(
            "undead", "zombie", "skeleton", "ghost", "ghoul", "grimaldi", "shade",
            "wraith", "lich", "vampire", "mummy", "specter", "reaper", "corpse",
        ).any { n.contains(it) } -> HostileArchetype.UNDEAD

        listOf(
            "llama", "dingo", "swine", "boar", "bear", "wolf", "hound", "rat",
            "cat", "toad", "beast", "brute", "hoarder", "badger", "hyena", "dog",
            "troll", "ogre", "orc", "minotaur", "rhino",
        ).any { n.contains(it) } -> HostileArchetype.BEAST

        listOf(
            "bird", "fowl", "skyfowl", "harpy", "caprid", "goat", "bat", "crow",
            "wing", "wyvern", "dragon", "hawk", "owl", "vulture", "griffin",
        ).any { n.contains(it) } -> HostileArchetype.SKYFOWL

        listOf(
            "mage", "wizard", "warlock", "cleric", "priest", "deity", "god",
            "cultist", "leprechaun", "clurichaun", "demon", "imp", "witch",
            "sorcerer", "necromancer", "shaman", "fae", "fairy", "djinn",
        ).any { n.contains(it) } -> HostileArchetype.MAGE

        else -> HostileArchetype.RETICLE
    }
}

private fun parseBattleState(page: DccPageResponse): ParsedBattleState? {
    val lines = page.paragraphs.map { spans -> spans.joinToString("") { it.text }.trim() }
    if (lines.isEmpty()) return null
    val fullText = lines.joinToString("\n")

    val encounterMatch = ENCOUNTER_REGEX.find(fullText)
    val encounterName = encounterMatch?.groupValues?.getOrNull(1)?.trim()
    val encounterWeapon = encounterMatch?.groupValues?.getOrNull(2)?.trim()

    val hpMatches = ENEMY_HP_REGEX.findAll(fullText).toList()
    val firstHpMatch = hpMatches.firstOrNull()
    val lastHpMatch = hpMatches.lastOrNull()

    val victoryMatch = VICTORY_REGEX.find(fullText)
    val defeatMatch = DEFEAT_REGEX.find(fullText)

    val hasVictoryText = victoryMatch != null ||
        fullText.contains("HOSTILE ELIMINATED", ignoreCase = true)
    val hasDefeatText = defeatMatch != null ||
        fullText.contains("CRITICAL BIOMETRIC FAILURE", ignoreCase = true) ||
        (page.player != null && !page.player.alive &&
            (page.pageType == "combat" || fullText.contains("You have been slain", ignoreCase = true)))

    // Extract optional server-provided X / Y HP from battlehud.php if present
    val slashMatches = SLASH_HP_REGEX.findAll(fullText).toList()
    val enemySlashMax = if (slashMatches.size >= 2) {
        slashMatches.last().groupValues[2].toIntOrNull()
    } else {
        null
    }

    if (hasDefeatText) {
        val killer = defeatMatch?.groupValues?.getOrNull(1)?.trim()
            ?: lastHpMatch?.groupValues?.getOrNull(1)?.trim()
            ?: encounterName
            ?: ""
        return ParsedBattleState(
            outcome = BattleOutcome.DEFEAT,
            enemyName = killer,
            enemyLevel = lastHpMatch?.groupValues?.getOrNull(2)?.toIntOrNull(),
            enemyWeapon = encounterWeapon,
            enemyHp = lastHpMatch?.groupValues?.getOrNull(3)?.toIntOrNull() ?: 0,
            enemyMaxHpFromPage = enemySlashMax,
        )
    }

    if (hasVictoryText) {
        val defeated = victoryMatch?.groupValues?.getOrNull(1)?.trim()
            ?: lastHpMatch?.groupValues?.getOrNull(1)?.trim()
            ?: encounterName
            ?: "Hostile"
        return ParsedBattleState(
            outcome = BattleOutcome.VICTORY,
            enemyName = defeated,
            enemyLevel = lastHpMatch?.groupValues?.getOrNull(2)?.toIntOrNull(),
            enemyWeapon = encounterWeapon,
            enemyHp = 0,
            enemyMaxHpFromPage = enemySlashMax,
        )
    }

    if (lastHpMatch != null) {
        val enemyName = lastHpMatch.groupValues[1].trim()
        val enemyLevel = lastHpMatch.groupValues[2].toIntOrNull()
        val currentHp = lastHpMatch.groupValues[3].toIntOrNull() ?: 0
        val initialHp = firstHpMatch?.groupValues?.getOrNull(3)?.toIntOrNull()
        val maxCandidate = maxOf(enemySlashMax ?: 0, initialHp ?: 0, currentHp).takeIf { it > 0 }
        val dealtTotal = DMG_DEALT_REGEX.findAll(fullText)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .sum()
            .takeIf { it > 0 }
        val takenTotal = DMG_TAKEN_REGEX.findAll(fullText)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .sum()
            .takeIf { it > 0 }
        val critDealt = fullText.contains("CRITICAL", ignoreCase = true)
        return ParsedBattleState(
            outcome = if (currentHp <= 0) BattleOutcome.VICTORY else BattleOutcome.ACTIVE,
            enemyName = enemyName,
            enemyLevel = enemyLevel,
            enemyWeapon = encounterWeapon,
            enemyHp = currentHp,
            enemyMaxHpFromPage = maxCandidate,
            damageDealt = dealtTotal,
            isCritDealt = critDealt,
            damageTaken = takenTotal,
        )
    }

    return null
}

// ============================================================================
// Neon Tactical Vector Icons (Cyan Crawler Shield + 10 Hostile Archetypes)
// ============================================================================

@Composable
fun CrawlerShieldIcon(
    size: Dp = 38.dp,
    accentColor: Color = DccColors.Cyan,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D121B))
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val shield = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                lineTo(w * 0.86f, h * 0.22f)
                lineTo(w * 0.82f, h * 0.56f)
                quadraticTo(w * 0.72f, h * 0.82f, w * 0.50f, h * 0.94f)
                quadraticTo(w * 0.28f, h * 0.82f, w * 0.18f, h * 0.56f)
                lineTo(w * 0.14f, h * 0.22f)
                close()
            }
            drawPath(shield, color = accentColor.copy(alpha = 0.18f))
            drawPath(shield, color = accentColor, style = stroke)

            // Inner tactical chevron
            val chevron = Path().apply {
                moveTo(w * 0.34f, h * 0.54f)
                lineTo(w * 0.50f, h * 0.36f)
                lineTo(w * 0.66f, h * 0.54f)
            }
            drawPath(chevron, color = accentColor, style = stroke)
            drawCircle(color = accentColor, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.66f))
        }
    }
}

@Composable
fun HostileArchetypeIcon(
    archetype: HostileArchetype,
    size: Dp = 38.dp,
    dimmed: Boolean = false,
) {
    val accent = if (dimmed) archetype.accent.copy(alpha = 0.6f) else archetype.accent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D121B))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
    ) {
        Canvas(modifier = Modifier.size(size * 0.70f)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (archetype) {
                HostileArchetype.GOBLIN -> drawGoblin(w, h, accent, stroke)
                HostileArchetype.ARACHNID -> drawArachnid(w, h, accent, stroke)
                HostileArchetype.DRONE -> drawDrone(w, h, accent, stroke)
                HostileArchetype.ELEMENTAL -> drawElemental(w, h, accent, stroke)
                HostileArchetype.UNDEAD -> drawUndead(w, h, accent, stroke)
                HostileArchetype.BEAST -> drawBeast(w, h, accent, stroke)
                HostileArchetype.SKYFOWL -> drawSkyfowl(w, h, accent, stroke)
                HostileArchetype.MAGE -> drawMage(w, h, accent, stroke)
                HostileArchetype.BOSS -> drawBoss(w, h, accent, stroke)
                HostileArchetype.RETICLE -> drawReticle(w, h, accent, stroke)
            }
            if (dimmed) {
                drawLine(
                    color = DccColors.Emerald,
                    start = Offset(w * 0.14f, h * 0.86f),
                    end = Offset(w * 0.86f, h * 0.14f),
                    strokeWidth = w * 0.09f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.drawGoblin(w: Float, h: Float, c: Color, stroke: Stroke) {
    // Pointed goblin ears + angular head
    val head = Path().apply {
        moveTo(w * 0.28f, h * 0.36f)
        lineTo(w * 0.06f, h * 0.26f)
        lineTo(w * 0.24f, h * 0.52f)
        lineTo(w * 0.34f, h * 0.80f)
        lineTo(w * 0.50f, h * 0.88f)
        lineTo(w * 0.66f, h * 0.80f)
        lineTo(w * 0.76f, h * 0.52f)
        lineTo(w * 0.94f, h * 0.26f)
        lineTo(w * 0.72f, h * 0.36f)
        quadraticTo(w * 0.50f, h * 0.16f, w * 0.28f, h * 0.36f)
        close()
    }
    drawPath(head, color = c.copy(alpha = 0.16f))
    drawPath(head, color = c, style = stroke)
    // Glowing slanted eyes
    drawLine(c, Offset(w * 0.33f, h * 0.48f), Offset(w * 0.44f, h * 0.53f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.67f, h * 0.48f), Offset(w * 0.56f, h * 0.53f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    // Jagged grin
    val grin = Path().apply {
        moveTo(w * 0.36f, h * 0.68f)
        lineTo(w * 0.45f, h * 0.74f)
        lineTo(w * 0.50f, h * 0.69f)
        lineTo(w * 0.55f, h * 0.74f)
        lineTo(w * 0.64f, h * 0.68f)
    }
    drawPath(grin, color = c, style = stroke)
}

private fun DrawScope.drawArachnid(w: Float, h: Float, c: Color, stroke: Stroke) {
    // Body + abdomen
    drawCircle(c.copy(alpha = 0.22f), radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.58f))
    drawCircle(c, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.58f), style = stroke)
    drawCircle(c, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.32f), style = stroke)
    // 6 angular legs
    val leftLegs = listOf(0.38f to 0.20f, 0.50f to 0.46f, 0.64f to 0.78f)
    for ((yStart, yEnd) in leftLegs) {
        val lp = Path().apply {
            moveTo(w * 0.34f, h * yStart)
            lineTo(w * 0.18f, h * (yStart - 0.08f))
            lineTo(w * 0.08f, h * yEnd)
        }
        val rp = Path().apply {
            moveTo(w * 0.66f, h * yStart)
            lineTo(w * 0.82f, h * (yStart - 0.08f))
            lineTo(w * 0.92f, h * yEnd)
        }
        drawPath(lp, color = c, style = stroke)
        drawPath(rp, color = c, style = stroke)
    }
}

private fun DrawScope.drawDrone(w: Float, h: Float, c: Color, stroke: Stroke) {
    // Hex chassis
    val hex = Path().apply {
        moveTo(w * 0.50f, h * 0.18f)
        lineTo(w * 0.78f, h * 0.34f)
        lineTo(w * 0.78f, h * 0.66f)
        lineTo(w * 0.50f, h * 0.82f)
        lineTo(w * 0.22f, h * 0.66f)
        lineTo(w * 0.22f, h * 0.34f)
        close()
    }
    drawPath(hex, color = c.copy(alpha = 0.16f))
    drawPath(hex, color = c, style = stroke)
    // Side rotor bars
    drawLine(c, Offset(w * 0.06f, h * 0.30f), Offset(w * 0.24f, h * 0.30f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.76f, h * 0.30f), Offset(w * 0.94f, h * 0.30f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    // Central optical crimson lens
    drawCircle(DccColors.Crimson, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.50f))
    drawCircle(Color.White, radius = w * 0.04f, center = Offset(w * 0.47f, h * 0.47f))
}

private fun DrawScope.drawElemental(w: Float, h: Float, c: Color, stroke: Stroke) {
    val flame = Path().apply {
        moveTo(w * 0.50f, h * 0.08f)
        quadraticTo(w * 0.78f, h * 0.36f, w * 0.80f, h * 0.62f)
        quadraticTo(w * 0.80f, h * 0.90f, w * 0.50f, h * 0.92f)
        quadraticTo(w * 0.20f, h * 0.90f, w * 0.20f, h * 0.62f)
        quadraticTo(w * 0.22f, h * 0.42f, w * 0.38f, h * 0.30f)
        quadraticTo(w * 0.36f, h * 0.46f, w * 0.44f, h * 0.48f)
        quadraticTo(w * 0.40f, h * 0.24f, w * 0.50f, h * 0.08f)
        close()
    }
    drawPath(flame, color = c.copy(alpha = 0.22f))
    drawPath(flame, color = c, style = stroke)
    drawCircle(Color(0xFFFDE047), radius = w * 0.11f, center = Offset(w * 0.50f, h * 0.68f))
}

private fun DrawScope.drawUndead(w: Float, h: Float, c: Color, stroke: Stroke) {
    // Stylized skull
    val skull = Path().apply {
        moveTo(w * 0.22f, h * 0.56f)
        cubicTo(w * 0.16f, h * 0.22f, w * 0.32f, h * 0.10f, w * 0.50f, h * 0.10f)
        cubicTo(w * 0.68f, h * 0.10f, w * 0.84f, h * 0.22f, w * 0.78f, h * 0.56f)
        lineTo(w * 0.68f, h * 0.66f)
        lineTo(w * 0.66f, h * 0.86f)
        lineTo(w * 0.34f, h * 0.86f)
        lineTo(w * 0.32f, h * 0.66f)
        close()
    }
    drawPath(skull, color = c.copy(alpha = 0.16f))
    drawPath(skull, color = c, style = stroke)
    // Eye sockets
    drawCircle(c, radius = w * 0.09f, center = Offset(w * 0.37f, h * 0.44f))
    drawCircle(c, radius = w * 0.09f, center = Offset(w * 0.63f, h * 0.44f))
    // Teeth lines
    drawLine(c, Offset(w * 0.44f, h * 0.74f), Offset(w * 0.44f, h * 0.86f), strokeWidth = w * 0.06f)
    drawLine(c, Offset(w * 0.56f, h * 0.74f), Offset(w * 0.56f, h * 0.86f), strokeWidth = w * 0.06f)
}

private fun DrawScope.drawBeast(w: Float, h: Float, c: Color, stroke: Stroke) {
    // Triple feral claw slashes
    val slashWidth = w * 0.095f
    drawLine(c, Offset(w * 0.16f, h * 0.76f), Offset(w * 0.52f, h * 0.14f), strokeWidth = slashWidth, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.34f, h * 0.86f), Offset(w * 0.72f, h * 0.16f), strokeWidth = slashWidth, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.54f, h * 0.86f), Offset(w * 0.88f, h * 0.28f), strokeWidth = slashWidth, cap = StrokeCap.Round)
    // Subtle inner highlight
    drawCircle(c.copy(alpha = 0.35f), radius = w * 0.06f, center = Offset(w * 0.22f, h * 0.26f))
}

private fun DrawScope.drawSkyfowl(w: Float, h: Float, c: Color, stroke: Stroke) {
    val wings = Path().apply {
        moveTo(w * 0.50f, h * 0.64f)
        lineTo(w * 0.10f, h * 0.24f)
        lineTo(w * 0.18f, h * 0.58f)
        lineTo(w * 0.40f, h * 0.68f)
        lineTo(w * 0.50f, h * 0.84f)
        lineTo(w * 0.60f, h * 0.68f)
        lineTo(w * 0.82f, h * 0.58f)
        lineTo(w * 0.90f, h * 0.24f)
        close()
    }
    drawPath(wings, color = c.copy(alpha = 0.18f))
    drawPath(wings, color = c, style = stroke)
    drawCircle(c, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.34f))
}

private fun DrawScope.drawMage(w: Float, h: Float, c: Color, stroke: Stroke) {
    // Arcane 4-point star inside mystic ring
    drawCircle(c.copy(alpha = 0.22f), radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
    val star = Path().apply {
        moveTo(w * 0.50f, h * 0.10f)
        lineTo(w * 0.60f, h * 0.40f)
        lineTo(w * 0.90f, h * 0.50f)
        lineTo(w * 0.60f, h * 0.60f)
        lineTo(w * 0.50f, h * 0.90f)
        lineTo(w * 0.40f, h * 0.60f)
        lineTo(w * 0.10f, h * 0.50f)
        lineTo(w * 0.40f, h * 0.40f)
        close()
    }
    drawPath(star, color = c.copy(alpha = 0.24f))
    drawPath(star, color = c, style = stroke)
    drawCircle(Color.White, radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.50f))
}

private fun DrawScope.drawBoss(w: Float, h: Float, c: Color, stroke: Stroke) {
    // 3-peak Syndicate tactical crown + crimson core gem
    val crown = Path().apply {
        moveTo(w * 0.14f, h * 0.78f)
        lineTo(w * 0.10f, h * 0.30f)
        lineTo(w * 0.34f, h * 0.50f)
        lineTo(w * 0.50f, h * 0.18f)
        lineTo(w * 0.66f, h * 0.50f)
        lineTo(w * 0.90f, h * 0.30f)
        lineTo(w * 0.86f, h * 0.78f)
        close()
    }
    drawPath(crown, color = c.copy(alpha = 0.22f))
    drawPath(crown, color = c, style = stroke)
    drawCircle(DccColors.Crimson, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.60f))
}

private fun DrawScope.drawReticle(w: Float, h: Float, c: Color, stroke: Stroke) {
    val center = Offset(w * 0.50f, h * 0.50f)
    drawCircle(c.copy(alpha = 0.14f), radius = w * 0.34f, center = center)
    drawCircle(c, radius = w * 0.34f, center = center, style = stroke)
    drawCircle(c, radius = w * 0.08f, center = center)
    drawLine(c, Offset(w * 0.50f, h * 0.06f), Offset(w * 0.50f, h * 0.24f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.50f, h * 0.76f), Offset(w * 0.50f, h * 0.94f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.06f, h * 0.50f), Offset(w * 0.24f, h * 0.50f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
    drawLine(c, Offset(w * 0.76f, h * 0.50f), Offset(w * 0.94f, h * 0.50f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
}
