package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

@Serializable(with = LocationSerializer::class)
data class Location(
    val id: Int,
    val type: String,
    val mapId: Int,
    val level: Int,
    val lat: Double,
    val lng: Double,
)
