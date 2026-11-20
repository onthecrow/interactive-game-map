package com.onthecrow.interactivegamemap.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @param backgroundColor is displayed where there are no tiles, it matches the edges of the tiles
 * @param areaColor the outline of the areas of the selected location, they are filled with it see-through
 * @param routeColor the line of a route and its jumps to fast travel points
 * @param routeWidth the width of the route along the roads at [routeWidthZoom]: it grows and shrinks with the map,
 * like the roads drawn on its tiles, a bit wider than them so the road shows under its ragged edges
 * @param darkTiles recolours the tiles and the images of the floors for the dark map, null when the game has none:
 * its map is the same in the dark theme then, and the settings don't offer the choice
 */
class MapStyle(
    val backgroundColor: Color,
    val areaColor: Color = Color(0xFFC62828),
    val routeColor: Color = areaColor,
    val routeWidth: Dp = 8.dp,
    val routeWidthZoom: Float = 8f,
    val darkTiles: TileColorMatrix? = null,
) {

    val hasDarkMap: Boolean get() = darkTiles != null

    /** How the images of the map are recoloured, null when they are drawn as they are. */
    fun tileColors(isDark: Boolean): TileColorMatrix? = darkTiles?.takeIf { isDark }

    /** The background recoloured like the tiles, so it still matches their edges. */
    fun backgroundColor(isDark: Boolean): Color = tileColors(isDark)?.apply(backgroundColor) ?: backgroundColor
}
