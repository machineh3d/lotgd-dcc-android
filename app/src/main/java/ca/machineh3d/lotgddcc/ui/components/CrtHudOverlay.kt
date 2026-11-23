package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ca.machineh3d.lotgddcc.ui.theme.DccColors

/**
 * Lightweight, non-interactive CRT scanline & Syndicate monitor vignette overlay.
 * Drawn purely with Canvas without intercepting touch input.
 */
@Composable
fun CrtHudOverlay(
    scanlinesEnabled: Boolean,
    vignetteEnabled: Boolean,
    neonEdgeGlowEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!scanlinesEnabled && !vignetteEnabled && !neonEdgeGlowEnabled) return

    val isLight = DccColors.isLight
    val cyanColor = DccColors.Cyan
    val crimsonColor = DccColors.Crimson

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Optional CRT horizontal scanlines
        if (scanlinesEnabled) {
            val stepPx = 3.dp.toPx().coerceAtLeast(3f)
            val strokePx = 1.dp.toPx().coerceAtLeast(1f)
            val lineColor = if (isLight) {
                Color.Black.copy(alpha = 0.045f)
            } else {
                Color.Black.copy(alpha = 0.16f)
            }
            var y = 0f
            while (y < h) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = strokePx,
                )
                y += stepPx
            }
        }

        // 2. Optional monitor edge vignette (subtle darkened corners)
        if (vignetteEnabled) {
            val radius = maxOf(w, h) * 0.78f
            val edgeAlpha = if (isLight) 0.10f else 0.34f
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = edgeAlpha),
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = radius,
                )
            )
        }

        // 3. Optional Syndicate broadcast neon edge glow (top cyan + bottom crimson hairline aura)
        if (neonEdgeGlowEnabled) {
            val topGlowHeight = 28.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        cyanColor.copy(alpha = if (isLight) 0.10f else 0.16f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = topGlowHeight,
                ),
                size = androidx.compose.ui.geometry.Size(w, topGlowHeight),
            )
            val bottomGlowHeight = 24.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        crimsonColor.copy(alpha = if (isLight) 0.08f else 0.14f),
                    ),
                    startY = h - bottomGlowHeight,
                    endY = h,
                ),
                topLeft = Offset(0f, h - bottomGlowHeight),
                size = androidx.compose.ui.geometry.Size(w, bottomGlowHeight),
            )
        }
    }
}
