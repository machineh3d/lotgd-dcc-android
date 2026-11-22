package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.GameTable
import ca.machineh3d.lotgddcc.ui.theme.DccColors

private val HEADER_KEYWORDS = listOf(
    "level", "cost", "upgrade", "function", "damage", "defense",
    "station", "item", "weapon", "armor", "price", "effect", "status", "name",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativeTableCards(
    tables: List<GameTable>,
    enabled: Boolean,
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        for (table in tables) {
            // Promote <td>-only header rows (e.g. Personal Space "Station / Upgrade | Level | Function | Upgrade Cost")
            val firstRow = table.rows.firstOrNull()
            val promoteFirstRow = table.headers.isEmpty() &&
                firstRow != null &&
                firstRow.actionUrl.isNullOrBlank() &&
                firstRow.cells.size >= 2 &&
                firstRow.cells.count { cell ->
                    val lc = cell.trim().lowercase()
                    HEADER_KEYWORDS.any { lc.contains(it) }
                } >= 2

            val headers = if (promoteFirstRow && firstRow != null) firstRow.cells else table.headers
            val dataRows = if (promoteFirstRow) table.rows.drop(1) else table.rows

            val isVendorTable = headers.any {
                it.equals("Cost", ignoreCase = true) ||
                    it.equals("Damage", ignoreCase = true) ||
                    it.equals("Defense", ignoreCase = true)
            }

            for (row in dataRows) {
                if (row.cells.isEmpty()) continue

                val primaryTitle = row.cells.firstOrNull().orEmpty().trim()
                val detailCells = row.cells.drop(1)
                val actionUrl = row.actionUrl
                val actionLabel = row.actionLabel?.trim().orEmpty()
                val canTap = enabled && !actionUrl.isNullOrBlank()

                // Split short stat/cost pills vs long descriptive prose so descriptions stay readable
                val shortChips = mutableListOf<Triple<String, String, Color>>()
                val descriptions = mutableListOf<String>()

                detailCells.forEachIndexed { idx, rawVal ->
                    var cleaned = rawVal.trim()
                    if (actionLabel.isNotBlank() && cleaned.startsWith(actionLabel, ignoreCase = true)) {
                        cleaned = cleaned.removePrefix(actionLabel).trim()
                    }
                    // Also strip any glued "[Install Lv 1]..." bracket action prefix
                    cleaned = cleaned.replace(Regex("""^\[[^\]]{2,24}\]\s*"""), "").trim()
                    if (cleaned.isBlank()) return@forEachIndexed

                    val hdr = headers.getOrNull(idx + 1).orEmpty().trim()
                    if (cleaned.length > 44 || hdr.contains("function", ignoreCase = true) || hdr.contains("desc", ignoreCase = true)) {
                        descriptions.add(cleaned)
                    } else {
                        val pillColor = when {
                            hdr.contains("cost", ignoreCase = true) ||
                                hdr.contains("gold", ignoreCase = true) ||
                                cleaned.contains("G", ignoreCase = false) && cleaned.any { it.isDigit() } -> DccColors.Gold
                            cleaned.contains("token", ignoreCase = true) ||
                                hdr.contains("gem", ignoreCase = true) -> DccColors.Violet
                            hdr.contains("damage", ignoreCase = true) ||
                                hdr.contains("atk", ignoreCase = true) -> DccColors.Crimson
                            hdr.contains("defense", ignoreCase = true) ||
                                hdr.contains("def", ignoreCase = true) -> DccColors.Emerald
                            else -> DccColors.Cyan
                        }
                        shortChips.add(Triple(hdr, cleaned, pillColor))
                    }
                }

                Surface(
                    color = DccColors.Surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (canTap) DccColors.Cyan.copy(alpha = 0.45f) else DccColors.Outline
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (canTap && actionUrl != null) {
                                Modifier.clickable { onActionClick(actionUrl) }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = primaryTitle,
                                    color = if (canTap) DccColors.TextPrimary else DccColors.TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp,
                                )

                                if (shortChips.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        for ((hdr, value, accent) in shortChips) {
                                            TableStatChip(
                                                label = hdr,
                                                value = value,
                                                accent = accent,
                                            )
                                        }
                                    }
                                }
                            }

                            if (!actionUrl.isNullOrBlank()) {
                                Spacer(Modifier.width(10.dp))
                                val btnText = when {
                                    isVendorTable -> "Buy"
                                    actionLabel.isNotBlank() && actionLabel != primaryTitle ->
                                        actionLabel.removePrefix("[").removeSuffix("]")
                                    else -> "Open"
                                }
                                Button(
                                    enabled = enabled,
                                    onClick = { onActionClick(actionUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(btnText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (descriptions.isNotEmpty()) {
                            for (desc in descriptions) {
                                Text(
                                    text = desc,
                                    color = DccColors.TextSecondary,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableStatChip(
    label: String,
    value: String,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DccColors.SurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = "$label: ",
                color = DccColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = value,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
