package com.onthecrow.interactivegamemap.map.view

import kotlin.test.Test
import kotlin.test.assertEquals

class MergedTilePartsTest {

    @Test
    fun `parts cover the same part of the world as the tile`() {
        assertEquals(
            listOf(
                MergedTilePart(x = 8206, y = 8182, column = 0, row = 0),
                MergedTilePart(x = 8207, y = 8182, column = 1, row = 0),
                MergedTilePart(x = 8206, y = 8183, column = 0, row = 1),
                MergedTilePart(x = 8207, y = 8183, column = 1, row = 1),
            ),
            mergedTileParts(x = 4103, y = 4091),
        )
    }
}
