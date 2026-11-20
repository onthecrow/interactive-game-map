package com.onthecrow.interactivegamemap.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme

enum class ShadePosition {
    TOP, BOTTOM;
}

// The shade spans twice the system bar: the bar is covered and the rest fades out softly
private const val SHADE_HEIGHT_IN_BARS = 2

// A shadow over light content keeps the dark icons readable, over dark content it carries the light ones
private const val SHADE_ALPHA_LIGHT = 0.25f
private const val SHADE_ALPHA_DARK = 0.55f

private const val SHADE_STOPS = 8

/**
 * A dark shade under the status bar ([ShadePosition.TOP]) or the navigation bar ([ShadePosition.BOTTOM]),
 * so the system icons stay readable over the edge-to-edge content.
 */
@Composable
fun BoxScope.SystemBarShadeView(
    position: ShadePosition,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val insets = WindowInsets.systemBars
    val barHeight = when (position) {
        ShadePosition.TOP -> insets.getTop(density)
        ShadePosition.BOTTOM -> insets.getBottom(density)
    }
    if (barHeight == 0) return

    val alpha = if (GameMapTheme.colors.isDark) SHADE_ALPHA_DARK else SHADE_ALPHA_LIGHT
    val brush = remember(position, alpha) { shadeBrush(position, alpha) }

    Box(
        modifier = modifier
            .height(with(density) { (barHeight * SHADE_HEIGHT_IN_BARS).toDp() })
            .align(if (position == ShadePosition.TOP) Alignment.TopCenter else Alignment.BottomCenter)
            .background(brush),
    )
}

/** Eased, so the shade ends without a visible edge: full at the screen's edge, half at the bar's inner edge. */
private fun shadeBrush(position: ShadePosition, alpha: Float): Brush {
    val stops = (0..SHADE_STOPS).map { index ->
        val fromEdge = index.toFloat() / SHADE_STOPS
        val smooth = fromEdge * fromEdge * (3 - 2 * fromEdge)
        val offset = if (position == ShadePosition.TOP) fromEdge else 1 - fromEdge
        offset to Color.Black.copy(alpha = alpha * (1 - smooth))
    }.sortedBy { it.first }
    return Brush.verticalGradient(*stops.toTypedArray())
}
