package com.onthecrow.interactivegamemap.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlsLayoutTest {
    // In dp: buttons of 48, gaps of 8, the zoom 100 tall, three side buttons over the bar
    private fun position(screenHeight: Int, top: Int, bottom: Int): Pair<Int, Int> {
        val barTop = bottom - 48
        val sideTop = barTop - 3 * (8 + 48)
        return zoomPosition(screenHeight, right = 400, top = top, barTop = barTop, sideTop = sideTop, sideWidth = 48, gap = 8, width = 48, height = 100)
    }

    @Test
    fun `on a tall screen the zoom is over the side buttons in the middle`() {
        // A phone upright: 800 tall
        val (x, y) = position(screenHeight = 800, top = 60, bottom = 760)
        assertEquals(352, x)
        assertEquals(350, y)
    }

    @Test
    fun `on a short screen the zoom goes beside the side buttons, not over them`() {
        // The OnePlus on its side: about 362 tall, the status bar 24 and the margins of 16
        val top = 24 + 16
        val bottom = 362 - 13 - 16
        val (x, y) = position(screenHeight = 362, top = top, bottom = bottom)
        assertEquals(400 - 48 - 8 - 48, x)
        assertTrue(y >= top && y + 100 <= bottom - 48 - 8, "$y")
    }
}
