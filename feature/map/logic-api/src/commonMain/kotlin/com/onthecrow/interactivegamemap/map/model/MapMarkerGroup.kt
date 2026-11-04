package com.onthecrow.interactivegamemap.map.model

data class MapMarkerGroup(
    val id: String,
    val name: String,
    val description: String?,
    val categories: List<MapMarkerCategory>,
)

data class MapMarkerCategory(
    val category: MarkerCategory,
    val count: Int,
    val countByCycle: Map<Int, Int>,
)
