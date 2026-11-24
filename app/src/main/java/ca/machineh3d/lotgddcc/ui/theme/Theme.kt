package ca.machineh3d.lotgddcc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import ca.machineh3d.lotgddcc.data.AppThemeMode

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

// Default: Borant Syndicate Dark
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

// Daylight Syndicate Light
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

// Void Black AMOLED (true #000000 OLED canvas)
val VoidAmoledPalette = DccColorPalette(
    isLight = false,
    Background = Color(0xFF000000),
    Surface = Color(0xFF0A0D12),
    SurfaceVariant = Color(0xFF141923),
    Outline = Color(0xFF263042),
    Crimson = Color(0xFFFF4757),
    Gold = Color(0xFFFBBF24),
    Cyan = Color(0xFF00E5FF),
    Emerald = Color(0xFF34D399),
    Violet = Color(0xFFC084FC),
    TextPrimary = Color(0xFFF8FAFC),
    TextSecondary = Color(0xFF94A3B8),
)

// Borant Terminal (classic phosphor green CRT)
val BorantTerminalPalette = DccColorPalette(
    isLight = false,
    Background = Color(0xFF050E08),
    Surface = Color(0xFF0B1A10),
    SurfaceVariant = Color(0xFF12281A),
    Outline = Color(0xFF1F472E),
    Crimson = Color(0xFFFF5C5C),
    Gold = Color(0xFFFFB020),
    Cyan = Color(0xFF39FF88),
    Emerald = Color(0xFF69F0AE),
    Violet = Color(0xFFA3E635),
    TextPrimary = Color(0xFFD9FFE8),
    TextSecondary = Color(0xFF7BB894),
)

// Desperado Club (neon synthwave plum & magenta)
val DesperadoClubPalette = DccColorPalette(
    isLight = false,
    Background = Color(0xFF10081B),
    Surface = Color(0xFF1A0F2B),
    SurfaceVariant = Color(0xFF27173F),
    Outline = Color(0xFF442A6B),
    Crimson = Color(0xFFF43F8E),
    Gold = Color(0xFFFBBF24),
    Cyan = Color(0xFF38BDF8),
    Emerald = Color(0xFF34D399),
    Violet = Color(0xFFE879F9),
    TextPrimary = Color(0xFFF5F3FF),
    TextSecondary = Color(0xFFB39DDB),
)

// Iron Tangle (warm forged rust, amber & patina teal)
val IronTanglePalette = DccColorPalette(
    isLight = false,
    Background = Color(0xFF120E0B),
    Surface = Color(0xFF1D1712),
    SurfaceVariant = Color(0xFF2A211A),
    Outline = Color(0xFF47372B),
    Crimson = Color(0xFFF97316),
    Gold = Color(0xFFFACC15),
    Cyan = Color(0xFF2DD4BF),
    Emerald = Color(0xFF84CC16),
    Violet = Color(0xFFFB7185),
    TextPrimary = Color(0xFFF5EFE6),
    TextSecondary = Color(0xFFB5A493),
)

fun paletteFor(mode: AppThemeMode, systemDark: Boolean): DccColorPalette = when (mode) {
    AppThemeMode.DARK -> SyndicateDarkPalette
    AppThemeMode.LIGHT -> SyndicateLightPalette
    AppThemeMode.AMOLED -> VoidAmoledPalette
    AppThemeMode.TERMINAL -> BorantTerminalPalette
    AppThemeMode.DESPERADO -> DesperadoClubPalette
    AppThemeMode.IRON_TANGLE -> IronTanglePalette
    AppThemeMode.SYSTEM -> if (systemDark) SyndicateDarkPalette else SyndicateLightPalette
}

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

private fun materialColorSchemeFor(palette: DccColorPalette): ColorScheme = if (palette.isLight) {
    lightColorScheme(
        primary = palette.Crimson,
        onPrimary = Color.White,
        secondary = palette.Cyan,
        onSecondary = Color.White,
        tertiary = palette.Gold,
        onTertiary = Color.White,
        background = palette.Background,
        onBackground = palette.TextPrimary,
        surface = palette.Surface,
        onSurface = palette.TextPrimary,
        surfaceVariant = palette.SurfaceVariant,
        onSurfaceVariant = palette.TextSecondary,
        outline = palette.Outline,
        error = palette.Crimson,
    )
} else {
    darkColorScheme(
        primary = palette.Crimson,
        onPrimary = Color.White,
        secondary = palette.Cyan,
        onSecondary = palette.Background,
        tertiary = palette.Gold,
        onTertiary = palette.Background,
        background = palette.Background,
        onBackground = palette.TextPrimary,
        surface = palette.Surface,
        onSurface = palette.TextPrimary,
        surfaceVariant = palette.SurfaceVariant,
        onSurfaceVariant = palette.TextSecondary,
        outline = palette.Outline,
        error = Color(0xFFF87171),
    )
}

@Composable
fun LotgdDccTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(themeMode, systemDark)
    DccColors.currentPalette = palette
    MaterialTheme(
        colorScheme = materialColorSchemeFor(palette),
        content = content
    )
}
