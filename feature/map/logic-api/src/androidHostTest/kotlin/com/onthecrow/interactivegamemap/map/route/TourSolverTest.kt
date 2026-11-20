package com.onthecrow.interactivegamemap.map.route

import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class TourSolverTest {

    /** Points on a plane with a start point and a jump to near the start from anywhere, so costs aren't symmetric. */
    private class PlaneCosts(val xs: DoubleArray, val ys: DoubleArray, private val jump: Double) : TourCosts {
        override val size = xs.size
        override fun start(j: Int) = hypot(xs[j], ys[j])
        override fun cost(i: Int, j: Int) = min(hypot(xs[i] - xs[j], ys[i] - ys[j]), jump + start(j))
    }

    private fun randomCosts(n: Int, seed: Int, jump: Double = 0.3): PlaneCosts {
        val random = Random(seed)
        return PlaneCosts(DoubleArray(n) { random.nextDouble() }, DoubleArray(n) { random.nextDouble() }, jump)
    }

    private fun solver(costs: PlaneCosts, exactLimit: Int = 12, budgetMillis: Int = 300) = TourSolver(
        costs = costs,
        xs = costs.xs,
        ys = costs.ys,
        improvementBudget = budgetMillis.milliseconds,
        exactLimit = exactLimit,
    )

    private fun permutations(items: List<Int>): Sequence<List<Int>> = sequence {
        if (items.size <= 1) {
            yield(items)
            return@sequence
        }
        for (item in items) {
            for (rest in permutations(items - item)) yield(listOf(item) + rest)
        }
    }

    @Test
    fun `the exact order is the shortest of all orders`() {
        repeat(5) { seed ->
            val costs = randomCosts(7, seed)
            val solver = solver(costs)
            val shortest = permutations((0 until 7).toList()).minOf { solver.cost(it.toIntArray()) }
            assertEquals(shortest, solver.cost(solver.solve()), 1e-9)
        }
    }

    @Test
    fun `the search finds the shortest order of small tours`() {
        var optimal = 0
        repeat(20) { seed ->
            val costs = randomCosts(12, 100 + seed)
            val exact = solver(costs).let { it.cost(it.solve()) }
            val searched = solver(costs, exactLimit = 0).let { it.cost(it.solve()) }
            assertTrue(searched <= exact * 1.03 + 1e-9, "seed $seed: $searched vs $exact")
            if (searched <= exact + 1e-9) optimal++
        }
        assertTrue(optimal >= 16, "optimal in $optimal of 20")
    }

    @Test
    fun `every node is in the order once`() {
        val costs = randomCosts(2_000, 7)
        val order = solver(costs, budgetMillis = 200).solve()
        assertContentEquals((0 until 2_000).toList(), order.sorted())
    }

    @Test
    fun `the search shortens the first order a lot`() {
        val costs = randomCosts(500, 11, jump = 10.0)
        val order = solver(costs, budgetMillis = 500).solve()
        val solver = solver(costs)
        // The in-order tour of random points is about n/3 long, a good one about 0.7 sqrt(n)
        val identity = solver.cost(IntArray(500) { it })
        assertTrue(solver.cost(order) < identity / 8, "${solver.cost(order)} vs $identity")
    }

    @Test
    fun `a cancelled search stops`() {
        val costs = randomCosts(3_000, 3)
        var calls = 0
        val solver = TourSolver(
            costs = costs,
            xs = costs.xs,
            ys = costs.ys,
            improvementBudget = 10_000.milliseconds,
            ensureActive = { if (++calls > 20) throw IllegalStateException("cancelled") },
        )
        assertFailsWith<IllegalStateException> { solver.solve() }
    }
}
