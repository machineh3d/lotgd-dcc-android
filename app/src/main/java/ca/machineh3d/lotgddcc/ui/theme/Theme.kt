package ca.machineh3d.lotgddcc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class DccColorPalette(
    val isLight: Boolean,
    val Background: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val Outline: Color,
    val Crimson: Color,
    val Gold: Color,
    val Cyan: Color,
    val Emerald: Color,
    val Violet: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
)

val SyndicateDarkPalette = DccColorPalette(
    isLight = false,
    Background = Color(0xFF0B0E14),
    Surface = Color(0xFF141923),
    SurfaceVariant = Color(0xFF1E2534),
    Outline = Color(0xFF2D3748),
    Crimson = Color(0xFFFF4D4D),
    Gold = Color(0xFFF59E0B),
    Cyan = Color(0xFF22D3EE),
    Emerald = Color(0xFF4ADE80),
    Violet = Color(0xFFC084FC),
    TextPrimary = Color(0xFFF1F5F9),
    TextSecondary = Color(0xFF94A3B8),
)

val SyndicateLightPalette = DccColorPalette(
    isLight = true,
    Background = Color(0xFFF3F6FA),
    Surface = Color(0xFFFFFFFF),
    SurfaceVariant = Color(0xFFE7EDF6),
    Outline = Color(0xFFCBD5E1),
    Crimson = Color(0xFFDC2626),
    Gold = Color(0xFFB45309),
    Cyan = Color(0xFF0891B2),
    Emerald = Color(0xFF15803D),
    Violet = Color(0xFF7C3AED),
    TextPrimary = Color(0xFF0F172A),
    TextSecondary = Color(0xFF475569),
)

object DccColors {
    var currentPalette by mutableStateOf(SyndicateDarkPalette)

    val isLight: Boolean get() = currentPalette.isLight
    val Background: Color get() = currentPalette.Background
    val Surface: Color get() = currentPalette.Surface
    val SurfaceVariant: Color get() = currentPalette.SurfaceVariant
    val Outline: Color get() = currentPalette.Outline
    val Crimson: Color get() = currentPalette.Crimson
    val Gold: Color get() = currentPalette.Gold
    val Cyan: Color get() = currentPalette.Cyan
    val Emerald: Color get() = currentPalette.Emerald
    val Violet: Color get() = currentPalette.Violet
    val TextPrimary: Color get() = currentPalette.TextPrimary
    val TextSecondary: Color get() = currentPalette.TextSecondary
}

private val SyndicateDarkColorScheme = darkColorScheme(
    primary = SyndicateDarkPalette.Crimson,
    onPrimary = Color.White,
    secondary = SyndicateDarkPalette.Cyan,
    onSecondary = Color(0xFF0B0E14),
    tertiary = SyndicateDarkPalette.Gold,
    onTertiary = Color(0xFF0B0E14),
    background = SyndicateDarkPalette.Background,
    onBackground = SyndicateDarkPalette.TextPrimary,
    surface = SyndicateDarkPalette.Surface,
    onSurface = SyndicateDarkPalette.TextPrimary,
    surfaceVariant = SyndicateDarkPalette.SurfaceVariant,
    onSurfaceVariant = SyndicateDarkPalette.TextSecondary,
    outline = SyndicateDarkPalette.Outline,
    error = Color(0xFFF87171),
)

private val SyndicateLightColorScheme = lightColorScheme(
    primary = SyndicateLightPalette.Crimson,
    onPrimary = Color.White,
    secondary = SyndicateLightPalette.Cyan,
    onSecondary = Color.White,
    tertiary = SyndicateLightPalette.Gold,
    onTertiary = Color.White,
    background = SyndicateLightPalette.Background,
    onBackground = SyndicateLightPalette.TextPrimary,
    surface = SyndicateLightPalette.Surface,
    onSurface = SyndicateLightPalette.TextPrimary,
    surfaceVariant = SyndicateLightPalette.SurfaceVariant,
    onSurfaceVariant = SyndicateLightPalette.TextSecondary,
    outline = SyndicateLightPalette.Outline,
    error = Color(0xFFDC2626),
)

@Composable
fun LotgdDccTheme(
    useLightTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    DccColors.currentPalette = if (useLightTheme) SyndicateLightPalette else SyndicateDarkPalette
    MaterialTheme(
        colorScheme = if (useLightTheme) SyndicateLightColorScheme else SyndicateDarkColorScheme,
        content = content
    )
}
