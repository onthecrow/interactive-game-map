package com.onthecrow.interactivegamemap.ui.components

import com.onthecrow.interactivegamemap.ui.resources.Res
import com.onthecrow.interactivegamemap.ui.resources.content_updated
import com.onthecrow.interactivegamemap.ui.resources.content_updating
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnackbarsTest {
    private val updated = GameSnackbar(Res.string.content_updated)

    private fun TestScope.ending(snackbar: ShownSnackbar): () -> Boolean {
        var ended = false
        launch {
            snackbar.awaitEnd()
            ended = true
        }
        runCurrent()
        return { ended }
    }

    @Test
    fun `snackbars come one at a time in their order, the same one twice too`() {
        val snackbars = GameSnackbars()
        val updating = GameSnackbar(Res.string.content_updating)
        snackbars.show(updated)
        snackbars.show(updating)
        snackbars.show(updated)
        assertEquals(listOf(updated, updating, updated), snackbars.queue.value.map { it.snackbar })
        snackbars.remove(snackbars.queue.value.first())
        assertEquals(listOf(updating, updated), snackbars.queue.value.map { it.snackbar })
    }

    @Test
    fun `a snackbar stays for its time`() = runTest {
        val ended = ending(ShownSnackbar(GameSnackbars.Entry(updated)))
        advanceTimeBy(SnackbarDuration.inWholeMilliseconds - 100)
        assertFalse(ended())
        advanceTimeBy(200)
        assertTrue(ended())
    }

    @Test
    fun `a held snackbar stays, let go its time starts again`() = runTest {
        val snackbar = ShownSnackbar(GameSnackbars.Entry(updated))
        val ended = ending(snackbar)
        advanceTimeBy(2_000)
        snackbar.isHeld.value = true
        advanceTimeBy(10_000)
        assertFalse(ended())
        snackbar.isHeld.value = false
        advanceTimeBy(SnackbarDuration.inWholeMilliseconds - 100)
        assertFalse(ended())
        advanceTimeBy(200)
        assertTrue(ended())
    }

    @Test
    fun `a swipe takes it away at once, held or not`() = runTest {
        val snackbar = ShownSnackbar(GameSnackbars.Entry(updated))
        val ended = ending(snackbar)
        snackbar.isHeld.value = true
        runCurrent()
        snackbar.isSwipedAway.value = true
        runCurrent()
        assertTrue(ended())
    }
}
