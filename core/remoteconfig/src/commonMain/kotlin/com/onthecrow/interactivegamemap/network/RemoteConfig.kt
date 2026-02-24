package com.onthecrow.interactivegamemap.network

data class RemoteConfig(
    val tilesUrl: String,
    val initialZoom: Int,
    val maxZoom: Int,
    val minZoom: Int,
    val startLat: Int,
    val startLng: Int,
)
