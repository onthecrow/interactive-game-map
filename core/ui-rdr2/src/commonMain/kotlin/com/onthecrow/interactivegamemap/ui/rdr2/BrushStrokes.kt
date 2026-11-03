package com.onthecrow.interactivegamemap.ui.rdr2

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// The brush work of the look: ink strokes with ragged edges and dry-brush ends, laid over each other, and the
// hand-painted red outline of a selection. A port of tools/figma-rdr2-ds/brush.js, which draws the design
// system in Figma from the same seeds: keep the two in step. The shapes are made in dp and stay inside the box.

/** mulberry32, the same numbers as in brush.js. */
private class BrushRandom(seed: Int) {
    private var a = seed

    fun next(): Double {
        a += 0x6D2B79F5
        var t = a
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

/** A smooth wave of ±[amp] along [length], with knots every [step]. */
private class BrushWave(r: BrushRandom, length: Double, private val step: Double, amp: Double) {
    private val knots = DoubleArray(max(2, ceil(length / step).toInt() + 1)) { (r.next() * 2 - 1) * amp }

    operator fun invoke(t: Double): Double {
        val f = (t / step).coerceIn(0.0, knots.size - 1.0001)
        val i = floor(f).toInt()
        val u = f - i
        val s = u * u * (3 - 2 * u)
        return knots[i] + (knots[i + 1] - knots[i]) * s
    }
}

/** Bristles: runs of a few samples that end at about the same depth, 0 is the edge of the box. */
private fun brushBristles(r: BrushRandom, count: Int): DoubleArray {
    val depths = DoubleArray(count)
    var filled = 0
    while (filled < count) {
        val run = 1 + floor(r.next() * 4).toInt()
        val depth = r.next().pow(1.7)
        var i = 0
        while (i < run && filled < count) {
            depths[filled++] = (depth + (r.next() - 0.5) * 0.12).coerceIn(0.0, 1.0)
            i++
        }
    }
    return depths
}

/** A loose bristle or a stroke of the brush past the ink, in the color of its layer at [alpha] of it. */
internal class BrushStreak(val rect: Rect, val alpha: Float)

/** One layer of ink, drawn at [alpha] in the color of the element. */
internal class BrushLayer(val path: Path, val alpha: Float, val streaks: List<BrushStreak>)

private class PolygonBuilder(private val scale: Float, fillType: PathFillType = PathFillType.EvenOdd) {
    val path = Path().apply { this.fillType = fillType }

    fun polygon(points: List<Pair<Double, Double>>) {
        points.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x.toFloat() * scale, y.toFloat() * scale) else path.lineTo(x.toFloat() * scale, y.toFloat() * scale)
        }
        path.close()
    }
}

private fun streak(x: Double, y: Double, w: Double, h: Double, alpha: Double, scale: Float) = BrushStreak(
    rect = Rect(Offset(x.toFloat() * scale, y.toFloat() * scale), Size(w.toFloat() * scale, h.toFloat() * scale)),
    alpha = alpha.toFloat(),
)

/**
 * One horizontal ink stroke filling x0..x1, y0..y1: wavy long edges, ragged ends [end] deep with dry-brush slits
 * near them, and loose bristles past the ends.
 */
private fun inkStroke(
    r: BrushRandom,
    x0: Double,
    y0: Double,
    x1: Double,
    y1: Double,
    amp: Double,
    endDepth: Double,
    waveStep: Double,
    slits: Int?,
    alpha: Float,
    scale: Float,
): BrushLayer {
    val w = x1 - x0
    val h = y1 - y0
    val end = min(endDepth, w / 4)
    val top = BrushWave(r, w, waveStep, amp)
    val bottom = BrushWave(r, w, waveStep, amp)
    val step = 6.0
    val rows = max(3, (h / 2.2).roundToInt())
    val right = brushBristles(r, rows + 1)
    val left = brushBristles(r, rows + 1)
    val points = mutableListOf<Pair<Double, Double>>()
    var x = end
    while (x <= w - end) {
        points += x0 + x to y0 + amp + top(x)
        x += step
    }
    for (i in 0..rows) points += x1 - end * right[i] to y0 + amp + (h - 2 * amp) * i / rows
    x = w - end
    while (x >= end) {
        points += x0 + x to y1 - amp + bottom(x)
        x -= step
    }
    for (i in rows downTo 0) points += x0 + end * left[i] to y0 + amp + (h - 2 * amp) * i / rows
    val builder = PolygonBuilder(scale)
    builder.polygon(points)
    repeat(slits ?: (h / 6).roundToInt()) {
        val atRight = r.next() < 0.5
        val len = 6 + r.next() * min(40.0, w * 0.18)
        val sh = 0.5 + r.next() * 1.1
        val sy = y0 + amp * 2 + r.next() * max(1.0, h - amp * 4 - sh)
        val sx = if (atRight) x1 - end - r.next() * w * 0.12 - len else x0 + end + r.next() * w * 0.12
        builder.polygon(listOf(sx to sy, sx + len to sy + r.next() * 0.4, sx + len to sy + sh, sx to sy + sh))
    }
    val streaks = List((h / 4).roundToInt()) {
        val atRight = r.next() < 0.5
        val len = end * (0.4 + r.next() * 0.9)
        val sh = 0.6 + r.next() * 1.6
        val sy = y0 + amp + r.next() * (h - 2 * amp - sh)
        val sx = if (atRight) x1 - len - r.next() * end * 0.3 else x0 + r.next() * end * 0.3
        streak(sx, sy, len, sh, 0.25 + r.next() * 0.6, scale)
    }
    return BrushLayer(builder.path, alpha, streaks)
}

/**
 * The background of a row, a field or a button, [w] × [h] dp: two strokes over each other, the lower one wider
 * and fainter, so the ends and the edges show the layers.
 */
internal fun brushStroke(w: Double, h: Double, seed: Int, scale: Float, underAlpha: Float = 0.45f, overAlpha: Float = 0.78f): List<BrushLayer> {
    val r = BrushRandom(seed)
    val amp = (h / 48).coerceIn(0.5, 1.2)
    val end = min(14.0, h * 0.3)
    val under = inkStroke(r, 0.0, 0.0, w, h, amp * 1.4, end * 1.15, waveStep = 34.0, slits = 0, alpha = underAlpha, scale = scale)
    val insetX = min(5.0, w / 30)
    val insetY = min(2.0, h / 16)
    val over = inkStroke(r, insetX, insetY, w - insetX * 0.6, h - insetY, amp, end * 0.75, waveStep = 30.0, slits = null, alpha = overAlpha, scale = scale)
    return listOf(under, over)
}

/** A number in 0..1 for the [k]th value of a [channel] of [seed], always the same for the same three, as in brush.js. */
private fun brushHash(seed: Int, channel: Int, k: Int): Double {
    // Int arithmetic wraps as `| 0` and Math.imul do in JS
    var x = seed + channel * 7919 + k * 15485863
    x = x xor (x ushr 16)
    x *= 0x7FEB352D
    x = x xor (x ushr 15)
    x *= 0x846CA68B.toInt()
    x = x xor (x ushr 16)
    return (x.toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
}

/** A smooth wave of ±[amp] along a side, its knots every [step] from the side's start: it stays put as the side grows. */
private fun sideWave(seed: Int, channel: Int, t: Double, step: Double, amp: Double): Double {
    val f = t / step
    val k = floor(f).toInt()
    val u = f - k
    val s = u * u * (3 - 2 * u)
    val a = brushHash(seed, channel, k) * 2 - 1
    val b = brushHash(seed, channel, k + 1) * 2 - 1
    return (a + (b - a) * s) * amp
}

/** The rare blobs of a side, where the brush left more paint or skipped: how far the edge strays out at [t]. */
private fun sideBlobs(seed: Int, channel: Int, t: Double): Double {
    val cell = 48.0
    val k = floor(t / cell).toInt()
    var sum = 0.0
    for (c in k - 1..k + 1) {
        val p = brushHash(seed, channel, c * 4)
        if (p >= 0.3 && p < 0.85) continue
        val center = (c + brushHash(seed, channel, c * 4 + 1)) * cell
        val width = 5 + brushHash(seed, channel, c * 4 + 2) * 7
        val v = brushHash(seed, channel, c * 4 + 3)
        val height = if (p < 0.3) 1.2 + v * 1.8 else -(0.6 + v * 0.8)
        val d = (t - center) / width
        if (abs(d) < 1) sum += height * (0.5 + 0.5 * cos(PI * d))
    }
    return sum
}

private const val EDGE_STEP = 3.0

/** [from], [to] and the points of the 3 dp grid between them, so the samples of a side stay put as it grows. */
private fun gridSteps(from: Double, to: Double): List<Double> {
    val out = mutableListOf(from)
    var k = floor(from / EDGE_STEP).toInt() + 1
    while (k * EDGE_STEP < to) {
        out += k * EDGE_STEP
        k++
    }
    out += to
    return out
}

/**
 * The sides of a panel: 0 top, 1 right, 2 bottom, 3 left, a point of each by its position along the side (x for
 * top and bottom, y for the others) and how far out of the edge [inset] from the box it is.
 */
private class PanelSides(val w: Double, val h: Double) {
    fun point(side: Int, t: Double, out: Double, inset: Double): Pair<Double, Double> = when (side) {
        0 -> t to inset - out
        1 -> w - inset + out to t
        2 -> t to h - inset + out
        else -> inset - out to t
    }

    fun range(side: Int, inset: Double, corner: Double): Pair<Double, Double> =
        if (side % 2 == 0) inset + corner to w - inset - corner else inset + corner to h - inset - corner

    /** The outline clockwise, the corners rounded by [corner], [edge] tells how far a side strays out at a point. */
    fun outline(inset: Double, corner: Double, edge: (side: Int, t: Double) -> Double): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val x0 = inset + corner
        val x1 = w - inset - corner
        val y0 = inset + corner
        val y1 = h - inset - corner
        fun arc(cx: Double, cy: Double, from: Double) {
            for (a in listOf(from + 30, from + 60)) points += cx + corner * cos(a * PI / 180) to cy + corner * sin(a * PI / 180)
        }
        gridSteps(x0, x1).forEach { points += point(0, it, edge(0, it), inset) }
        arc(x1, y0, -90.0)
        gridSteps(y0, y1).forEach { points += point(1, it, edge(1, it), inset) }
        arc(x1, y1, 0.0)
        gridSteps(x0, x1).asReversed().forEach { points += point(2, it, edge(2, it), inset) }
        arc(x0, y1, 90.0)
        gridSteps(y0, y1).asReversed().forEach { points += point(3, it, edge(3, it), inset) }
        arc(x0, y0, 180.0)
        return points
    }
}

private fun circlePoints(cx: Double, cy: Double, r: Double): List<Pair<Double, Double>> =
    List(12) { i -> cx + r * cos(2 * PI * i / 12) to cy + r * sin(2 * PI * i / 12) }

/**
 * A panel of the game, [w] × [h] dp, as a brush laid it in haste: a wash with an almost straight edge and the
 * streaks of a dry brush along it, and over it the black sheet with an edge that wanders a little, swells into
 * a blob here and there, sheds a few drops of paint around and drips at the bottom, with a gap of the dry brush
 * along a side now and then. Everything is placed by its position along a side, so a panel which grows taller,
 * like a sheet being dragged, keeps its sides and only moves its bottom.
 */
internal fun brushPanel(w: Double, h: Double, seed: Int, scale: Float, underAlpha: Float = 0.5f, overAlpha: Float = 0.92f): List<BrushLayer> {
    val sides = PanelSides(w, h)

    // The wash and its streaks
    val washInset = 1.5
    val washCorner = min(4.0, min(w, h) / 2 - washInset - 1).coerceAtLeast(0.0)
    val wash = PolygonBuilder(scale, PathFillType.NonZero)
    wash.polygon(
        sides.outline(washInset, washCorner) { side, t ->
            sideWave(seed, 100 + side * 16, t, 70.0, 0.9) + sideWave(seed, 101 + side * 16, t, 8.0, 0.2)
        },
    )
    val streaks = mutableListOf<BrushStreak>()
    for (side in 0..3) {
        val (from, to) = sides.range(side, washInset, washCorner)
        val ch = 106 + side * 16
        val cell = 14.0
        for (c in ceil(from / cell).toInt() until floor(to / cell).toInt()) {
            if (brushHash(seed, ch, c * 8) >= 0.55) continue
            val start = (c + brushHash(seed, ch, c * 8 + 1)) * cell
            val len = min(10 + brushHash(seed, ch, c * 8 + 2) * 40, to - start)
            val offset = 2.0 + brushHash(seed, ch, c * 8 + 3) * 5.5
            val width = 0.4 + brushHash(seed, ch, c * 8 + 4) * 0.9
            val alpha = 0.15 + brushHash(seed, ch, c * 8 + 5) * 0.3
            if (len < 4) continue
            val (x, y, sw, sh) = when (side) {
                0 -> listOf(start, offset, len, width)
                1 -> listOf(w - offset - width, start, width, len)
                2 -> listOf(start, h - offset - width, len, width)
                else -> listOf(offset, start, width, len)
            }
            streaks += streak(x, y, sw, sh, alpha, scale)
        }
    }

    // The black sheet with its blobs, drops, drips and gaps
    val inset = 8.0
    val corner = min(6.0, min(w, h) / 2 - inset - 1).coerceAtLeast(0.0)
    fun edge(side: Int, t: Double) =
        sideWave(seed, side * 16, t, 26.0, 1.3) + sideWave(seed, side * 16 + 1, t, 5.0, 0.3) + sideBlobs(seed, side * 16 + 2, t)
    val sheet = PolygonBuilder(scale, PathFillType.NonZero)
    sheet.polygon(sides.outline(inset, corner, ::edge))
    for (side in 0..3) {
        val (from, to) = sides.range(side, inset, corner)
        // Drops around the edge
        var ch = side * 16 + 3
        var cell = 40.0
        for (c in ceil(from / cell).toInt() until floor(to / cell).toInt()) {
            if (brushHash(seed, ch, c * 16) >= 0.3) continue
            val count = 1 + (brushHash(seed, ch, c * 16 + 1) * 3).toInt()
            for (j in 0 until count) {
                val t = (c + brushHash(seed, ch, c * 16 + 2 + j * 3)) * cell
                val radius = 0.5 + brushHash(seed, ch, c * 16 + 3 + j * 3) * 1.1
                val out = edge(side, t)
                val room = inset - out - radius - 0.3
                val near = radius + 0.6
                if (room < near) continue
                val (cx, cy) = sides.point(side, t, out + near + brushHash(seed, ch, c * 16 + 4 + j * 3) * (room - near), inset)
                sheet.polygon(circlePoints(cx, cy, radius))
            }
        }
        // Drips under the bottom
        if (side == 2) {
            ch = side * 16 + 4
            cell = 30.0
            for (c in ceil(from / cell).toInt() until floor(to / cell).toInt()) {
                if (brushHash(seed, ch, c * 8) >= 0.35) continue
                val cx = (c + brushHash(seed, ch, c * 8 + 1)) * cell
                val width = 1.6 + brushHash(seed, ch, c * 8 + 2) * 2.0
                val out = edge(2, cx)
                val len = min(inset - out - 0.6, 3 + brushHash(seed, ch, c * 8 + 3) * 7)
                if (len < 2) continue
                val base = h - inset + out - 1
                val tip = base + len - width * 0.35
                val points = mutableListOf(cx - width / 2 to base, cx + width / 2 to base, cx + width * 0.35 to base + len * 0.6)
                for (i in 0..6) {
                    val a = PI * i / 6
                    points += cx + width * 0.35 * cos(a) to tip + width * 0.35 * sin(a)
                }
                points += cx - width * 0.35 to base + len * 0.6
                sheet.polygon(points)
            }
        }
        // Gaps of the dry brush along the side, wound the other way to cut through
        ch = side * 16 + 5
        cell = 34.0
        for (c in ceil(from / cell).toInt() until floor(to / cell).toInt()) {
            if (brushHash(seed, ch, c * 8) >= 0.4) continue
            val len = 8 + brushHash(seed, ch, c * 8 + 1) * 14
            val start = (c + brushHash(seed, ch, c * 8 + 2) * (1 - len / cell)) * cell
            val depth = 3.5 + brushHash(seed, ch, c * 8 + 3) * 3.0
            val width = 0.4 + brushHash(seed, ch, c * 8 + 4) * 0.4
            val (ax, ay, bx, by) = when (side) {
                0 -> listOf(start, inset + depth, start + len, inset + depth + width)
                1 -> listOf(w - inset - depth - width, start, w - inset - depth, start + len)
                2 -> listOf(start, h - inset - depth - width, start + len, h - inset - depth)
                else -> listOf(inset + depth, start, inset + depth + width, start + len)
            }
            sheet.polygon(listOf(ax to ay, ax to by, bx to by, bx to ay))
        }
    }
    return listOf(BrushLayer(wash.path, underAlpha, streaks), BrushLayer(sheet.path, overAlpha, emptyList()))
}

/**
 * The red outline of a selection around a [w] × [h] dp box: four painted sides, each a little wavy and uneven
 * in width, running a touch past the corners. The box is the outer edge of the paint.
 */
internal fun brushOutline(w: Double, h: Double, seed: Int, scale: Float, width: Double = 3.5, overshoot: Double = 1.6): Path {
    val r = BrushRandom(seed)
    val half = width / 2
    val m = half + 0.8
    val sides = listOf(
        (m - overshoot * r.next() to m) to (w - m + overshoot * r.next() to m),
        (w - m to m - overshoot * r.next()) to (w - m to h - m + overshoot * r.next()),
        (w - m + overshoot * r.next() to h - m) to (m - overshoot * r.next() to h - m),
        (m to h - m + overshoot * r.next()) to (m to m - overshoot * r.next()),
    )
    val builder = PolygonBuilder(scale)
    sides.forEach { (a, b) ->
        val dx = b.first - a.first
        val dy = b.second - a.second
        val len = hypot(dx, dy)
        val ux = dx / len
        val uy = dy / len
        // The normal, to the left of the direction
        val nx = -uy
        val ny = ux
        val wobble = BrushWave(r, len, 40.0, 0.45)
        val thickness = BrushWave(r, len, 22.0, width * 0.18)
        val n = max(2, (len / 3).roundToInt())
        val upper = mutableListOf<Pair<Double, Double>>()
        val lower = mutableListOf<Pair<Double, Double>>()
        for (i in 0..n) {
            val t = i.toDouble() / n * len
            // A nick here and there, thinner tips at the ends
            val tip = min(1.0, min(t, len - t) / 4 + 0.55)
            val nick = if (r.next() < 0.04) 0.55 else 1.0
            val hw = (half + thickness(t)) * tip * nick
            val cx = a.first + ux * t + nx * wobble(t)
            val cy = a.second + uy * t + ny * wobble(t)
            val up = hw + (r.next() - 0.5) * 0.5
            val low = hw + (r.next() - 0.5) * 0.5
            upper += cx + nx * up to cy + ny * up
            lower += cx - nx * low to cy - ny * low
        }
        builder.polygon(upper + lower.asReversed())
    }
    return builder.path
}

/**
 * A painted ring just inside a disc of [d] dp, as uneven as the outlines: the ring of an active control.
 */
internal fun brushRing(d: Double, seed: Int, scale: Float, width: Double = 3.0): Path {
    val r = BrushRandom(seed)
    val radius = d / 2 - width / 2 - 1
    val wobble = BrushWave(r, 2 * PI * radius, 30.0, 0.35)
    val thickness = BrushWave(r, 2 * PI * radius, 18.0, width * 0.2)
    val n = max(24, (2 * PI * radius / 2.5).roundToInt())
    val outer = mutableListOf<Pair<Double, Double>>()
    val inner = mutableListOf<Pair<Double, Double>>()
    for (i in 0 until n) {
        val angle = 2 * PI * i / n
        val t = radius * angle
        val c = radius + wobble(t)
        val hw = width / 2 + thickness(t)
        outer += d / 2 + (c + hw) * cos(angle) to d / 2 + (c + hw) * sin(angle)
        inner += d / 2 + (c - hw) * cos(angle) to d / 2 + (c - hw) * sin(angle)
    }
    val builder = PolygonBuilder(scale)
    builder.polygon(outer)
    builder.polygon(inner.asReversed())
    return builder.path
}

/**
 * The edge of the game's white discs, the cores of its prompts, inside a box of [d] dp: a circle worn a little out
 * of round, its rim jittered as a printed one.
 */
internal fun brushDisc(d: Double, seed: Int, scale: Float): Path {
    val r = BrushRandom(seed)
    val radius = d / 2 - 1.2
    // A few slow bulges and a fine jitter
    val harmonics = List(3) { k -> Triple(k + 2, (0.006 + r.next() * 0.012) * radius, r.next() * 2 * PI) }
    val n = max(32, (PI * d / 1.5).roundToInt())
    val points = List(n) { i ->
        val a = 2 * PI * i / n
        val rr = radius + harmonics.sumOf { (k, amp, phase) -> amp * sin(k * a + phase) } + (r.next() - 0.5) * 0.5
        d / 2 + rr * cos(a) to d / 2 + rr * sin(a)
    }
    val builder = PolygonBuilder(scale)
    builder.polygon(points)
    return builder.path
}

/** A dot of grunge, in the units it was made in times the scale. */
internal class Speck(val center: Offset, val radius: Float, val alpha: Float)

/** A thin scratch through the glyph. */
internal class Scratch(val start: Offset, val end: Offset, val width: Float, val alpha: Float)

/**
 * The wear of a printed disc of [d] dp: [wear] and [scratches] eat into the glyph in the color of the disc,
 * [mottle] stains the disc around it.
 */
internal class Grunge(val wear: List<Speck>, val scratches: List<Scratch>, val mottle: List<Speck>)

internal fun brushGrunge(d: Double, seed: Int, scale: Float): Grunge {
    val r = BrushRandom(seed)
    val c = d / 2
    fun inside(within: Double): Offset {
        val angle = r.next() * 2 * PI
        val dist = kotlin.math.sqrt(r.next()) * within
        return Offset(((c + dist * cos(angle)) * scale).toFloat(), ((c + dist * sin(angle)) * scale).toFloat())
    }
    val wear = List((d * d / 14).roundToInt()) {
        val center = inside(c * 0.78)
        Speck(center, ((0.2 + r.next().pow(2) * 0.45) * scale).toFloat(), (0.7 + r.next() * 0.3).toFloat())
    }
    val scratches = List((d / 14).roundToInt()) {
        val start = inside(c * 0.6)
        val len = d * (0.06 + r.next() * 0.08) * scale
        val angle = r.next() * 2 * PI
        val end = Offset(start.x + (len * cos(angle)).toFloat(), start.y + (len * sin(angle)).toFloat())
        Scratch(start, end, ((0.25 + r.next() * 0.2) * scale).toFloat(), (0.6 + r.next() * 0.4).toFloat())
    }
    val mottle = List((d * d / 30).roundToInt()) {
        Speck(inside(c * 0.92), ((0.2 + r.next() * 0.7) * scale).toFloat(), (0.04 + r.next() * 0.08).toFloat())
    }
    return Grunge(wear, scratches, mottle)
}

/** A stable seed for an element of the given size, so a row keeps its strokes while it's on screen. */
internal fun brushSeed(w: Double, h: Double, salt: Int = 0): Int =
    (w.roundToInt() * 73856093) xor (h.roundToInt() * 19349663) xor (salt * 83492791)

/** The layers in [color], the first, lower one in [under]. */
internal fun DrawScope.drawBrushLayers(layers: List<BrushLayer>, color: Color, under: Color = color) {
    layers.forEachIndexed { index, layer ->
        val layerColor = if (index == 0) under else color
        drawPath(layer.path, layerColor, alpha = layer.alpha)
        layer.streaks.forEach { streak ->
            drawRect(layerColor, streak.rect.topLeft, streak.rect.size, alpha = streak.alpha * layer.alpha)
        }
    }
}

/**
 * Ink strokes behind the element, in [color]; [salt] tells apart the elements of the same size. The alphas are
 * the ones of the wider lower stroke and of the upper one.
 */
internal fun Modifier.brushStroke(color: Color, salt: Int = 0, underAlpha: Float = 0.45f, overAlpha: Float = 0.78f): Modifier =
    drawWithCache {
        val w = size.width / density.toDouble()
        val h = size.height / density.toDouble()
        val layers = brushStroke(w, h, brushSeed(w, h, salt), density, underAlpha, overAlpha)
        onDrawBehind { drawBrushLayers(layers, color) }
    }

/**
 * A ragged ink sheet behind the element, in [color] over a wider stroke in [rim], running [extendBottom] past its
 * bottom.
 */
internal fun Modifier.brushPanel(
    color: Color,
    rim: Color = color,
    salt: Int = 0,
    extendBottom: Dp = 0.dp,
    underAlpha: Float = 0.5f,
    overAlpha: Float = 0.92f,
): Modifier = drawWithCache {
    val w = size.width / density.toDouble()
    val h = size.height / density.toDouble() + extendBottom.value
    // Neither the seed nor the sides follow the height, which changes as a sheet is dragged
    val layers = brushPanel(w, h, brushSeed(w, 0.0, salt), density, underAlpha, overAlpha)
    onDrawBehind { drawBrushLayers(layers, color, rim) }
}

/** The red outline of the selection over the element while [visible]. */
internal fun Modifier.selectionOutline(visible: Boolean, color: Color, salt: Int = 0): Modifier = drawWithCache {
    val w = size.width / density.toDouble()
    val h = size.height / density.toDouble()
    val outline = if (visible && w > 8 && h > 8) brushOutline(w, h, brushSeed(w, h, salt + 7), density) else null
    onDrawWithContent {
        drawContent()
        outline?.let { drawPath(it, color) }
    }
}
