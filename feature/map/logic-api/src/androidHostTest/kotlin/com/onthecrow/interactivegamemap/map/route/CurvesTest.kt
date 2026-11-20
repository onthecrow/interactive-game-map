package com.onthecrow.interactivegamemap.map.route

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurvesTest {
    private fun p(x: Double, y: Double) = WorldPoint(x, y)

    // The sharpest turn of the line, but at the points of [skip]
    private fun maxTurn(points: List<WorldPoint>, skip: Set<WorldPoint> = emptySet()): Double {
        var max = 0.0
        for (i in 1 until points.size - 1) {
            if (points[i] in skip) continue
            val ax = points[i].x - points[i - 1].x
            val ay = points[i].y - points[i - 1].y
            val bx = points[i + 1].x - points[i].x
            val by = points[i + 1].y - points[i].y
            val la = hypot(ax, ay)
            val lb = hypot(bx, by)
            if (la == 0.0 || lb == 0.0) continue
            max = maxOf(max, acos(((ax * bx + ay * by) / (la * lb)).coerceIn(-1.0, 1.0)))
        }
        return max
    }

    private fun degrees(radians: Double) = radians * 180 / PI

    @Test
    fun `a road of short segments bending a lot becomes a smooth curve through all its points`() {
        // A quarter of a circle in 6 segments, 15° each, then a turn of about 80° onto a straight road
        val road = (0..6).map { k -> p(10 * cos(PI / 2 * k / 6), 10 * sin(PI / 2 * k / 6)) } + p(0.0, 20.0)
        val curved = smoothLine(road, anchors = emptySet())
        assertTrue(curved.containsAll(road), "goes through every point of the road")
        assertEquals(road.first(), curved.first())
        assertEquals(road.last(), curved.last())
        assertTrue(maxTurn(road) > PI / 4)
        assertTrue(degrees(maxTurn(curved)) < 20, "${degrees(maxTurn(curved))}°")
    }

    @Test
    fun `a right angle of long sides is rounded`() {
        val line = listOf(p(0.0, 0.0), p(10.0, 0.0), p(10.0, 10.0))
        val curved = smoothLine(line, anchors = emptySet())
        assertTrue(curved.size > line.size)
        assertTrue(degrees(maxTurn(curved)) < 20, "${degrees(maxTurn(curved))}°")
    }

    @Test
    fun `the line may turn sharply at an anchor, smoothly elsewhere`() {
        val stop = p(10.0, 0.0)
        val line = listOf(p(0.0, 0.0), p(5.0, 0.5), stop, p(10.0, 10.0), p(15.0, 10.5), p(20.0, 10.0))
        val curved = smoothLine(line, anchors = setOf(2))
        assertTrue(stop in curved)
        assertTrue(degrees(maxTurn(curved, skip = setOf(stop))) < 20, "${degrees(maxTurn(curved, skip = setOf(stop)))}°")
    }

    @Test
    fun `a straight line stays as it is`() {
        val line = listOf(p(0.0, 0.0), p(1.0, 0.0), p(2.0, 0.0))
        assertEquals(line, smoothLine(line, anchors = emptySet()))
    }
}
