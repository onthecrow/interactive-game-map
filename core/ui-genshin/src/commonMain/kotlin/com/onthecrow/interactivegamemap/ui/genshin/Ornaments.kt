package com.onthecrow.interactivegamemap.ui.genshin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The sparkle of the icon spans 22 of its 24 points. */
private const val SPARKLE_EXTENT = 11f / 12f

/** A four-pointed sparkle of [size] across, as GI/Icon/sparkle. */
private fun sparklePath(center: Offset, size: Float): Path {
    val k = size / 2 * SPARKLE_EXTENT / 11f
    fun x(v: Float) = center.x + v * k
    fun y(v: Float) = center.y + v * k
    return Path().apply {
        moveTo(x(0f), y(-11f))
        cubicTo(x(0.9f), y(-3.9f), x(3.9f), y(-0.9f), x(11f), y(0f))
        cubicTo(x(3.9f), y(0.9f), x(0.9f), y(3.9f), x(0f), y(11f))
        cubicTo(x(-0.9f), y(3.9f), x(-3.9f), y(0.9f), x(-11f), y(0f))
        cubicTo(x(-3.9f), y(-0.9f), x(-0.9f), y(-3.9f), x(0f), y(-11f))
        close()
    }
}

internal fun DrawScope.drawSparkle(center: Offset, size: Dp, color: Color, alpha: Float = 1f) {
    drawPath(sparklePath(center, size.toPx()), color, alpha)
}

internal fun DrawScope.drawDiamond(center: Offset, size: Dp, color: Color) {
    val r = size.toPx() / 2
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }
    drawPath(path, color)
}

/** The gold rule of the game dialogs: a sparkle and a diamond at both ends of a thin line. */
@Composable
internal fun OrnamentDivider(modifier: Modifier = Modifier) {
    val color = Genshin.colors.borderOrnament
    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val cy = size.height / 2
        val sparkle = 14.dp
        val diamond = 7.dp
        val gap = 3.dp.toPx()
        val diamondX = sparkle.toPx() + gap + diamond.toPx() / 2
        drawSparkle(Offset(sparkle.toPx() / 2, cy), sparkle, color)
        drawSparkle(Offset(size.width - sparkle.toPx() / 2, cy), sparkle, color)
        drawDiamond(Offset(diamondX, cy), diamond, color)
        drawDiamond(Offset(size.width - diamondX, cy), diamond, color)
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(diamondX + diamond.toPx() / 2 + gap, cy),
            end = Offset(size.width - diamondX - diamond.toPx() / 2 - gap, cy),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private val PanelShape = RoundedCornerShape(4.dp)

/**
 * A cream panel of the game: a thin inner gold frame and glowing gold corners, which stick out of the
 * panel, so nothing around may clip it.
 */
internal fun Modifier.genshinPanel(colors: GenshinColors): Modifier = this
    .shadow(elevation = 16.dp, shape = PanelShape)
    .background(colors.surfacePanel, PanelShape)
    .border(1.5.dp, colors.borderSubtle, PanelShape)
    .drawBehind {
        val inset = 6.dp.toPx()
        drawRoundRect(
            color = colors.borderOrnament.copy(alpha = 0.55f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - 2 * inset, size.height - 2 * inset),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
    .drawWithContent {
        drawContent()
        drawPanelCorners(colors.borderOrnament, colors.buttonGlyphConfirm)
    }

private fun DrawScope.drawPanelCorners(color: Color, glow: Color) {
    val edge = 13.dp.toPx()
    val width = 1.5.dp.toPx()
    val glowRadius = 13.dp.toPx()
    val corners = listOf(
        Offset(0f, 0f) to Offset(1f, 1f),
        Offset(size.width, 0f) to Offset(-1f, 1f),
        Offset(0f, size.height) to Offset(1f, -1f),
        Offset(size.width, size.height) to Offset(-1f, -1f),
    )
    corners.forEach { (corner, inward) ->
        drawLine(color, corner, Offset(corner.x + inward.x * edge, corner.y), width)
        drawLine(color, corner, Offset(corner.x, corner.y + inward.y * edge), width)
        drawCircle(
            brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center = corner, radius = glowRadius),
            radius = glowRadius,
            center = corner,
        )
        drawSparkle(corner, 16.dp, color)
    }
}

/**
 * The sparkles of a settings row: the big one at [center], where the darker end of the row begins, two
 * fainter ones to its left.
 */
internal fun DrawScope.drawStarCluster(center: Offset, colors: GenshinColors) {
    drawSparkle(Offset(center.x - 59.dp.toPx(), center.y), 30.dp, colors.ornamentStar, alpha = 0.7f)
    drawSparkle(Offset(center.x - 36.dp.toPx(), center.y), 44.dp, colors.ornamentStar, alpha = 0.95f)
    drawSparkle(center, 60.dp, colors.ornamentStarAccent, alpha = 0.6f)
}
