package ca.machineh3d.lotgddcc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DccColors {
    val Background = Color(0xFF0B0E14)
    val Surface = Color(0xFF141923)
    val SurfaceVariant = Color(0xFF1E2534)
    val Outline = Color(0xFF2D3748)

    val Crimson = Color(0xFFFF4D4D)
    val Gold = Color(0xFFF59E0B)
    val Cyan = Color(0xFF22D3EE)
    val Emerald = Color(0xFF4ADE80)
    val Violet = Color(0xFFC084FC)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
}

private val SyndicateDarkColorScheme = darkColorScheme(
    primary = DccColors.Crimson,
    onPrimary = Color.White,
    secondary = DccColors.Cyan,
    onSecondary = Color(0xFF0B0E14),
    tertiary = DccColors.Gold,
    onTertiary = Color(0xFF0B0E14),
    background = DccColors.Background,
    onBackground = DccColors.TextPrimary,
    surface = DccColors.Surface,
    onSurface = DccColors.TextPrimary,
    surfaceVariant = DccColors.SurfaceVariant,
    onSurfaceVariant = DccColors.TextSecondary,
    outline = DccColors.Outline,
    error = Color(0xFFF87171),
)

@Composable
fun LotgdDccTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SyndicateDarkColorScheme,
        content = content
    )
}
