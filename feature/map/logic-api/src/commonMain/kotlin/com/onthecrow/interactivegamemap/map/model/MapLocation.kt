package com.onthecrow.interactivegamemap.map.model

data class MapLocation(
    val id: Int,
    val mapId: Int,
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
) {
    companion object {
        val EMPTY = MapLocation(
            id = -1,
            mapId = -1,
            name = "",
            description = "",
            lat = 0.0,
            lng = 0.0,
        )
    }
}
