package com.onthecrow.interactivegamemap.map.route

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeSource

/** What a tour costs: the way to its first node, and from node to node; it ends at its last node. */
internal interface TourCosts {
    val size: Int

    /** From the start, before the first node. */
    fun start(j: Int): Double

    /** From [i] to [j], not always the same as from [j] to [i]. */
    fun cost(i: Int, j: Int): Double
}

// Up to this many nodes every order is weighed, the result is the shortest one
private const val EXACT_LIMIT = 12
private const val NEIGHBORS = 10
private const val SMALL_TOUR_NEIGHBORS = 16
private const val SMALL_TOUR = 200
// The nodes nearest to the start: a jump there is worth trying after any node
private const val START_NEIGHBORS = 4
// Or-opt moves segments of up to this many nodes
private const val MAX_SEGMENT = 3
// A kick swaps two neighbouring stretches of the tour, each of up to this many nodes
private const val MAX_KICK = 40
private const val MIN_IDLE_KICKS = 1_000
private const val IDLE_KICKS_PER_NODE = 30
private const val CHECK_EVERY = 256

private const val START = -1
private const val END = -2

/**
 * The shortest order of the nodes of [costs], a path from the start to the last node. Every order is weighed for a
 * few nodes; for more, a tour of nearest nodes is shortened by 2-opt and Or-opt moves among the nearest nodes of each,
 * then kicked and shortened again for [improvementBudget] or until kicks stop helping (iterated local search).
 *
 * @param xs where the nodes are, to find the nearest ones
 * @param ensureActive throws when the work is cancelled, it's called every now and then
 * @param onOrderProgress how far the first order is, from 0 to 1
 * @param onImproveProgress how much of [improvementBudget] has gone, from 0 to 1
 */
internal class TourSolver(
    private val costs: TourCosts,
    private val xs: DoubleArray,
    private val ys: DoubleArray,
    private val improvementBudget: Duration,
    private val random: Random = Random(costs.size),
    private val ensureActive: () -> Unit = {},
    private val onOrderProgress: (Float) -> Unit = {},
    private val onImproveProgress: (Float) -> Unit = {},
    private val exactLimit: Int = EXACT_LIMIT,
) {
    private val n = costs.size

    fun solve(): IntArray {
        if (n == 0) return IntArray(0)
        if (n == 1) return intArrayOf(0)
        if (n <= exactLimit) return exact()
        val neighbors = neighbors()
        val tour = Tour(nearestNeighborOrder())
        // The first pass looks at every node at least once, and again at the ones next to each change
        tour.improve(neighbors, (0 until n).toList()) { looked ->
            onOrderProgress(0.5f + 0.5f * (looked.toFloat() / (FIRST_PASS_LOOKS * n)).coerceAtMost(1f))
        }
        onOrderProgress(1f)
        iteratedLocalSearch(tour, neighbors)
        return tour.order
    }

    fun cost(order: IntArray): Double {
        if (order.isEmpty()) return 0.0
        var total = costs.start(order[0])
        for (k in 0 until order.size - 1) total += costs.cost(order[k], order[k + 1])
        return total
    }

    /** Held-Karp over the subsets of the nodes. */
    private fun exact(): IntArray {
        val full = (1 shl n) - 1
        val best = DoubleArray((full + 1) * n) { Double.POSITIVE_INFINITY }
        val previous = IntArray((full + 1) * n) { -1 }
        for (j in 0 until n) best[(1 shl j) * n + j] = costs.start(j)
        for (mask in 1..full) {
            if (mask % CHECK_EVERY == 0) ensureActive()
            for (j in 0 until n) {
                val here = best[mask * n + j]
                if (here.isInfinite() || mask and (1 shl j) == 0) continue
                for (k in 0 until n) {
                    if (mask and (1 shl k) != 0) continue
                    val next = mask or (1 shl k)
                    val cost = here + costs.cost(j, k)
                    if (cost < best[next * n + k]) {
                        best[next * n + k] = cost
                        previous[next * n + k] = j
                    }
                }
            }
        }
        var last = (0 until n).minBy { best[full * n + it] }
        val order = IntArray(n)
        var mask = full
        for (k in n - 1 downTo 0) {
            order[k] = last
            val before = previous[mask * n + last]
            mask = mask xor (1 shl last)
            last = before
        }
        return order
    }

    /** The nearest nodes of each node by distance, and the nodes cheapest to start at, without itself. */
    private fun neighbors(): Array<IntArray> {
        val count = min(n - 1, if (n <= SMALL_TOUR) SMALL_TOUR_NEIGHBORS else NEIGHBORS)
        val grid = PointGrid(xs, ys)
        val startNodes = (0 until n).sortedBy { costs.start(it) }.take(START_NEIGHBORS)
        return Array(n) { i ->
            if (i % CHECK_EVERY == 0) ensureActive()
            val nearest = grid.nearest(i, count) { true }
            (nearest + startNodes.filter { it != i && it !in nearest }).toIntArray()
        }
    }

    /** From the start to the cheapest node, then each time to the cheaper of the nearest nodes and the best jump. */
    private fun nearestNeighborOrder(): IntArray {
        val grid = PointGrid(xs, ys)
        val visited = BooleanArray(n)
        val byStart = (0 until n).sortedBy { costs.start(it) }.toIntArray()
        var startPointer = 0
        val order = IntArray(n)
        var current = START
        for (step in 0 until n) {
            if (step % CHECK_EVERY == 0) {
                ensureActive()
                onOrderProgress(step.toFloat() / n * 0.5f)
            }
            while (visited[byStart[startPointer]]) startPointer++
            var best = byStart[startPointer]
            var bestCost = if (current == START) costs.start(best) else costs.cost(current, best)
            if (current != START) {
                for (j in grid.nearest(current, NEAREST_CANDIDATES) { !visited[it] }) {
                    val cost = costs.cost(current, j)
                    if (cost < bestCost) {
                        best = j
                        bestCost = cost
                    }
                }
            }
            visited[best] = true
            grid.remove(best)
            order[step] = best
            current = best
        }
        return order
    }

    private fun iteratedLocalSearch(tour: Tour, neighbors: Array<IntArray>) {
        val clock = TimeSource.Monotonic.markNow()
        val best = tour.snapshot()
        var idle = 0
        val idleLimit = max(MIN_IDLE_KICKS, IDLE_KICKS_PER_NODE * n)
        var kicks = 0
        while (idle < idleLimit) {
            val elapsed = clock.elapsedNow()
            if (elapsed >= improvementBudget) break
            if (kicks % 16 == 0) {
                ensureActive()
                onImproveProgress((elapsed / improvementBudget).toFloat().coerceIn(0f, 1f))
            }
            kicks++
            val touched = tour.kick()
            tour.improve(neighbors, touched)
            if (tour.total < best.total - tour.epsilon) {
                tour.saveTo(best)
                idle = 0
            } else {
                tour.restore(best)
                idle++
            }
        }
        onImproveProgress(1f)
    }

    /**
     * An order of the nodes with the cost of each step both ways, and their sums from the first step, so that the
     * cost of a stretch read backwards is known at once.
     */
    private inner class Tour(initial: IntArray) {
        val order = initial.copyOf()
        private val position = IntArray(n).also { p -> order.forEachIndexed { k, node -> p[node] = k } }
        // forward[k] is the cost from order[k] to order[k + 1], backward[k] the one from order[k + 1] to order[k]
        private val forward = DoubleArray(n - 1) { costs.cost(order[it], order[it + 1]) }
        private val backward = DoubleArray(n - 1) { costs.cost(order[it + 1], order[it]) }
        private val forwardSum = DoubleArray(n)
        private val backwardSum = DoubleArray(n)
        var total = 0.0
            private set
        val epsilon: Double

        init {
            updateSums()
            epsilon = max(total, 1e-12) * 1e-12
        }

        private fun updateSums() {
            for (k in 1 until n) {
                forwardSum[k] = forwardSum[k - 1] + forward[k - 1]
                backwardSum[k] = backwardSum[k - 1] + backward[k - 1]
            }
            total = costs.start(order[0]) + forwardSum[n - 1]
        }

        private fun at(k: Int): Int = when {
            k < 0 -> START
            k >= n -> END
            else -> order[k]
        }

        private fun c(i: Int, j: Int): Double = when {
            j == END -> 0.0
            i == START -> costs.start(j)
            else -> costs.cost(i, j)
        }

        // The cost of the step from position k to k + 1, k from -1 (the start) to n - 1 (the end)
        private fun step(k: Int): Double = when {
            k < 0 -> costs.start(order[0])
            k >= n - 1 -> 0.0
            else -> forward[k]
        }

        /** How much shorter the tour gets when the positions [a]..[b] are read backwards. */
        private fun reversalGain(a: Int, b: Int): Double {
            val before = step(a - 1) + step(b) + forwardSum[b] - forwardSum[a]
            val after = c(at(a - 1), order[b]) + c(order[a], at(b + 1)) + backwardSum[b] - backwardSum[a]
            return before - after
        }

        /** How much shorter it gets when [a]..[b] goes between the positions [p] and [p] + 1, read backwards or not. */
        private fun moveGain(a: Int, b: Int, p: Int, reversed: Boolean): Double {
            val removal = step(a - 1) + step(b) - c(at(a - 1), at(b + 1))
            val x = at(p)
            val y = at(p + 1)
            val insertion = if (reversed) {
                c(x, order[b]) + c(order[a], y) + backwardSum[b] - backwardSum[a] - (forwardSum[b] - forwardSum[a])
            } else {
                c(x, order[a]) + c(order[b], y)
            } - step(p)
            return removal - insertion
        }

        private fun reverse(a: Int, b: Int) {
            if (a >= b) return
            var i = a
            var j = b
            while (i < j) {
                val t = order[i]; order[i] = order[j]; order[j] = t
                position[order[i]] = i
                position[order[j]] = j
                i++; j--
            }
            // The steps inside turn around: forward ones become backward ones in the opposite order
            i = a
            j = b - 1
            while (i < j) {
                val f = forward[i]; forward[i] = forward[j]; forward[j] = f
                val r = backward[i]; backward[i] = backward[j]; backward[j] = r
                i++; j--
            }
            for (k in a until b) {
                val f = forward[k]; forward[k] = backward[k]; backward[k] = f
            }
            if (a > 0) setStep(a - 1)
            if (b < n - 1) setStep(b)
        }

        private fun setStep(k: Int) {
            forward[k] = costs.cost(order[k], order[k + 1])
            backward[k] = costs.cost(order[k + 1], order[k])
        }

        private fun move(a: Int, b: Int, p: Int, reversed: Boolean) {
            val length = b - a + 1
            if (p > b) {
                val other = p - b
                reverse(a, p)
                reverse(a, a + other - 1)
                if (!reversed) reverse(a + other, p)
            } else {
                reverse(p + 1, b)
                reverse(p + 1 + length, b)
                if (!reversed) reverse(p + 1, p + length)
            }
        }

        /**
         * 2-opt and Or-opt moves among [neighbors] while they shorten the tour, starting at the nodes [from];
         * [onLooked] gets how many nodes have been looked at so far, now and then.
         */
        fun improve(neighbors: Array<IntArray>, from: List<Int>, onLooked: (Int) -> Unit = {}) {
            val queue = ArrayDeque(from)
            val queued = BooleanArray(n)
            from.forEach { queued[it] = true }
            var checks = 0
            while (queue.isNotEmpty()) {
                if (++checks % CHECK_EVERY == 0) {
                    ensureActive()
                    onLooked(checks)
                }
                val u = queue.removeFirst()
                queued[u] = false
                val touched = improveAt(u, neighbors[u]) ?: continue
                updateSums()
                for (node in touched + u) {
                    if (node in 0 until n && !queued[node]) {
                        queued[node] = true
                        queue.addLast(node)
                    }
                }
            }
        }

        /** Makes the best move around [u], the nodes next to what changed, or null when no move shortens the tour. */
        private fun improveAt(u: Int, neighbors: IntArray): List<Int>? {
            val i = position[u]
            var bestGain = epsilon
            var kind = 0
            var p1 = 0
            var p2 = 0
            var p3 = 0
            var reversedMove = false

            fun reversal(a: Int, b: Int) {
                if (a < 0 || b >= n || a >= b) return
                val gain = reversalGain(a, b)
                if (gain > bestGain) {
                    bestGain = gain; kind = 1; p1 = a; p2 = b
                }
            }

            fun segmentMove(a: Int, b: Int, p: Int) {
                if (a < 0 || b >= n || p < -1 || p >= n || p in a - 1..b) return
                for (reversed in booleanArrayOf(false, true)) {
                    if (reversed && a == b) continue
                    val gain = moveGain(a, b, p, reversed)
                    if (gain > bestGain) {
                        bestGain = gain; kind = 2; p1 = a; p2 = b; p3 = p; reversedMove = reversed
                    }
                }
            }

            // The start goes straight to u
            reversal(0, i)
            for (v in neighbors) {
                val j = position[v]
                if (j > i) {
                    reversal(i + 1, j) // u -> v
                    reversal(i, j - 1) // u -> v from the other side
                } else {
                    reversal(j + 1, i) // v -> u
                    reversal(j, i - 1) // v -> u from the other side
                }
                for (length in 1..MAX_SEGMENT) {
                    // A stretch starting or ending at u, put after v or before it
                    for (a in intArrayOf(i, i - length + 1)) {
                        val b = a + length - 1
                        segmentMove(a, b, j)
                        segmentMove(a, b, j - 1)
                    }
                }
            }
            return when (kind) {
                1 -> {
                    val touched = listOf(at(p1 - 1), order[p1], order[p2], at(p2 + 1))
                    reverse(p1, p2)
                    touched
                }
                2 -> {
                    val touched = listOf(at(p1 - 1), order[p1], order[p2], at(p2 + 1), at(p3), at(p3 + 1))
                    move(p1, p2, p3, reversedMove)
                    touched
                }
                else -> null
            }
        }

        /** Swaps two neighbouring stretches somewhere, the nodes at the ends of the stretches. */
        fun kick(): List<Int> {
            val first = 1 + random.nextInt(min(MAX_KICK, max(1, n / 3)))
            val second = 1 + random.nextInt(min(MAX_KICK, max(1, n / 3)))
            val a = random.nextInt(max(1, n - first - second + 1))
            val b = min(n - 1, a + first - 1)
            val p = min(n - 1, b + second)
            if (p <= b) return emptyList()
            val touched = listOf(at(a - 1), order[a], order[b], at(b + 1), order[p], at(p + 1))
            move(a, b, p, reversed = false)
            updateSums()
            return touched.filter { it >= 0 }
        }

        fun snapshot(): Snapshot = Snapshot(order.copyOf(), position.copyOf(), forward.copyOf(), backward.copyOf(), total)

        fun saveTo(snapshot: Snapshot) {
            order.copyInto(snapshot.order)
            position.copyInto(snapshot.position)
            forward.copyInto(snapshot.forward)
            backward.copyInto(snapshot.backward)
            snapshot.total = total
        }

        fun restore(snapshot: Snapshot) {
            snapshot.order.copyInto(order)
            snapshot.position.copyInto(position)
            snapshot.forward.copyInto(forward)
            snapshot.backward.copyInto(backward)
            updateSums()
        }
    }

    private class Snapshot(
        val order: IntArray,
        val position: IntArray,
        val forward: DoubleArray,
        val backward: DoubleArray,
        var total: Double,
    )

    private companion object {
        const val NEAREST_CANDIDATES = 8
        // About how many times the first pass looks at each node, to show its progress
        const val FIRST_PASS_LOOKS = 4
    }
}

/** The points in square cells, to find the nearest ones to a point; points can be taken out. */
private class PointGrid(private val xs: DoubleArray, private val ys: DoubleArray) {
    private val minX = xs.min()
    private val minY = ys.min()
    private val cell: Double
    private val columns: Int
    private val rows: Int
    private val cells: Array<IntArray>
    private val counts: IntArray
    // Where each point is in its cell
    private val slot = IntArray(xs.size)
    private val cellOf = IntArray(xs.size)

    init {
        val width = max(xs.max() - minX, 1e-12)
        val height = max(ys.max() - minY, 1e-12)
        // About two points a cell
        cell = max(sqrt(width * height / max(1, xs.size / 2)), 1e-12)
        columns = max(1, ceil(width / cell).toInt() + 1)
        rows = max(1, ceil(height / cell).toInt() + 1)
        counts = IntArray(columns * rows)
        for (i in xs.indices) counts[cellIndex(i)]++
        cells = Array(columns * rows) { IntArray(counts[it]) }
        counts.fill(0)
        for (i in xs.indices) {
            val c = cellIndex(i)
            cellOf[i] = c
            slot[i] = counts[c]
            cells[c][counts[c]++] = i
        }
    }

    private fun cellIndex(i: Int): Int {
        val column = floor((xs[i] - minX) / cell).toInt().coerceIn(0, columns - 1)
        val row = floor((ys[i] - minY) / cell).toInt().coerceIn(0, rows - 1)
        return row * columns + column
    }

    fun remove(i: Int) {
        val c = cellOf[i]
        val last = cells[c][--counts[c]]
        cells[c][slot[i]] = last
        slot[last] = slot[i]
    }

    /** Up to [count] points nearest to the point [i] that pass [accept], the nearest first, without [i]. */
    inline fun nearest(i: Int, count: Int, accept: (Int) -> Boolean): List<Int> {
        val pc = floor((xs[i] - minX) / cell).toInt().coerceIn(0, columns - 1)
        val pr = floor((ys[i] - minY) / cell).toInt().coerceIn(0, rows - 1)
        val found = ArrayList<Pair<Double, Int>>()
        var ring = 0
        val maxRing = max(columns, rows)
        while (ring <= maxRing) {
            // A ring further than the farthest of enough found points can't have nearer ones
            if (found.size >= count) {
                found.sortBy { it.first }
                if ((ring - 1) * cell > found[count - 1].first) break
            }
            for (row in pr - ring..pr + ring) {
                if (row < 0 || row >= rows) continue
                val edge = row == pr - ring || row == pr + ring
                var column = pc - ring
                while (column <= pc + ring) {
                    if (column in 0 until columns) {
                        val c = row * columns + column
                        for (s in 0 until counts[c]) {
                            val j = cells[c][s]
                            if (j != i && accept(j)) {
                                val dx = xs[j] - xs[i]
                                val dy = ys[j] - ys[i]
                                found += sqrt(dx * dx + dy * dy) to j
                            }
                        }
                    }
                    column += if (edge || ring == 0) 1 else 2 * ring
                }
            }
            ring++
        }
        found.sortBy { it.first }
        return found.take(count).map { it.second }
    }
}
