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
    /** Whether the marker list shows how many markers the category has. */
    val showsCount: Boolean = true,
)

/** How the markers of a category look on the map, the size is in dp. */
@Serializable
data class MarkerPin(
    /** The pin, or the pin under [pictogram]. */
    val icon: MarkerIcon,
    /** The image for the markers in dungeons, a badge is drawn over [icon] when there is none. */
    val dungeonIcon: MarkerIcon? = null,
    /**
     * Drawn over [icon] in the middle of its round head: the head is as wide as the pin, at its top, a disc is all
     * head. So one pin serves many categories, each with its pictogram.
     */
    val pictogram: MarkerIcon? = null,
    val width: Int,
    val height: Int,
    /** Where the location lies on the pin, of its height from the top: at its point by default, 0.5 for a disc. */
    val anchorY: Float = 1f,
)

/**
 * A whole image, or a part of a sprite when the position and the size are set. The url can be a data: URL of an
 * image shipped with the app, a PNG one or a vector drawable of [VECTOR_DRAWABLE_MIME_TYPE].
 */
@Serializable
data class MarkerIcon(
    val url: String,
    val x: Int? = null,
    val y: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    /** Rubbed off the image wherever it is drawn: on the map and in the lists. */
    val wear: MarkerWear? = null,
) {
    companion object {
        /** The XML of an Android vector drawable, which is drawn sharp at any size. */
        const val VECTOR_DRAWABLE_MIME_TYPE = "image/vnd.android.vector+xml"
    }
}

/**
 * The wear of an image, as of printed ink: the image is kept only where [texture] is clear, the Porter-Duff
 * destination out. Every image is laid over a part of the texture as big as the image, its own part.
 */
@Serializable
data class MarkerWear(
    /** Only its alpha counts: how much is rubbed off. A bitmap. */
    val texture: MarkerIcon,
    /** How many dp the texture spans. */
    val size: Int,
    /** How much of the texture's alpha is rubbed off, from 0 to 1. */
    val strength: Float = 1f,
)

@Serializable
data class MarkerGroup(
    val id: String,
    val name: String,
    val description: String? = null,
    val categoryIds: List<String>,
)
