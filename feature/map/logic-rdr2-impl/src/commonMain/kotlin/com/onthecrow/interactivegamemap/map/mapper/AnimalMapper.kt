package com.onthecrow.interactivegamemap.map.mapper

import com.onthecrow.interactivegamemap.map.model.Animal
import com.onthecrow.interactivegamemap.map.model.GeoPoint
import com.onthecrow.interactivegamemap.map.model.LocationArea
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerCategory
import com.onthecrow.interactivegamemap.map.model.MarkerGroup

private const val ANIMAL_CATEGORY_ID_PREFIX = "animal-"
private const val ANIMALS_GROUP_ID = "animals"
private const val WORLD_MAP = "world"
private const val WORLD_MAP_ID = 1

// The ids of the animals' markers are past the source's ones, an animal's points follow its compendium number
internal const val ANIMAL_ID_OFFSET = 10_000_000
internal const val MAX_ANIMAL_POINTS = 10

// An animal is marked as on the map of the game: the paw on the black disc of the other pins
private const val LEGENDARY_ANIMAL_CATEGORY_ID = "37"
private const val PAW_URL = "files/pictograms/animal.xml"

/** Every animal is a category of its own, the categories are in the order of the compendium. */
internal val Animal.categoryId: String
    get() = ANIMAL_CATEGORY_ID_PREFIX + compendium

// The legendary animals are the source's markers already, the animals of Guarma have no points
private val List<Animal>.displayed: List<Animal>
    get() = filter { !it.legendary && it.map == WORLD_MAP && it.points.isNotEmpty() }.sortedBy { it.compendium }

/** Adds a category for every animal, hidden until the user shows it, in a group of the world map. */
internal fun MarkerCatalog.withAnimals(animals: List<Animal>): MarkerCatalog {
    val legendary = categories.getValue(LEGENDARY_ANIMAL_CATEGORY_ID)
    val paw = legendary.icon.copy(url = PAW_URL)
    val pin = legendary.pin.copy(pictogram = paw)
    val animalCategories = animals.displayed.map { animal ->
        MarkerCategory(
            id = animal.categoryId,
            name = animal.name,
            icon = paw,
            pin = pin,
            visibleByDefault = false,
            // An animal has one marker, the count would say nothing
            showsCount = false,
        )
    }
    val group = MarkerGroup(
        id = ANIMALS_GROUP_ID,
        name = "Animals",
        categoryIds = animalCategories.map { it.id },
    )
    return copy(
        categories = categories + animalCategories.associateBy { it.id },
        groupsByMap = groupsByMap + (WORLD_MAP_ID to groupsByMap[WORLD_MAP_ID].orEmpty() + group),
    )
}

internal fun List<Animal>.toMapLocations(catalog: MarkerCatalog): List<MapLocation> {
    return displayed.flatMap { animal ->
        val pin = catalog.categories[animal.categoryId]?.pin
        val areas = animal.zones.map { zone -> LocationArea(zone.polygon.map { GeoPoint(it.lat, it.lng) }) }
        animal.points.take(MAX_ANIMAL_POINTS).mapIndexed { index, point ->
            MapLocation(
                id = ANIMAL_ID_OFFSET + animal.compendium * MAX_ANIMAL_POINTS + index,
                mapId = WORLD_MAP_ID,
                categoryId = animal.categoryId,
                name = animal.name,
                description = animal.description,
                lat = point.lat,
                lng = point.lng,
                pin = pin,
                areas = areas,
            )
        }
    }
}
