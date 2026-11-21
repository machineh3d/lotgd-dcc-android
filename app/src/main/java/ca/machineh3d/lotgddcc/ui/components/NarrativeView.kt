package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun NarrativeView(
    paragraphs: List<List<TextSpan>>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        for (spans in paragraphs) {
            val plain = spans.joinToString("") { it.text }.trim()
            if (plain.isEmpty()) continue

            val isSystemAiBanner = plain.contains("NEW ACHIEVEMENT", ignoreCase = true) ||
                plain.contains("SECRET ACHIEVEMENT", ignoreCase = true) ||
                plain.contains("LOOT BOX", ignoreCase = true)

            val annotated = buildAnnotatedString {
                for (span in spans) {
                    val parsedColor = parseHexColor(span.color) ?: DccColors.TextPrimary
                    val style = SpanStyle(
                        color = parsedColor,
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

            if (isSystemAiBanner) {
                Surface(
                    color = DccColors.Gold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, DccColors.Gold.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = annotated,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = annotated,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = DccColors.TextPrimary,
                )
            }
        }
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
