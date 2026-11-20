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
 */
class MapStyle(
    val backgroundColor: Color,
    val areaColor: Color = Color(0xFFC62828),
    val routeColor: Color = areaColor,
    val routeWidth: Dp = 8.dp,
    val routeWidthZoom: Float = 8f,
)
