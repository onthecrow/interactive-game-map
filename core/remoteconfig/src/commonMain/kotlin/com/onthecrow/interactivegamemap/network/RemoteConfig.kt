package com.onthecrow.interactivegamemap.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfig(
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

@Serializable
data class MapTiles(
    val urlTemplate: String,
    val scheme: TileScheme,
)

@Serializable
enum class TileScheme {
    @SerialName("centered")
    CENTERED,

    @SerialName("xyz")
    XYZ,
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
