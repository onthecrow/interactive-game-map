package com.onthecrow.interactivegamemap.map.route

import com.onthecrow.interactivegamemap.map.model.TravelNetwork
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Where a place meets a road: [point] on the road, [along] its line from its start, [offRoad] straight from the place. */
internal class Attachment(
    val road: Int,
    val along: Double,
    val offRoad: Double,
    val point: WorldPoint,
)

/**
 * The roads of a [TravelNetwork] made ready for routes: the shortest distance between every two junctions, and the
 * road segments by where they are, to put places on the nearest roads.
 *
 * The distances of all the junctions are kept, junctions² of them: fine for the about 1 200 junctions of RDR2, a
 * network of many thousands would need them computed on demand instead.
 */
internal class RoadGraph(
    network: TravelNetwork,
    ensureActive: () -> Unit = {},
    onProgress: (Float) -> Unit = {},
) {
    val junctionCount: Int = network.junctions.size
    private val roadCount = network.roads.size
    private val roadFrom = IntArray(roadCount) { network.roads[it].from }
    private val roadTo = IntArray(roadCount) { network.roads[it].to }
    private val roadXs: Array<DoubleArray>
    private val roadYs: Array<DoubleArray>
    /** How far along its road each point of the road is. */
    private val roadAlong: Array<DoubleArray>
    private val roadLength = DoubleArray(roadCount)

    private val distances = FloatArray(junctionCount * junctionCount) { Float.POSITIVE_INFINITY }
    /** The road the shortest path from the junction of the row comes to the junction of the column by. */
    private val via = IntArray(junctionCount * junctionCount) { -1 }

    private val grid: SegmentGrid

    init {
        val junctions = network.junctions.map { it.toWorld() }
        val xs = arrayOfNulls<DoubleArray>(roadCount)
        val ys = arrayOfNulls<DoubleArray>(roadCount)
        val along = arrayOfNulls<DoubleArray>(roadCount)
        network.roads.forEachIndexed { r, road ->
            val points = road.path.map { it.toWorld() }.ifEmpty { listOf(junctions[road.from], junctions[road.to]) }
            xs[r] = DoubleArray(points.size) { points[it].x }
            ys[r] = DoubleArray(points.size) { points[it].y }
            val a = DoubleArray(points.size)
            for (k in 1 until points.size) a[k] = a[k - 1] + points[k].distanceTo(points[k - 1])
            along[r] = a
            roadLength[r] = a.last()
        }
        roadXs = xs.requireNoNulls()
        roadYs = ys.requireNoNulls()
        roadAlong = along.requireNoNulls()
        computeDistances(ensureActive, onProgress)
        grid = SegmentGrid(roadXs, roadYs)
    }

    fun from(road: Int): Int = roadFrom[road]

    fun to(road: Int): Int = roadTo[road]

    fun length(road: Int): Double = roadLength[road]

    /** Along the roads from the junction [u] to the junction [v]. */
    fun distance(u: Int, v: Int): Double = distances[u * junctionCount + v].toDouble()

    private fun computeDistances(ensureActive: () -> Unit, onProgress: (Float) -> Unit) {
        val n = junctionCount
        // The roads of each junction
        val degree = IntArray(n + 1)
        for (r in 0 until roadCount) {
            degree[roadFrom[r]]++
            degree[roadTo[r]]++
        }
        val start = IntArray(n + 1)
        for (j in 0 until n) start[j + 1] = start[j] + degree[j]
        val fill = start.copyOf()
        val adjacentRoad = IntArray(start[n])
        val adjacentJunction = IntArray(start[n])
        for (r in 0 until roadCount) {
            adjacentRoad[fill[roadFrom[r]]] = r
            adjacentJunction[fill[roadFrom[r]]++] = roadTo[r]
            adjacentRoad[fill[roadTo[r]]] = r
            adjacentJunction[fill[roadTo[r]]++] = roadFrom[r]
        }
        val heap = MinHeap(start[n] + n)
        val best = DoubleArray(n)
        val done = BooleanArray(n)
        for (s in 0 until n) {
            if (s % 64 == 0) {
                ensureActive()
                onProgress(s.toFloat() / n)
            }
            best.fill(Double.POSITIVE_INFINITY)
            done.fill(false)
            val row = s * n
            best[s] = 0.0
            heap.clear()
            heap.push(0.0, s)
            while (heap.isNotEmpty()) {
                val u = heap.popNode()
                if (done[u]) continue
                done[u] = true
                distances[row + u] = best[u].toFloat()
                for (k in start[u] until start[u + 1]) {
                    val v = adjacentJunction[k]
                    val r = adjacentRoad[k]
                    val d = best[u] + roadLength[r]
                    if (d < best[v]) {
                        best[v] = d
                        via[row + v] = r
                        heap.push(d, v)
                    }
                }
            }
        }
        onProgress(1f)
    }

    /**
     * The nearest roads to [point], the nearest one first: at most [count], those further than the nearest one by
     * more than [slack] of its distance, or [minSlack], are left out.
     */
    fun attach(point: WorldPoint, count: Int, slack: Double, minSlack: Double): List<Attachment> {
        val nearest = HashMap<Int, Attachment>()
        var limit = Double.POSITIVE_INFINITY
        grid.forEachSegmentAround(point, stopAt = { ringDistance -> ringDistance > limit }) { road, k ->
            val attachment = project(point, road, k)
            val current = nearest[road]
            if (current == null || attachment.offRoad < current.offRoad) {
                nearest[road] = attachment
                limit = min(limit, attachment.offRoad + max(attachment.offRoad * slack, minSlack))
            }
        }
        val closest = nearest.values.minOfOrNull { it.offRoad } ?: return emptyList()
        val within = closest + max(closest * slack, minSlack)
        return nearest.values.filter { it.offRoad <= within }.sortedBy { it.offRoad }.take(count)
    }

    private fun project(point: WorldPoint, road: Int, k: Int): Attachment {
        val xs = roadXs[road]
        val ys = roadYs[road]
        val dx = xs[k + 1] - xs[k]
        val dy = ys[k + 1] - ys[k]
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) 0.0 else (((point.x - xs[k]) * dx + (point.y - ys[k]) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        val onRoad = WorldPoint(xs[k] + dx * t, ys[k] + dy * t)
        val along = roadAlong[road][k] + (roadAlong[road][k + 1] - roadAlong[road][k]) * t
        return Attachment(road, along, point.distanceTo(onRoad), onRoad)
    }

    /** The line of [road] from [from] to [to] along it, either way, without the point at [from]. */
    fun appendRoadPart(out: MutableList<WorldPoint>, road: Int, from: Double, to: Double) {
        val along = roadAlong[road]
        if (from <= to) {
            for (k in along.indices) {
                if (along[k] > from && along[k] < to) out += WorldPoint(roadXs[road][k], roadYs[road][k])
            }
        } else {
            for (k in along.indices.reversed()) {
                if (along[k] < from && along[k] > to) out += WorldPoint(roadXs[road][k], roadYs[road][k])
            }
        }
        out += pointAt(road, to)
    }

    private fun pointAt(road: Int, distance: Double): WorldPoint {
        val along = roadAlong[road]
        val xs = roadXs[road]
        val ys = roadYs[road]
        if (distance <= 0.0) return WorldPoint(xs.first(), ys.first())
        if (distance >= along.last()) return WorldPoint(xs.last(), ys.last())
        var low = 0
        var high = along.size - 1
        while (high - low > 1) {
            val middle = (low + high) / 2
            if (along[middle] <= distance) low = middle else high = middle
        }
        val span = along[high] - along[low]
        val t = if (span == 0.0) 0.0 else (distance - along[low]) / span
        return WorldPoint(xs[low] + (xs[high] - xs[low]) * t, ys[low] + (ys[high] - ys[low]) * t)
    }

    /** The line of the shortest way along the roads from the junction [u] to [v], without the point of [u]. */
    fun appendJunctionPath(out: MutableList<WorldPoint>, u: Int, v: Int) {
        if (u == v) return
        val row = u * junctionCount
        val steps = mutableListOf<Int>()
        var current = v
        while (current != u) {
            val road = via[row + current]
            if (road < 0) return
            steps += road
            // A road coming to the junction by its end was taken forward
            current = if (roadTo[road] == current) roadFrom[road] else roadTo[road]
        }
        current = u
        for (road in steps.asReversed()) {
            val forward = roadFrom[road] == current
            appendRoadPart(out, road, if (forward) 0.0 else roadLength[road], if (forward) roadLength[road] else 0.0)
            current = if (forward) roadTo[road] else roadFrom[road]
        }
    }
}

/** The segments of the roads in square cells, to look for the ones near a point ring of cells by ring. */
private class SegmentGrid(private val xs: Array<DoubleArray>, private val ys: Array<DoubleArray>) {
    private val minX: Double
    private val minY: Double
    private val cell: Double
    private val columns: Int
    private val rows: Int
    private val cellStart: IntArray
    private val segmentRoad: IntArray
    private val segmentIndex: IntArray

    init {
        var x0 = Double.POSITIVE_INFINITY
        var y0 = Double.POSITIVE_INFINITY
        var x1 = Double.NEGATIVE_INFINITY
        var y1 = Double.NEGATIVE_INFINITY
        var segments = 0
        for (r in xs.indices) {
            for (k in xs[r].indices) {
                x0 = min(x0, xs[r][k]); x1 = max(x1, xs[r][k])
                y0 = min(y0, ys[r][k]); y1 = max(y1, ys[r][k])
            }
            segments += max(0, xs[r].size - 1)
        }
        if (segments == 0) {
            x0 = 0.0; y0 = 0.0; x1 = 1.0; y1 = 1.0
        }
        minX = x0
        minY = y0
        val width = max(x1 - x0, 1e-9)
        val height = max(y1 - y0, 1e-9)
        // About four segments a cell
        cell = sqrt(width * height / max(1, segments / 4))
        columns = max(1, ceil(width / cell).toInt())
        rows = max(1, ceil(height / cell).toInt())
        val counts = IntArray(columns * rows + 1)
        forEachCellOfSegments { c, _, _ -> counts[c + 1]++ }
        for (c in 0 until columns * rows) counts[c + 1] += counts[c]
        cellStart = counts.copyOf()
        val fill = counts.copyOf()
        segmentRoad = IntArray(counts.last())
        segmentIndex = IntArray(counts.last())
        forEachCellOfSegments { c, r, k ->
            segmentRoad[fill[c]] = r
            segmentIndex[fill[c]++] = k
        }
    }

    private inline fun forEachCellOfSegments(action: (cell: Int, road: Int, k: Int) -> Unit) {
        for (r in xs.indices) {
            for (k in 0 until xs[r].size - 1) {
                val c0 = column(min(xs[r][k], xs[r][k + 1]))
                val c1 = column(max(xs[r][k], xs[r][k + 1]))
                val r0 = row(min(ys[r][k], ys[r][k + 1]))
                val r1 = row(max(ys[r][k], ys[r][k + 1]))
                for (row in r0..r1) for (column in c0..c1) action(row * columns + column, r, k)
            }
        }
    }

    private fun column(x: Double): Int = floor((x - minX) / cell).toInt().coerceIn(0, columns - 1)

    private fun row(y: Double): Int = floor((y - minY) / cell).toInt().coerceIn(0, rows - 1)

    /**
     * Calls [action] with the segments of ring after ring of cells around [point], until the cells run out or [stopAt]
     * is true for how far the next ring is at least. A segment in several cells comes several times.
     */
    inline fun forEachSegmentAround(point: WorldPoint, stopAt: (ringDistance: Double) -> Boolean, action: (road: Int, k: Int) -> Unit) {
        val pc = floor((point.x - minX) / cell).toInt()
        val pr = floor((point.y - minY) / cell).toInt()
        val maxRing = max(columns, rows) + max(0, max(-pc, pc - columns)) + max(0, max(-pr, pr - rows)) + 1
        for (ring in 0..maxRing) {
            if (stopAt(max(0, ring - 1) * cell)) return
            for (row in pr - ring..pr + ring) {
                if (row < 0 || row >= rows) continue
                val edge = row == pr - ring || row == pr + ring
                var column = pc - ring
                while (column <= pc + ring) {
                    if (column in 0 until columns) {
                        val c = row * columns + column
                        for (s in cellStart[c] until cellStart[c + 1]) action(segmentRoad[s], segmentIndex[s])
                    }
                    // Inside the ring only its first and last cells of a row are on it
                    column += if (edge || ring == 0) 1 else 2 * ring
                }
            }
        }
    }
}

/** A binary heap of (key, node), the smallest key first; a node may be in it several times. */
internal class MinHeap(capacity: Int) {
    private var keys = DoubleArray(max(capacity, 16))
    private var nodes = IntArray(max(capacity, 16))
    private var size = 0

    fun clear() {
        size = 0
    }

    fun isNotEmpty(): Boolean = size > 0

    fun push(key: Double, node: Int) {
        if (size == keys.size) {
            keys = keys.copyOf(size * 2)
            nodes = nodes.copyOf(size * 2)
        }
        var i = size++
        while (i > 0) {
            val parent = (i - 1) / 2
            if (keys[parent] <= key) break
            keys[i] = keys[parent]
            nodes[i] = nodes[parent]
            i = parent
        }
        keys[i] = key
        nodes[i] = node
    }

    fun popNode(): Int {
        val top = nodes[0]
        size--
        if (size > 0) {
            val key = keys[size]
            val node = nodes[size]
            var i = 0
            while (true) {
                var child = 2 * i + 1
                if (child >= size) break
                if (child + 1 < size && keys[child + 1] < keys[child]) child++
                if (keys[child] >= key) break
                keys[i] = keys[child]
                nodes[i] = nodes[child]
                i = child
            }
            keys[i] = key
            nodes[i] = node
        }
        return top
    }
}
