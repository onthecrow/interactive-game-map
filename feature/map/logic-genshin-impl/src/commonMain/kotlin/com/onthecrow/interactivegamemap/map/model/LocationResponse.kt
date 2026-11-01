package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

@Serializable
internal data class LocationResponse(
    val data: LocationColumns,
)

@Serializable
internal data class LocationColumns(
    val id: List<Int>,
    val type: List<String>,
    val mid: List<Int>,
    val lng: List<Double>,
    val lat: List<Double>,
    val meta: List<String>,
)
