package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// The route stays visible zoomed far out and doesn't cover the map zoomed in
private val MinRouteWidth = 4.dp
private val MaxRouteWidth = 24.dp

// The legs across country and the jumps to fast travel points are crayon dashes half as wide as the route along the
// roads, as on the game's map; their lengths are in widths of the road
private const val OFF_ROAD_WIDTH = 0.5f
private const val OFF_ROAD_DASH = 2.2f
private const val OFF_ROAD_GAP = 1.4f

/** The width of the route along the roads at [zoom]: twice as wide a zoom level in, like the roads on the tiles. */
internal fun MapStyle.routeLineWidth(zoom: Float): Dp {
    val width = routeWidth.value * 2f.pow(zoom - routeWidthZoom)
    return width.coerceIn(MinRouteWidth.value, MaxRouteWidth.value).dp
}

/**
 * The width of the dashes of the legs across country and of the jumps to fast travel points at [zoom]: always the same
 * part of the road's, so the dashes drawn in their texture are as many widths of the road long at any zoom.
 */
internal fun MapStyle.offRoadLineWidth(zoom: Float): Dp = (routeLineWidth(zoom).value * OFF_ROAD_WIDTH).dp

// The textures are this wide; the map stamps them along a line as wide as it, each as long as the texture is tall for
// that width, one after another
private const val TEXTURE_SIZE = 64

/**
 * A stroke of a red crayon on paper, stamped along the route along the roads by the map: its edges are ragged, its
 * colour uneven in blots and grain, with specks of paper showing through. The top of the image is toward the start of
 * the line and the bottom of one stamp meets the top of the next, so every noise here repeats over the height of the
 * image.
 */
@Composable
internal fun rememberCrayonTexture(color: Color): ImageBitmap = remember(color) {
    drawCrayon(color, height = TEXTURE_SIZE, stroke = TEXTURE_SIZE)
}

/**
 * A dash of the same crayon and the gap after it, stamped along a leg across country or a jump to a fast travel point:
 * one after another, they are its dashes. Their lengths grow and shrink with the width of the line, as the map does.
 *
 * The dashes are in the texture, not cut from the line, because the map holds a stamp of a texture for each span of a
 * line that has it, some 15 KB: a span for each dash was thousands of them on a long route, more than the memory of
 * the app.
 */
@Composable
internal fun rememberCrayonDashTexture(color: Color): ImageBitmap = remember(color) {
    val (height, stroke) = dashTextureRows()
    drawCrayon(color, height = height, stroke = stroke)
}

/** How tall the texture of a dash and its gap is and how many of its rows from the top are the dash. */
internal fun dashTextureRows(): Pair<Int, Int> {
    // In widths of the dashed line
    val dash = OFF_ROAD_DASH / OFF_ROAD_WIDTH
    val gap = OFF_ROAD_GAP / OFF_ROAD_WIDTH
    return (TEXTURE_SIZE * (dash + gap)).roundToInt() to (TEXTURE_SIZE * dash).roundToInt()
}

/**
 * The crayon over the first [stroke] rows of an image [height] tall, the rest of it clear. A stroke shorter than the
 * image is a dash, its ends worn round; one as tall as it goes on into the next stamp, its noise repeats over it.
 */
private fun drawCrayon(color: Color, height: Int, stroke: Int): ImageBitmap {
    val image = ImageBitmap(TEXTURE_SIZE, height)
    // The noise of a stroke that goes on repeats over its rows; a dash is all there is of it
    val rows = if (stroke == height) height else Int.MAX_VALUE / 2
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(TEXTURE_SIZE.toFloat(), height.toFloat())) {
        for (y in 0 until stroke) {
            // The edges wander in and out, each its own way, a little rough from row to row
            val left = 0.05f + 0.08f * noise(y, cell = 8, seed = 1, rows = rows) + 0.03f * hash(y, 1, 9)
            val right = 0.95f - 0.08f * noise(y, cell = 8, seed = 2, rows = rows) - 0.03f * hash(y, 2, 9)
            // Along a dash, how far its nearer end is, in widths of the line
            val end = if (stroke == height) Float.MAX_VALUE else (minOf(y, stroke - 1 - y) + 0.5f) / TEXTURE_SIZE
            for (x in 0 until TEXTURE_SIZE) {
                val across = (x + 0.5f) / TEXTURE_SIZE
                var coverage = ((across - left) / EDGE).coerceIn(0f, 1f) * ((right - across) / EDGE).coerceIn(0f, 1f)
                if (end < DASH_END + DASH_END_ROUGHNESS) {
                    // The end of a dash is worn round, unevenly
                    val off = ((across - 0.5f) / 0.5f).coerceIn(-1f, 1f)
                    val rough = DASH_END_ROUGHNESS * hash(x / 4, if (y < stroke / 2) 0 else 1, 10)
                    val worn = DASH_END * (1 - sqrt(1 - off * off)) + rough
                    coverage *= ((end - worn) / EDGE).coerceIn(0f, 1f)
                }
                if (coverage <= 0f) continue
                // Blots where the crayon pressed harder, and the grain of the paper under it
                var value = 0.62f + 0.25f * noise(x, y, cell = 16, seed = 3, rows = rows) +
                    0.13f * noise(x, y, cell = 4, seed = 4, rows = rows)
                // Specks where the paper shows through
                if (hash(x, y, 5) < 0.12f) value *= 0.5f
                val alpha = (coverage * value).coerceIn(0f, 1f) * color.alpha
                drawRect(color.copy(alpha = alpha), topLeft = Offset(x.toFloat(), y.toFloat()), size = Size(1f, 1f))
            }
        }
    }
    return image
}

// How soft the edges of the stroke are, a part of its width
private const val EDGE = 0.05f

// How deep the worn round end of a dash is, and how uneven, parts of the width of the line
private const val DASH_END = 0.3f
private const val DASH_END_ROUGHNESS = 0.04f

/** Value noise from 0 to 1 along the height of the texture, repeating over [rows] of it. */
private fun noise(y: Int, cell: Int, seed: Int, rows: Int): Float {
    val period = rows / cell
    val position = y.toFloat() / cell
    val i = floor(position).toInt()
    val a = hash(i % period, 0, seed)
    val b = hash((i + 1) % period, 0, seed)
    return a + (b - a) * smoothstep(position - i)
}

/** Value noise from 0 to 1 over the texture, repeating over [rows] of its height. */
private fun noise(x: Int, y: Int, cell: Int, seed: Int, rows: Int): Float {
    val period = rows / cell
    val gx = x.toFloat() / cell
    val gy = y.toFloat() / cell
    val x0 = floor(gx).toInt()
    val y0 = floor(gy).toInt()
    val sx = smoothstep(gx - x0)
    val sy = smoothstep(gy - y0)
    fun corner(i: Int, j: Int) = hash(i, j % period, seed)
    val top = corner(x0, y0) + (corner(x0 + 1, y0) - corner(x0, y0)) * sx
    val bottom = corner(x0, y0 + 1) + (corner(x0 + 1, y0 + 1) - corner(x0, y0 + 1)) * sx
    return top + (bottom - top) * sy
}

private fun smoothstep(t: Float): Float = t * t * (3 - 2 * t)

/** A stable pseudo-random value from 0 to 1 for the cell ([x], [y]) of the noise [seed]. */
private fun hash(x: Int, y: Int, seed: Int): Float {
    var h = x * 374761393 + y * 668265263 + seed * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777215f
}
