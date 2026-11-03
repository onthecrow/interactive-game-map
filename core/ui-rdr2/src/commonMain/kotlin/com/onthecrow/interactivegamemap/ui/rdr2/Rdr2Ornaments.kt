package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The room the corner ornaments of the frame take outside its lines. */
internal val FrameOrnament = 8.dp

/** A leaf pair of the frame's corner, pointing out of the top left corner at the origin. */
private fun cornerLeaves(unit: Float): Path = Path().apply {
    fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
    // Along the top line
    moveTo(p(1.5f, 0f).x, p(1.5f, 0f).y)
    cubicTo(p(0.5f, -2.6f).x, p(0.5f, -2.6f).y, p(-3.2f, -4.2f).x, p(-3.2f, -4.2f).y, p(-5.6f, -3.1f).x, p(-5.6f, -3.1f).y)
    cubicTo(p(-3.6f, -2.6f).x, p(-3.6f, -2.6f).y, p(-1.6f, -1.2f).x, p(-1.6f, -1.2f).y, p(-0.4f, 0.4f).x, p(-0.4f, 0.4f).y)
    close()
    // Along the left line
    moveTo(p(0f, 1.5f).x, p(0f, 1.5f).y)
    cubicTo(p(-2.6f, 0.5f).x, p(-2.6f, 0.5f).y, p(-4.2f, -3.2f).x, p(-4.2f, -3.2f).y, p(-3.1f, -5.6f).x, p(-3.1f, -5.6f).y)
    cubicTo(p(-2.6f, -3.6f).x, p(-2.6f, -3.6f).y, p(-1.2f, -1.6f).x, p(-1.2f, -1.6f).y, p(0.4f, -0.4f).x, p(0.4f, -0.4f).y)
    close()
    // The bud in the corner
    addOval(Rect(p(-3.1f, -3.1f), Size(2.2f * unit, 2.2f * unit)))
}

/**
 * The frame of the game's titles, INDEX or SALOON: a heavy outer line and a light inner one with notched
 * corners, and a pair of leaves out of every corner. The lines run [FrameOrnament] inside the box.
 */
internal fun DrawScope.drawTitleFrame(color: Color) {
    val pad = FrameOrnament.toPx()
    val outerWidth = 2.4.dp.toPx()
    val innerWidth = 1.3.dp.toPx()
    val gap = 4.5.dp.toPx()
    val notch = 5.dp.toPx()
    val outer = Rect(pad, pad, size.width - pad, size.height - pad)
    drawRect(color, outer.topLeft, outer.size, style = Stroke(outerWidth))
    // The inner line, its corners cut in with quarter circles
    val i = outer.deflate(outerWidth / 2 + gap + innerWidth / 2)
    val inner = Path().apply {
        moveTo(i.left + notch, i.top)
        lineTo(i.right - notch, i.top)
        arcTo(Rect(Offset(i.right, i.top), notch), 180f, -90f, false)
        lineTo(i.right, i.bottom - notch)
        arcTo(Rect(Offset(i.right, i.bottom), notch), 270f, -90f, false)
        lineTo(i.left + notch, i.bottom)
        arcTo(Rect(Offset(i.left, i.bottom), notch), 0f, -90f, false)
        lineTo(i.left, i.top + notch)
        arcTo(Rect(Offset(i.left, i.top), notch), 90f, -90f, false)
        close()
    }
    drawPath(inner, color, style = Stroke(innerWidth))
    val leaves = cornerLeaves(1.4.dp.toPx())
    listOf(
        Offset(outer.left, outer.top) to Offset(1f, 1f),
        Offset(outer.right, outer.top) to Offset(-1f, 1f),
        Offset(outer.left, outer.bottom) to Offset(1f, -1f),
        Offset(outer.right, outer.bottom) to Offset(-1f, -1f),
    ).forEach { (corner, flip) ->
        translate(corner.x, corner.y) {
            scale(flip.x, flip.y, pivot = Offset.Zero) {
                drawPath(leaves, color)
            }
        }
    }
}

/**
 * A title of a screen or a dialog in the frame of the game, in capitals on one line: a long one, as some languages
 * have, shrinks to fit rather than breaks a word.
 */
@Composable
internal fun TitleFrame(text: String, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    val colors = Rdr2.colors
    val style = Rdr2.type.display
    Box(
        modifier = modifier
            .heightIn(min = height)
            .drawBehind { drawTitleFrame(colors.frame) }
            .padding(horizontal = FrameOrnament + 14.dp, vertical = FrameOrnament + 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.uppercase(),
            style = style.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize, stepSize = 1.sp),
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** The thin rule of the game's panels, with the small notch under its middle. */
@Composable
internal fun NotchedRule(modifier: Modifier = Modifier, notch: Boolean = true) {
    val color = Rdr2.colors.rule
    Canvas(modifier = modifier.fillMaxWidth().height(6.dp)) {
        val y = 1.dp.toPx()
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2.dp.toPx())
        if (notch) {
            val half = 4.dp.toPx()
            val cx = size.width / 2
            drawPath(
                Path().apply {
                    moveTo(cx - half, y)
                    lineTo(cx + half, y)
                    lineTo(cx, y + half)
                    close()
                },
                color,
            )
        }
    }
}

/** The box of the game's checklist, with the painted red tick when [checked]. */
@Composable
internal fun Checkbox(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = Rdr2.colors
    Box(modifier = modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(colors.checkboxFill)
                .border(2.dp, colors.checkboxBorder),
        )
        if (checked) {
            // The tick is bigger than the box and runs out of its top right corner, as in the game
            Icon(
                imageVector = Rdr2Icons.PaintedCheck,
                contentDescription = null,
                tint = colors.selection,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** The kinds of the round glyphs of the game's prompts, WAYPOINT ⓧ or BACK ◯. */
internal enum class PromptGlyph { Confirm, Cancel, Back }

/** The glyph of a prompt on its worn white disc. */
@Composable
internal fun PromptGlyphView(glyph: PromptGlyph, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val colors = Rdr2.colors
    PromptDisc(size = size, modifier = modifier, halo = false, salt = glyph.ordinal) {
        val icon: ImageVector? = when (glyph) {
            PromptGlyph.Confirm -> Rdr2Icons.Check
            PromptGlyph.Cancel -> Rdr2Icons.Cross
            PromptGlyph.Back -> null
        }
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.promptGlyph, modifier = Modifier.size(size * 0.7f))
        } else {
            Box(modifier = Modifier.size(size * 0.56f).border(size * 0.11f, colors.promptGlyph, CircleShape))
        }
    }
}

/**
 * The white disc of the game's prompts, [size] across: an edge worn out of round, the glyph of [content] worn by
 * specks and scratches in the color of the disc, the paper stained around it. With [halo] a dark shadow spreads
 * around it, past its box; [ring] is the grey ring of an active one, [pressed] darkens the disc.
 */
@Composable
internal fun PromptDisc(
    size: Dp,
    modifier: Modifier = Modifier,
    halo: Boolean = true,
    ring: Boolean = false,
    pressed: Boolean = false,
    salt: Int = 0,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Rdr2.colors
    val disc = if (pressed) colors.promptDiscPressed else colors.promptDisc
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val d = this.size.minDimension / density.toDouble()
                val seed = brushSeed(d, d, salt + 101)
                val edge = brushDisc(d, seed, density)
                val grunge = brushGrunge(d, seed, density)
                val radius = this.size.minDimension / 2
                val ringWidth = radius * 0.28f
                onDrawWithContent {
                    if (halo) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to colors.promptHalo.copy(alpha = 0.3f),
                                0.74f to colors.promptHalo.copy(alpha = 0.22f),
                                1f to Color.Transparent,
                                center = center,
                                radius = radius * 1.3f,
                            ),
                            radius = radius * 1.3f,
                        )
                    }
                    if (ring) {
                        drawCircle(colors.promptRing, radius = radius + ringWidth / 2, style = Stroke(ringWidth))
                    }
                    drawPath(edge, disc)
                    grunge.mottle.forEach { drawCircle(colors.promptMottle, it.radius, it.center, alpha = it.alpha) }
                    drawContent()
                    grunge.wear.forEach { drawCircle(disc, it.radius, it.center, alpha = it.alpha) }
                    grunge.scratches.forEach {
                        drawLine(disc, it.start, it.end, strokeWidth = it.width, cap = StrokeCap.Round, alpha = it.alpha)
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** The faint grain of the paper or of the night under the panels, darker towards the edges. */
internal fun DrawScope.drawScreenGrain(grain: Color) {
    val r = BrushRandomFloats(seed = 1873)
    val count = (size.width * size.height / 9000f).toInt().coerceAtMost(600)
    repeat(count) {
        val x = r.next() * size.width
        val y = r.next() * size.height
        val len = (4 + r.next() * 28).dp.toPx()
        drawLine(grain.copy(alpha = 0.035f + r.next() * 0.05f), Offset(x, y), Offset(x + len, y + (r.next() - 0.5f)), strokeWidth = (0.6f + r.next()).dp.toPx())
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, grain.copy(alpha = 0.28f)),
            center = center,
            radius = size.maxDimension * 0.75f,
        ),
    )
}

/** Floats in 0..1 for the ornaments, the same on every draw. */
private class BrushRandomFloats(seed: Int) {
    private var state = seed

    fun next(): Float {
        state = state * 1103515245 + 12345
        return ((state ushr 8) and 0xFFFF) / 65536f
    }
}
