package com.onthecrow.interactivegamemap.map.model

data class MapLocation(
    val id: Int,
    val mapId: Int,
    val categoryId: String,
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val cycle: Int? = null,
    val dungeon: LocationDungeon? = null,
    val pin: MarkerPin? = null,
    /** The places around the location it is about, like where an animal lives, drawn when it is selected. */
    val areas: List<LocationArea> = emptyList(),
) {
    companion object {
        val EMPTY = MapLocation(
            id = -1,
            mapId = -1,
            categoryId = "",
            name = "",
            description = "",
            lat = 0.0,
            lng = 0.0,
        )
    }
}

/** A closed outline, the last point connects to the first one. */
data class LocationArea(
    val outline: List<GeoPoint>,
)

data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

data class LocationDungeon(
    val dungeonId: Int,
    val floorId: Int?,
    val entranceIds: List<Int>,
)
