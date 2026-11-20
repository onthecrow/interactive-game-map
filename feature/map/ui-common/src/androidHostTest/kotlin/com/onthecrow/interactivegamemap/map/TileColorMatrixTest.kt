package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileColorMatrixTest {
    private val paper = Color(0xFFD3B790)
    private val ink = Color(0xFF3A3530)
    private val darkPaper = Color(0xFF34312D)
    private val paleInk = Color(0xFFBEB7AC)
    private val dark = TileColorMatrix.byBrightness(light = paper to darkPaper, dark = ink to paleInk)

    @Test
    fun `the paper and the ink get their colors`() {
        assertClose(darkPaper, dark.apply(paper))
        assertClose(paleInk, dark.apply(ink))
    }

    @Test
    fun `a black disc turns light and a light pictogram dark`() {
        val disc = dark.apply(Color(0xFF070605))
        val pictogram = dark.apply(Color(0xFFE2DFD8))

        assertTrue(disc.red > 0.85f && disc.green > 0.85f && disc.blue > 0.8f, "$disc")
        assertTrue(pictogram.red < 0.1f && pictogram.green < 0.1f && pictogram.blue < 0.1f, "$pictogram")
    }

    @Test
    fun `alpha is kept`() {
        assertEquals(0.5f, dark.apply(paper.copy(alpha = 0.5f)).alpha, 1 / 255f)
    }

    @Test
    fun `the identity leaves colors as they are`() {
        val identity = TileColorMatrix(
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        )

        assertClose(paper, identity.apply(paper))
    }

    @Test
    fun `matrices with the same values are equal`() {
        assertEquals(dark, TileColorMatrix.byBrightness(light = paper to darkPaper, dark = ink to paleInk))
        assertEquals(dark.hashCode(), TileColorMatrix(dark.values.copyOf()).hashCode())
    }

    @Test
    fun `the dark map recolours the background like the tiles`() {
        val style = MapStyle(backgroundColor = paper, routeWidth = 8.dp, darkTiles = dark)

        assertTrue(style.hasDarkMap)
        assertEquals(dark, style.tileColors(isDark = true))
        assertNull(style.tileColors(isDark = false))
        assertClose(darkPaper, style.backgroundColor(isDark = true))
        assertEquals(paper, style.backgroundColor(isDark = false))
    }

    @Test
    fun `a game without a dark map draws it as it is`() {
        val style = MapStyle(backgroundColor = Color.Black)

        assertFalse(style.hasDarkMap)
        assertNull(style.tileColors(isDark = true))
        assertEquals(Color.Black, style.backgroundColor(isDark = true))
    }

    private fun assertClose(expected: Color, actual: Color) {
        val channels = listOf(
            expected.red to actual.red,
            expected.green to actual.green,
            expected.blue to actual.blue,
            expected.alpha to actual.alpha,
        )
        assertTrue(channels.all { (e, a) -> abs(e - a) <= 1.5f / 255 }, "expected $expected, got $actual")
    }
}
