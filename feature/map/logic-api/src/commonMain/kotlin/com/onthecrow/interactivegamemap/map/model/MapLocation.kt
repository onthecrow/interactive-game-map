package com.onthecrow.interactivegamemap.map.model

data class MapLocation(
    val id: Int,
    val mapId: Int,
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
)
