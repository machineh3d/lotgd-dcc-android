package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.TextSpan
import ca.machineh3d.lotgddcc.ui.theme.DccColors

private val COMPACT_COMBAT_SKIP_REGEX = Regex(
    """(?:'s\s+Hitpoints\s*(?:\(Level\s*\d+\))?\s*:|End of Round:|~ ~ ~ Fight ~ ~ ~)""",
    RegexOption.IGNORE_CASE,
)

// Strips stray web theme footer lines (e.g. "• Dungeon Crawler World: Earth", ".", "About & Credits")
// that otherwise float awkwardly above inline choice cards and room forms.
private val WEB_FOOTER_CHROME_REGEX = Regex(
    """^(?:[•●·\-\s]*Dungeon Crawler World:\s*Earth(?:\s*[·•|]\s*About\s*(?:&|and)\s*Credits)?|[•●·\-\s]*About\s*(?:&|and)\s*Credits|[.·•|])$""",
    RegexOption.IGNORE_CASE,
)

// Matches jammed room resource lines like:
// "Gold Credits: 1,058Sponsor Tokens: 4Daily Workbench Crafts Left: 2 / 2"
private val STAT_PAIR_REGEX = Regex(
    """([A-Za-z][A-Za-z0-9 '&/\-]{2,34}):\s*([0-9][0-9,\./\s]*)(?=[A-Z]|$)"""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NarrativeView(
    paragraphs: List<List<TextSpan>>,
    onLinkClick: (String) -> Unit,
    fontSizeSp: Int = 15,
    compactCombatLog: Boolean = false,
    isCombatPage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isLight = DccColors.isLight
    val lineHeightSp = (fontSizeSp * 1.45f).toInt()

    Column(
        verticalArrangement = Arrangement.spacedBy(if (compactCombatLog && isCombatPage) 6.dp else 10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        for (spans in paragraphs) {
            val plain = spans.joinToString("") { it.text }.trim()
            if (plain.isEmpty()) continue
            if (WEB_FOOTER_CHROME_REGEX.matches(plain)) continue

            if (compactCombatLog && isCombatPage && COMPACT_COMBAT_SKIP_REGEX.containsMatchIn(plain)) {
                continue
            }

            // Detect jammed room resource/counter lines and render as clean Syndicate stat pills
            val statPairs = extractRoomResourcePills(plain, spans)
            if (statPairs != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for ((label, value) in statPairs) {
                        RoomResourcePill(label = label, value = value)
                    }
                }
                continue
            }

            val isLootBox = plain.contains("LOOT BOX", ignoreCase = true)
            val isAchievement = plain.contains("NEW ACHIEVEMENT", ignoreCase = true) ||
                plain.contains("SECRET ACHIEVEMENT", ignoreCase = true)
            val isMordecaiGuide = plain.contains("GAME GUIDE MORDECAI", ignoreCase = true) ||
                plain.contains("MORDECAI'S ROOKIE", ignoreCase = true) ||
                plain.contains("MORDECAI TIP", ignoreCase = true)

            val annotated = buildAnnotatedString {
                for (span in spans) {
                    val rawColor = parseHexColor(span.color)
                    val adaptedColor = when {
                        rawColor == null -> DccColors.TextPrimary
                        isLight -> adaptColorForLightTheme(rawColor)
                        else -> rawColor
                    }
                    val style = SpanStyle(
                        color = adaptedColor,
                        fontWeight = if (span.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (span.italic) FontStyle.Italic else FontStyle.Normal,
                    )

                    val href = span.link
                    if (!href.isNullOrBlank()) {
                        val linkStyle = SpanStyle(
                            color = DccColors.Cyan,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                        )
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = href,
                                styles = TextLinkStyles(style = linkStyle),
                                linkInteractionListener = { onLinkClick(href) }
                            )
                        ) {
                            append(span.text)
                        }
                    } else {
                        withStyle(style) {
                            append(span.text)
                        }
                    }
                }
            }

            if (isAchievement || isLootBox || isMordecaiGuide) {
                val accent = when {
                    isLootBox -> DccColors.Cyan
                    isAchievement -> DccColors.Gold
                    else -> DccColors.Emerald
                }
                val badgeTitle = when {
                    isLootBox -> "SPONSOR LOOT BOX"
                    isAchievement -> "SYSTEM AI ACHIEVEMENT"
                    else -> "GAME GUIDE MORDECAI"
                }
                val icon = when {
                    isLootBox -> Icons.Filled.CardGiftcard
                    isAchievement -> Icons.Filled.EmojiEvents
                    else -> Icons.Filled.AutoStories
                }

                Surface(
                    color = accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(accent.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = badgeTitle,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badgeTitle,
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = annotated,
                                fontSize = fontSizeSp.sp,
                                lineHeight = lineHeightSp.sp,
                                color = DccColors.TextPrimary,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = annotated,
                    fontSize = fontSizeSp.sp,
                    lineHeight = lineHeightSp.sp,
                    color = DccColors.TextPrimary,
                )
            }
        }
    }
}

private fun extractRoomResourcePills(
    plain: String,
    spans: List<TextSpan>,
): List<Pair<String, String>>? {
    // Only split short non-link status bars containing 2+ "Label: Number" pairs
    if (plain.length > 160 || spans.any { !it.link.isNullOrBlank() }) return null
    val matches = STAT_PAIR_REGEX.findAll(plain).toList()
    if (matches.size < 2) return null

    // Confirm most of the string is covered by these stat pairs
    val matchedLen = matches.sumOf { it.value.length }
    if (matchedLen < plain.length * 0.72) return null

    return matches.map { m ->
        m.groupValues[1].trim() to m.groupValues[2].trim()
    }
}

@Composable
private fun RoomResourcePill(
    label: String,
    value: String,
) {
    val lc = label.lowercase()
    val accent = when {
        lc.contains("gold") || lc.contains("credit") -> DccColors.Gold
        lc.contains("token") || lc.contains("gem") -> DccColors.Violet
        lc.contains("hp") || lc.contains("health") -> DccColors.Emerald
        else -> DccColors.Cyan
    }

    Surface(
        color = DccColors.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = DccColors.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            )
            Text(
                text = value,
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private fun adaptColorForLightTheme(color: Color): Color {
    val lum = color.luminance()
    return if (lum > 0.42f) {
        val factor = if (lum > 0.78f) 0.42f else 0.58f
        Color(
            red = (color.red * factor).coerceIn(0f, 1f),
            green = (color.green * factor).coerceIn(0f, 1f),
            blue = (color.blue * factor).coerceIn(0f, 1f),
            alpha = color.alpha,
        )
    } else {
        color
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val cleaned = hex.removePrefix("#")
    return runCatching {
        val colorInt = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or colorInt)
            8 -> Color(colorInt)
            else -> null
        }
    }.getOrNull()
}
