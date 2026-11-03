package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.Color

/**
 * @param backgroundColor is displayed where there are no tiles, it matches the edges of the tiles
 * @param areaColor the outline of the areas of the selected location, they are filled with it see-through
 */
class MapStyle(
    val backgroundColor: Color,
    val areaColor: Color = Color(0xFFC62828),
)
