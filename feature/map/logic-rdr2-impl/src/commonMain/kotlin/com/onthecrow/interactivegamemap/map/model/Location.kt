package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
internal data class Location(
    @SerialName("id")
    val id: Int,
    @SerialName("map_id")
    val mapId: Int,
    @SerialName("category_id")
    val categoryId: Int,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String?,
    @SerialName("latitude")
    val lat: Double,
    @SerialName("longitude")
    val lng: Double,
    @SerialName("tags")
    val tags: List<Int> = emptyList(),
    @SerialName("interior_id")
    val interiorId: Int? = null,
)
