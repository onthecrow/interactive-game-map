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

data class LocationDungeon(
    val dungeonId: Int,
    val floorId: Int?,
    val entranceIds: List<Int>,
)
