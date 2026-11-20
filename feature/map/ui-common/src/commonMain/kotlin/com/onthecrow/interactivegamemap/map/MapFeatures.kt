package com.onthecrow.interactivegamemap.map

/**
 * What an app offers on its map beyond the markers, by its game. The logic is the same in every app; a game without
 * the data of a feature keeps it off here, and its data module answers with a stub.
 *
 * @param routes the route through the markers on the map, along the roads and with fast travel
 */
class MapFeatures(
    val routes: Boolean = false,
)
