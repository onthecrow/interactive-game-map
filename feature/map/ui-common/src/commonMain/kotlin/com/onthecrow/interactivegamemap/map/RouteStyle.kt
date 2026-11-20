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
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

// The route stays visible zoomed far out and doesn't cover the map zoomed in
private val MinRouteWidth = 4.dp
private val MaxRouteWidth = 24.dp

// The legs across country and the jumps to fast travel points are crayon dashes half as wide as the route along the
// roads, as on the game's map; their lengths are in widths of the road
private const val OFF_ROAD_WIDTH = 0.5f
private val MinOffRoadWidth = 2.dp
internal const val OFF_ROAD_DASH = 2.2f
internal const val OFF_ROAD_GAP = 1.4f

// Google Maps draws the world 256 dp wide at zoom 0
private const val WORLD_DP = 256.0

/** The width of the route along the roads at [zoom]: twice as wide a zoom level in, like the roads on the tiles. */
internal fun MapStyle.routeLineWidth(zoom: Float): Dp {
    val width = routeWidth.value * 2f.pow(zoom - routeWidthZoom)
    return width.coerceIn(MinRouteWidth.value, MaxRouteWidth.value).dp
}

/** The width of the dashes of the legs across country and of the jumps to fast travel points at [zoom]. */
internal fun MapStyle.offRoadLineWidth(zoom: Float): Dp =
    maxOf(routeLineWidth(zoom).value * OFF_ROAD_WIDTH, MinOffRoadWidth.value).dp

/**
 * The dashes of the line through [points] as the map shows it at [zoom]: [dash] dp long with [gap] dp between, from
 * its start; each dash is the points of the line along it. The last one may be shorter, a line shorter than [dash] is
 * one dash.
 */
internal fun dashes(points: List<GeoPoint>, zoom: Float, dash: Float, gap: Float): List<List<GeoPoint>> {
    if (points.size < 2 || dash <= 0f) return listOf(points)
    val scale = WORLD_DP * 2.0.pow(zoom.toDouble())
    val world = points.map { worldDp(it, scale) }
    val out = mutableListOf<List<GeoPoint>>()
    var current = mutableListOf<GeoPoint>()
    var isDash = true
    // How much of the dash or the gap is left
    var left = dash.toDouble()
    current += points.first()
    for (k in 0 until world.size - 1) {
        val (ax, ay) = world[k]
        val (bx, by) = world[k + 1]
        val length = hypot(bx - ax, by - ay)
        var done = 0.0
        while (length - done > left) {
            done += left
            val t = done / length
            val point = fromWorldDp(ax + (bx - ax) * t, ay + (by - ay) * t, scale)
            if (isDash) {
                current += point
                out += current
            } else {
                current = mutableListOf(point)
            }
            isDash = !isDash
            left = if (isDash) dash.toDouble() else gap.toDouble()
        }
        left -= length - done
        if (isDash) current += points[k + 1]
    }
    if (isDash && current.size >= 2) out += current
    return out
}

private fun worldDp(point: GeoPoint, scale: Double): Pair<Double, Double> {
    val s = sin(point.lat * PI / 180)
    val x = (point.lng + 180) / 360 * scale
    val y = (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * scale
    return x to y
}

private fun fromWorldDp(x: Double, y: Double, scale: Double): GeoPoint {
    val lat = atan(sinh(PI - 2 * PI * y / scale)) * 180 / PI
    return GeoPoint(lat = lat, lng = x / scale * 360 - 180)
}

// The texture is a square: the map stamps it along the line, as wide as the line and as long
private const val TEXTURE_SIZE = 64

/**
 * A stroke of a red crayon on paper, stamped along the route by the map: its edges are ragged, its colour uneven in
 * blots and grain, with specks of paper showing through. The top of the image is toward the start of the line and the
 * bottom of one stamp meets the top of the next, so every noise here repeats over the height of the image.
 */
@Composable
internal fun rememberCrayonTexture(color: Color): ImageBitmap = remember(color) { drawCrayon(color) }

private fun drawCrayon(color: Color): ImageBitmap {
    val image = ImageBitmap(TEXTURE_SIZE, TEXTURE_SIZE)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(TEXTURE_SIZE.toFloat(), TEXTURE_SIZE.toFloat())) {
        for (y in 0 until TEXTURE_SIZE) {
            // The edges wander in and out, each its own way, a little rough from row to row
            val left = 0.05f + 0.08f * noise(y, cell = 8, seed = 1) + 0.03f * hash(y, 1, 9)
            val right = 0.95f - 0.08f * noise(y, cell = 8, seed = 2) - 0.03f * hash(y, 2, 9)
            for (x in 0 until TEXTURE_SIZE) {
                val across = (x + 0.5f) / TEXTURE_SIZE
                val coverage = ((across - left) / EDGE).coerceIn(0f, 1f) * ((right - across) / EDGE).coerceIn(0f, 1f)
                if (coverage <= 0f) continue
                // Blots where the crayon pressed harder, and the grain of the paper under it
                var value = 0.62f + 0.25f * noise(x, y, cell = 16, seed = 3) + 0.13f * noise(x, y, cell = 4, seed = 4)
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

/** Value noise from 0 to 1 along the height of the texture, repeating over it. */
private fun noise(y: Int, cell: Int, seed: Int): Float {
    val period = TEXTURE_SIZE / cell
    val position = y.toFloat() / cell
    val i = floor(position).toInt()
    val a = hash(i % period, 0, seed)
    val b = hash((i + 1) % period, 0, seed)
    return a + (b - a) * smoothstep(position - i)
}

/** Value noise from 0 to 1 over the texture, repeating over its height. */
private fun noise(x: Int, y: Int, cell: Int, seed: Int): Float {
    val period = TEXTURE_SIZE / cell
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
