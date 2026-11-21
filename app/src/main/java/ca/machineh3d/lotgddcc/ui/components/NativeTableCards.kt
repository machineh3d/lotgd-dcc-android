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
            val headers = table.headers
            val isVendorTable = headers.any {
                it.equals("Cost", ignoreCase = true) ||
                    it.equals("Damage", ignoreCase = true) ||
                    it.equals("Defense", ignoreCase = true)
            }

            for (row in table.rows) {
                if (row.cells.isEmpty()) continue

                val primaryTitle = row.cells.firstOrNull().orEmpty()
                val detailCells = row.cells.drop(1)
                val actionUrl = row.actionUrl
                val canTap = enabled && !actionUrl.isNullOrBlank()

                Surface(
                    color = DccColors.Surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (canTap) DccColors.Cyan.copy(alpha = 0.5f) else DccColors.Outline
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = primaryTitle,
                                color = if (canTap) DccColors.TextPrimary else DccColors.TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp,
                            )

                            if (detailCells.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    detailCells.forEachIndexed { idx, value ->
                                        if (value.isBlank()) return@forEachIndexed
                                        val hdr = headers.getOrNull(idx + 1).orEmpty()
                                        val pillColor = when {
                                            hdr.contains("cost", ignoreCase = true) ||
                                                hdr.contains("gold", ignoreCase = true) -> DccColors.Gold
                                            hdr.contains("damage", ignoreCase = true) ||
                                                hdr.contains("atk", ignoreCase = true) -> DccColors.Crimson
                                            hdr.contains("defense", ignoreCase = true) ||
                                                hdr.contains("def", ignoreCase = true) -> DccColors.Emerald
                                            else -> DccColors.Cyan
                                        }
                                        TableStatChip(
                                            label = hdr,
                                            value = value,
                                            accent = pillColor
                                        )
                                    }
                                }
                            }
                        }

                        if (!actionUrl.isNullOrBlank()) {
                            Spacer(Modifier.width(10.dp))
                            val btnText = when {
                                isVendorTable -> "Buy"
                                !row.actionLabel.isNullOrBlank() && row.actionLabel != primaryTitle -> row.actionLabel
                                else -> "Open"
                            }
                            Button(
                                enabled = enabled,
                                onClick = { onActionClick(actionUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(btnText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            .padding(horizontal = 8.dp, vertical = 3.dp)
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
