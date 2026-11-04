package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

@Serializable
data class MarkerCatalog(
    val categories: Map<String, MarkerCategory>,
    val groupsByMap: Map<Int, List<MarkerGroup>>,
    /** The names of the maps by their ids where they differ from the remote config's ones. */
    val mapNames: Map<Int, String> = emptyMap(),
)

@Serializable
data class MarkerCategory(
    val id: String,
    val name: String,
    val icon: MarkerIcon,
    val pin: MarkerPin,
    val visibleByDefault: Boolean,
    val hasCycles: Boolean = false,
)

/** How the markers of a category look on the map, the size is in dp. */
@Serializable
data class MarkerPin(
    val icon: MarkerIcon,
    /** The image for the markers in dungeons, a badge is drawn over [icon] when there is none. */
    val dungeonIcon: MarkerIcon? = null,
    val width: Int,
    val height: Int,
)

/** A whole image, or a part of a sprite when the position and the size are set. */
@Serializable
data class MarkerIcon(
    val url: String,
    val x: Int? = null,
    val y: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class MarkerGroup(
    val id: String,
    val name: String,
    val description: String? = null,
    val categoryIds: List<String>,
)
