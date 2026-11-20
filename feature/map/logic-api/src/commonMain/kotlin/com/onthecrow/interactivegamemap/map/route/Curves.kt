package com.onthecrow.interactivegamemap.map.route

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

// A segment gets a point of its curve for about every this much the line turns at its ends: the curve bends most
// near a sharp corner, so it takes this many to keep each of its own turns under about 15°
internal const val TURN_PER_POINT = PI / 36
private const val MAX_POINTS_PER_SEGMENT = 24
// A segment between bends gentler than this stays straight: they show no corner, and roads have many of them
private const val MIN_TURN = PI / 12

/**
 * The line through [points] as a smooth curve: a centripetal Catmull-Rom spline, which goes through every point, so
 * it stays on the roads it follows, with no corners and no loops. The points of [anchors], like the locations of a
 * route, cut it into pieces smoothed apart: there the line may turn sharply, under the pin of the location.
 */
internal fun smoothLine(points: List<WorldPoint>, anchors: Set<Int>): List<WorldPoint> {
    if (points.size < 3) return points
    val out = ArrayList<WorldPoint>(points.size * 3)
    out += points.first()
    var start = 0
    for (i in 1 until points.size) {
        if (i in anchors || i == points.lastIndex) {
            appendSpline(out, points.subList(start, i + 1))
            start = i
        }
    }
    return out
}

/** The spline through [piece] without its first point; its ends go straight toward their neighbours. */
private fun appendSpline(out: MutableList<WorldPoint>, piece: List<WorldPoint>) {
    if (piece.size < 3) {
        out += piece.drop(1)
        return
    }
    val last = piece.lastIndex
    for (k in 0 until last) {
        val p1 = piece[k]
        val p2 = piece[k + 1]
        // Beyond the ends a point as far again on the line, so the ends of the curve point along their segments
        val p0 = if (k > 0) piece[k - 1] else reflect(p2, p1)
        val p3 = if (k + 1 < last) piece[k + 2] else reflect(p1, p2)
        val turn = max(turnAngle(p0, p1, p2), turnAngle(p1, p2, p3))
        val steps = if (turn < MIN_TURN) 1 else ceil(turn / TURN_PER_POINT).toInt().coerceIn(1, MAX_POINTS_PER_SEGMENT)
        for (s in 1 until steps) out += catmullRom(p0, p1, p2, p3, s.toDouble() / steps)
        out += p2
    }
}

/** [point] mirrored through [center]. */
private fun reflect(point: WorldPoint, center: WorldPoint): WorldPoint =
    WorldPoint(2 * center.x - point.x, 2 * center.y - point.y)

/** The point at [fraction] of the way from [p1] to [p2] on the centripetal Catmull-Rom spline of the four points. */
private fun catmullRom(p0: WorldPoint, p1: WorldPoint, p2: WorldPoint, p3: WorldPoint, fraction: Double): WorldPoint {
    // The knots are spaced by the square roots of the distances: no cusps and no loops within a segment
    val t0 = 0.0
    val t1 = t0 + knot(p0, p1)
    val t2 = t1 + knot(p1, p2)
    val t3 = t2 + knot(p2, p3)
    val t = t1 + (t2 - t1) * fraction
    val a1 = lerp(p0, p1, t0, t1, t)
    val a2 = lerp(p1, p2, t1, t2, t)
    val a3 = lerp(p2, p3, t2, t3, t)
    val b1 = lerp(a1, a2, t0, t2, t)
    val b2 = lerp(a2, a3, t1, t3, t)
    return lerp(b1, b2, t1, t2, t)
}

private fun knot(a: WorldPoint, b: WorldPoint): Double = max(sqrt(a.distanceTo(b)), 1e-12)

private fun lerp(a: WorldPoint, b: WorldPoint, ta: Double, tb: Double, t: Double): WorldPoint {
    val wb = (t - ta) / (tb - ta)
    return WorldPoint(a.x + (b.x - a.x) * wb, a.y + (b.y - a.y) * wb)
}

/** How much the line turns at [corner], 0 straight on, π back the way it came. */
private fun turnAngle(previous: WorldPoint, corner: WorldPoint, next: WorldPoint): Double {
    val ax = corner.x - previous.x
    val ay = corner.y - previous.y
    val bx = next.x - corner.x
    val by = next.y - corner.y
    val lengths = sqrt((ax * ax + ay * ay) * (bx * bx + by * by))
    if (lengths == 0.0) return 0.0
    return acos(((ax * bx + ay * by) / lengths).coerceIn(-1.0, 1.0))
}
