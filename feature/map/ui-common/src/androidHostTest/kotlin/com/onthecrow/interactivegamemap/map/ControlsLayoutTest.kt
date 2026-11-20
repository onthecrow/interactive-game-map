package com.onthecrow.interactivegamemap.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlsLayoutTest {
    // In dp: buttons of 48, gaps of 8, the zoom 100 tall, three side buttons over the bar
    private fun placement(screenHeight: Int, top: Int, bottom: Int, below: Int = 0): ZoomPlacement {
        val barTop = bottom - 48
        val sideTop = barTop - 3 * (8 + 48)
        return zoomPlacement(screenHeight, right = 400, top = top, barTop = barTop, sideTop = sideTop, gap = 8, width = 48, height = 100, below = below)
    }

    @Test
    fun `on a tall screen the zoom is over the side buttons in the middle`() {
        // A phone upright: 800 tall
        assertEquals(ZoomPlacement(x = 352, y = 350, sideRight = 400), placement(screenHeight = 800, top = 60, bottom = 760))
    }

    @Test
    fun `on a short screen the zoom goes to the right edge, the side buttons beside it`() {
        // The OnePlus on its side: about 362 tall, the status bar 24 and the margins of 16
        val top = 24 + 16
        val bottom = 362 - 13 - 16
        val placement = placement(screenHeight = 362, top = top, bottom = bottom, below = 56)
        assertEquals(400 - 48, placement.x)
        assertEquals(400 - 48 - 8, placement.sideRight)
        // The zoom and the recenter button under it are above the bar
        assertTrue(placement.y >= top && placement.y + 100 + 56 <= bottom - 48 - 8, "${placement.y}")
    }

    @Test
    fun `the room kept for the recenter button under the zoom keeps it clear of the side buttons`() {
        // The zoom is in the middle as long as the recenter button fits between it and the side buttons
        assertEquals(ZoomPlacement(x = 352, y = 350, sideRight = 400), placement(screenHeight = 800, top = 60, bottom = 760, below = 56))
        // A screen less tall: the zoom moves up so the recenter button is still above the side buttons
        val short = placement(screenHeight = 600, top = 60, bottom = 560, below = 56)
        val sideTop = 560 - 48 - 3 * (8 + 48)
        assertEquals(400, short.sideRight)
        assertTrue(short.y + 100 + 56 <= sideTop - 8, "${short.y}")
        assertTrue(short.y < (600 - 100) / 2)
    }
}
