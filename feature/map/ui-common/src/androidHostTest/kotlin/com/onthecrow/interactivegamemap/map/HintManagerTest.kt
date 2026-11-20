package com.onthecrow.interactivegamemap.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class HintManagerTest {
    // The hint on the screen, the ones whose buttons are there, and the buttons the user has used
    private data class Screen(
        val hint: String? = null,
        val buttons: Set<String> = emptySet(),
        val known: Set<String> = emptySet(),
    )

    private val screen = MutableStateFlow(Screen())
    private val shown = mutableSetOf<String>()

    // The route goes first when both could, though it's second in the list: its priority is higher
    private val manager = HintManager(
        hints = listOf("recenter", "route"),
        priority = { if (it == "route") 2 else 1 },
        isShown = { it in shown },
        setShown = { shown += it },
        settle = 1.seconds,
        pause = 10.seconds,
    )

    private fun TestScope.start() = launch {
        manager.run(
            states = screen,
            current = { it.hint },
            canShow = { state, hint -> hint in state.buttons },
            isKnown = { state, hint -> hint in state.known },
            show = { hint -> screen.value = screen.value.copy(hint = hint) },
        )
    }

    @Test
    fun `a hint doesn't wait for one whose button isn't there`() = runTest {
        screen.value = Screen(buttons = setOf("recenter"))
        val job = start()
        advanceTimeBy(900)
        assertNull(screen.value.hint)
        advanceTimeBy(200)
        assertEquals("recenter", screen.value.hint)
        assertEquals(setOf("recenter"), shown)

        // The route button comes later: its hint after the last is gone, the pause and a moment more
        screen.value = screen.value.copy(hint = null, buttons = setOf("route", "recenter"))
        advanceTimeBy(10_500)
        assertNull(screen.value.hint)
        advanceTimeBy(600)
        assertEquals("route", screen.value.hint)
        screen.value = screen.value.copy(hint = null)
        runCurrent()
        advanceTimeBy(20_000)
        // Every hint shown, none comes again
        assertFalse(job.isActive)
        assertEquals(setOf("route", "recenter"), shown)
    }

    @Test
    fun `of hints that could come together the higher priority goes first`() = runTest {
        screen.value = Screen(buttons = setOf("route", "recenter"))
        val job = start()
        advanceTimeBy(1_100)
        assertEquals("route", screen.value.hint)
        screen.value = screen.value.copy(hint = null)
        advanceTimeBy(11_100)
        assertEquals("recenter", screen.value.hint)
        job.cancel()
    }

    @Test
    fun `a hint of a higher priority that comes while the screen settles goes first`() = runTest {
        screen.value = Screen(buttons = setOf("recenter"))
        val job = start()
        advanceTimeBy(500)
        screen.value = screen.value.copy(buttons = setOf("route", "recenter"))
        advanceTimeBy(600)
        assertEquals("route", screen.value.hint)
        assertFalse("recenter" in shown)
        job.cancel()
    }

    @Test
    fun `a hint the user knows the button of counts as shown without showing`() = runTest {
        // A route is built: the route button is known, its hint never comes
        screen.value = Screen(buttons = setOf("route"), known = setOf("route"))
        val job = start()
        runCurrent()
        assertTrue("route" in shown)
        advanceTimeBy(5_000)
        assertNull(screen.value.hint)
        job.cancel()
    }

    @Test
    fun `a hint is shown only when it still makes sense after the screen settles`() = runTest {
        val job = start()
        screen.value = Screen(buttons = setOf("route"))
        advanceTimeBy(500)
        // Something comes over the map before the moment is up: no hint, and it starts over
        screen.value = Screen()
        advanceTimeBy(5_000)
        assertNull(screen.value.hint)
        assertTrue(shown.isEmpty())
        screen.value = Screen(buttons = setOf("route"))
        advanceTimeBy(1_100)
        assertEquals("route", screen.value.hint)
        job.cancel()
    }

    @Test
    fun `hints shown at a launch before don't come again`() = runTest {
        shown += "route"
        screen.value = Screen(buttons = setOf("route", "recenter"))
        val job = start()
        advanceTimeBy(1_100)
        assertEquals("recenter", screen.value.hint)
        job.cancel()
    }
}
