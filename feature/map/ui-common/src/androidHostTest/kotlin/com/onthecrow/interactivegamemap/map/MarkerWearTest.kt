package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MarkerWearTest {
    @Test
    fun `an image is laid over a part of the texture as big as it`() {
        // 512 px over 128 dp: 4 px a dp
        val region = wearRegion(512, 512, 128, Size(33f, 44f), seed = 7)

        assertEquals(132, region.width)
        assertEquals(176, region.height)
        assertTrue(region.left >= 0 && region.top >= 0 && region.right <= 512 && region.bottom <= 512)
    }

    @Test
    fun `equal images wear alike, the others differ`() {
        val pin = Size(33f, 44f)

        assertEquals(wearRegion(512, 512, 128, pin, seed = 7), wearRegion(512, 512, 128, pin, seed = 7))
        assertNotEquals(wearRegion(512, 512, 128, pin, seed = 7), wearRegion(512, 512, 128, pin, seed = 8))
    }

    @Test
    fun `an image bigger than the texture is laid over all of it`() {
        assertEquals(IntRect(0, 0, 512, 512), wearRegion(512, 512, 128, Size(300f, 200f), seed = 1))
    }
}
