package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The animals of the compendium, made by tools/rdr2-animals from a guide's screenshots of the map. */
@Serializable
internal data class AnimalCompendium(
    @SerialName("animals")
    val animals: List<Animal>,
)

/**
 * @param map "world", or "guarma", which isn't in the app, its animals have no points
 * @param points where the guide's author found the animal
 * @param zones the areas the author marked around those places
 * @param description in the markup of the markers' descriptions
 */
@Serializable
internal data class Animal(
    @SerialName("compendium")
    val compendium: Int,
    @SerialName("name")
    val name: String,
    @SerialName("legendary")
    val legendary: Boolean,
    @SerialName("map")
    val map: String,
    @SerialName("points")
    val points: List<AnimalPoint>,
    @SerialName("zones")
    val zones: List<AnimalZone>,
    @SerialName("description")
    val description: String,
)

@Serializable
internal data class AnimalPoint(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
)

@Serializable
internal data class AnimalZone(
    @SerialName("polygon")
    val polygon: List<AnimalPoint>,
)
