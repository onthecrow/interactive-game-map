package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

/** The maps of a game, the one opened first is [defaultMapId]. */
@Serializable
data class MapsConfig(
    val defaultMapId: Int,
    val maps: List<GameMap>,
)

@Serializable
data class GameMap(
    val id: Int,
    val name: String,
    val iconUrl: String? = null,
    val minZoom: Int,
    val maxZoom: Int,
    val tiles: MapTiles,
    val camera: MapCamera,
    val bounds: MapBounds? = null,
    val dungeonBaseOpacity: Float,
    val dungeons: List<MapDungeon> = emptyList(),
)

/**
 * Where the tiles of a map are and how their grid lies on the world.
 *
 * @param urlTemplate the url of a tile with the tokens of its [scheme], like `{{x}}`, `{{y}}`, `{{z}}`
 * @param scheme the name of the [com.onthecrow.interactivegamemap.map.tiles.TileUrlResolver] which places the tiles
 * @param zoomOffset [TileSchemes.XYZ] only: the tiles of zoom z are drawn at zoom z + [zoomOffset], in the middle of
 * the world. A map that fills its whole tile grid spans most of the longitudes, and the camera kept in the bounds of
 * such a map jumps to its other side across the antimeridian. The map's zooms in the config are the drawn ones, and
 * the minimal one is above [zoomOffset] so that the map lies on whole tiles.
 * @param params what a scheme of a game needs besides these, like the size of its grid
 */
@Serializable
data class MapTiles(
    val urlTemplate: String,
    val scheme: String,
    val zoomOffset: Int = 0,
    val params: Map<String, String> = emptyMap(),
)

/** The schemes every app knows, a game adds its own ones. */
object TileSchemes {
    /** The tiles around the middle of the world, the y of a tile counted up from it. */
    const val CENTERED = "centered"

    /** The usual web tiles, see [MapTiles.zoomOffset]. */
    const val XYZ = "xyz"
}

@Serializable
data class MapCamera(
    val lat: Double,
    val lng: Double,
    val zoom: Int,
)

@Serializable
data class MapBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
)

@Serializable
data class MapDungeon(
    val id: Int,
    val floors: List<MapFloor> = emptyList(),
)

@Serializable
data class MapFloor(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val bounds: MapBounds,
)
