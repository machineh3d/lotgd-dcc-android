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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.DccPageResponse
import ca.machineh3d.lotgddcc.ui.theme.DccColors
import kotlinx.coroutines.delay

private data class PlayingCard(
    val rank: String,
    val suit: String,
    val isHidden: Boolean = false,
)

private data class BlackjackHandVisual(
    val dealerCards: List<PlayingCard>,
    val playerCards: List<PlayingCard>,
    val dealerTotal: String?,
    val playerTotal: String?,
)

private data class SlotReelVisual(
    val reels: List<String>,
    val isWin: Boolean,
)

private val CARD_SYMBOL_REGEX = Regex("""\b(10|[2-9AJQK])([♠♥♦♣SHDC])\b""")
private val CARD_WORD_REGEX = Regex("""\b(Ace|King|Queen|Jack|10|[2-9])\s+of\s+(Spades|Hearts|Diamonds|Clubs)\b""", RegexOption.IGNORE_CASE)
private val SLOT_BRACKET_REGEX = Regex("""\[\s*([^\[\]]{1,18})\s*\]\s*[-|]?\s*\[\s*([^\[\]]{1,18})\s*\]\s*[-|]?\s*\[\s*([^\[\]]{1,18})\s*\]""")
private val SLOT_PIPE_REGEX = Regex("""\|\s*([^|\n]{1,18})\s*\|\s*([^|\n]{1,18})\s*\|\s*([^|\n]{1,18})\s*\|""")

@Composable
fun CasinoVisualPanel(
    page: DccPageResponse,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val fullText = remember(page.paragraphs) {
        page.paragraphs.joinToString("\n") { line -> line.joinToString("") { it.text }.trim() }
    }
    val lowerUrl = page.currentUrl.lowercase()
    val lowerTitle = page.title.lowercase()
    val isCasinoContext = lowerUrl.contains("casino") ||
        lowerTitle.contains("desperado") ||
        lowerTitle.contains("blackjack") ||
        lowerTitle.contains("slot") ||
        fullText.contains("Blackjack", ignoreCase = true) ||
        fullText.contains("Slot", ignoreCase = true)

    if (!isCasinoContext) return

    val blackjack = remember(fullText) { parseBlackjackHands(fullText) }
    val slots = remember(fullText) { if (blackjack == null) parseSlotReels(fullText) else null }

    if (blackjack != null) {
        BlackjackTableCard(hand = blackjack, modifier = modifier)
    } else if (slots != null) {
        SlotMachineCard(
            visual = slots,
            spinKey = page.currentUrl + "|" + fullText.hashCode(),
            animate = animate,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlackjackTableCard(
    hand: BlackjackHandVisual,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF0E241C),
        border = BorderStroke(1.dp, DccColors.Emerald.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "DESPERADO CLUB · BLACKJACK TABLE",
                color = DccColors.Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            if (hand.dealerCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DEALER", color = DccColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (!hand.dealerTotal.isNullOrBlank()) {
                            Text(hand.dealerTotal, color = DccColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        hand.dealerCards.forEach { card -> PlayingCardView(card) }
                    }
                }
            }

            if (hand.playerCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("YOUR HAND", color = DccColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (!hand.playerTotal.isNullOrBlank()) {
                            Text(hand.playerTotal, color = DccColors.Emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        hand.playerCards.forEach { card -> PlayingCardView(card) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingCardView(card: PlayingCard) {
    if (card.isHidden) {
        Surface(
            color = Color(0xFF2B1319),
            border = BorderStroke(1.5.dp, DccColors.Crimson.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 3.dp,
            modifier = Modifier
                .width(50.dp)
                .height(70.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "?",
                    color = DccColors.Crimson,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        return
    }

    val isRed = card.suit == "♥" || card.suit == "♦"
    val ink = if (isRed) Color(0xFFDC2626) else Color(0xFF111827)

    Surface(
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .width(50.dp)
            .height(70.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            Text(
                text = card.rank,
                color = ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            Text(
                text = card.suit,
                color = ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = card.rank,
                color = ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun SlotMachineCard(
    visual: SlotReelVisual,
    spinKey: String,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val spinSymbols = remember { listOf("7", "BAR", "★", "♦", "💣", "👑", "🍀") }
    var reel0 by remember(spinKey) { mutableStateOf(visual.reels.getOrElse(0) { "?" }) }
    var reel1 by remember(spinKey) { mutableStateOf(visual.reels.getOrElse(1) { "?" }) }
    var reel2 by remember(spinKey) { mutableStateOf(visual.reels.getOrElse(2) { "?" }) }
    var finished by remember(spinKey) { mutableStateOf(!animate) }

    LaunchedEffect(spinKey, animate) {
        if (!animate) {
            reel0 = visual.reels.getOrElse(0) { "?" }
            reel1 = visual.reels.getOrElse(1) { "?" }
            reel2 = visual.reels.getOrElse(2) { "?" }
            finished = true
            return@LaunchedEffect
        }
        finished = false
        for (tick in 0 until 9) {
            if (tick < 4) reel0 = spinSymbols[(tick + 1) % spinSymbols.size]
            else reel0 = visual.reels.getOrElse(0) { "?" }

            if (tick < 6) reel1 = spinSymbols[(tick + 3) % spinSymbols.size]
            else reel1 = visual.reels.getOrElse(1) { "?" }

            if (tick < 8) reel2 = spinSymbols[(tick + 5) % spinSymbols.size]
            else reel2 = visual.reels.getOrElse(2) { "?" }

            delay(55L)
        }
        reel0 = visual.reels.getOrElse(0) { "?" }
        reel1 = visual.reels.getOrElse(1) { "?" }
        reel2 = visual.reels.getOrElse(2) { "?" }
        finished = true
    }

    val borderColor = if (finished && visual.isWin) DccColors.Gold else DccColors.Crimson.copy(alpha = 0.55f)

    Surface(
        color = DccColors.Surface,
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = if (finished && visual.isWin) "★ SYNDICATE SLOTS — PAYOUT! ★" else "DESPERADO CLUB · SYNDICATE SLOTS",
                color = if (finished && visual.isWin) DccColors.Gold else DccColors.Cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(reel0, reel1, reel2).forEach { sym ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DccColors.Background)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = sym,
                            color = if (finished && visual.isWin) DccColors.Gold else DccColors.TextPrimary,
                            fontSize = if (sym.length <= 3) 22.sp else 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

private fun parseBlackjackHands(text: String): BlackjackHandVisual? {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val dealerCards = mutableListOf<PlayingCard>()
    val playerCards = mutableListOf<PlayingCard>()
    var dealerTotal: String? = null
    var playerTotal: String? = null

    for (line in lines) {
        val lower = line.lowercase()
        val isDealerLine = lower.contains("dealer") || lower.contains("house")
        val isPlayerLine = lower.contains("your hand") || lower.contains("player") || lower.contains("you hold") || lower.contains("your cards")

        if (!isDealerLine && !isPlayerLine) continue

        val cards = extractCardsFromLine(line)
        if (isDealerLine && (lower.contains("[?]") || lower.contains("hidden") || lower.contains("face down"))) {
            if (cards.none { it.isHidden }) {
                cards.add(PlayingCard("?", "?", isHidden = true))
            }
        }

        val totalMatch = Regex("""(?:total|value|score|\()\s*:?\s*(\d{1,2})\s*\)?""", RegexOption.IGNORE_CASE).find(line)
        val totalStr = totalMatch?.groupValues?.getOrNull(1)

        if (isDealerLine && cards.isNotEmpty()) {
            dealerCards.addAll(cards)
            if (totalStr != null) dealerTotal = "Total: $totalStr"
        } else if (isPlayerLine && cards.isNotEmpty()) {
            playerCards.addAll(cards)
            if (totalStr != null) playerTotal = "Total: $totalStr"
        }
    }

    if (dealerCards.isEmpty() && playerCards.isEmpty()) return null
    return BlackjackHandVisual(dealerCards, playerCards, dealerTotal, playerTotal)
}

private fun extractCardsFromLine(line: String): MutableList<PlayingCard> {
    val result = mutableListOf<PlayingCard>()
    for (m in CARD_SYMBOL_REGEX.findAll(line)) {
        val rank = m.groupValues[1].uppercase()
        val suit = when (m.groupValues[2].uppercase()) {
            "♠", "S" -> "♠"
            "♥", "H" -> "♥"
            "♦", "D" -> "♦"
            else -> "♣"
        }
        result.add(PlayingCard(rank, suit))
    }
    if (result.isEmpty()) {
        for (m in CARD_WORD_REGEX.findAll(line)) {
            val rank = when (m.groupValues[1].lowercase()) {
                "ace" -> "A"
                "king" -> "K"
                "queen" -> "Q"
                "jack" -> "J"
                else -> m.groupValues[1]
            }
            val suit = when (m.groupValues[2].lowercase()) {
                "spades" -> "♠"
                "hearts" -> "♥"
                "diamonds" -> "♦"
                else -> "♣"
            }
            result.add(PlayingCard(rank, suit))
        }
    }
    return result
}

private fun parseSlotReels(text: String): SlotReelVisual? {
    val bracketMatch = SLOT_BRACKET_REGEX.find(text)
    val pipeMatch = if (bracketMatch == null) SLOT_PIPE_REGEX.find(text) else null
    val match = bracketMatch ?: pipeMatch ?: return null

    val r1 = match.groupValues[1].trim()
    val r2 = match.groupValues[2].trim()
    val r3 = match.groupValues[3].trim()
    if (r1.isBlank() || r2.isBlank() || r3.isBlank()) return null

    val isWin = text.contains("win", ignoreCase = true) ||
        text.contains("jackpot", ignoreCase = true) ||
        text.contains("payout", ignoreCase = true) ||
        (r1.equals(r2, ignoreCase = true) && r2.equals(r3, ignoreCase = true))

    return SlotReelVisual(listOf(r1, r2, r3), isWin)
}
