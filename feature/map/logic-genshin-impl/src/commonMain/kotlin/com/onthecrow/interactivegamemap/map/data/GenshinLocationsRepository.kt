package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabProjection
import com.onthecrow.interactivegamemap.map.model.LocationDungeon
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerCategory
import com.onthecrow.interactivegamemap.map.model.MarkerGroup
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.map.model.MarkerPin
import interactivegamemap.feature.map.logic_genshin_impl.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64

// Our own pin, the picture of the category in its head
private const val PIN_FILE = "files/pins/genshin_pin.xml"
private const val PIN_WIDTH = 34
private const val PIN_HEIGHT = 42

// The pictures of the categories are big, the image service of HoYoLAB's CDN gives them in the size of a pin's head
private const val ICON_SIZE_QUERY = "?x-oss-process=image%2Fresize%2Cw_96%2Fquality%2CQ_90%2Fformat%2Cwebp"

// Shown at the first launch: the Statues of The Seven and the Teleport Waypoints, as the game's map shows them
private val VISIBLE_BY_DEFAULT = setOf(2, 3)

/** The points of HoYoLAB's map with the names of their categories, as the last update left them. */
internal class GenshinLocationsRepository(private val content: GenshinContent) : LocationsRepository {
    private val mutex = Mutex()
    private var catalogCache: Pair<String, MarkerCatalog>? = null
    private var locationsCache: Pair<String, List<MapLocation>>? = null
    private var pinUrl: String? = null

    override suspend fun getLocations(language: String): List<MapLocation> {
        val snapshot = content.snapshot() ?: return emptyList()
        val catalog = getMarkerCatalog(language)
        val key = "${content.revision.value}/$language"
        mutex.withLock { locationsCache?.takeIf { it.first == key }?.let { return it.second } }
        val locations = snapshot.toLocations(catalog)
        mutex.withLock { locationsCache = key to locations }
        return locations
    }

    override suspend fun getMarkerCatalog(language: String): MarkerCatalog {
        val labels = content.labels(language) ?: return MarkerCatalog(emptyMap(), emptyMap())
        val snapshot = content.snapshot()
        val key = "${content.revision.value}/${labels.language}"
        mutex.withLock { catalogCache?.takeIf { it.first == key }?.let { return it.second } }
        val catalog = labels.toCatalog(pin = MarkerIcon(url = pinUrl()), mapIds = snapshot?.maps?.map { it.id }.orEmpty())
        mutex.withLock { catalogCache = key to catalog }
        return catalog
    }

    private suspend fun pinUrl(): String {
        pinUrl?.let { return it }
        val url = "data:${MarkerIcon.VECTOR_DRAWABLE_MIME_TYPE};base64," + Base64.encode(Res.readBytes(PIN_FILE))
        pinUrl = url
        return url
    }
}

/** Every group of the tree for every map: the marker list leaves out the categories without points on a map. */
internal fun SnapshotLabels.toCatalog(pin: MarkerIcon, mapIds: List<Int>): MarkerCatalog {
    val categories = groups.flatMap { group ->
        group.children.map { label ->
            val icon = MarkerIcon(url = label.icon + ICON_SIZE_QUERY)
            MarkerCategory(
                id = label.id.toString(),
                name = label.name,
                icon = icon,
                pin = MarkerPin(icon = pin, pictogram = icon, width = PIN_WIDTH, height = PIN_HEIGHT),
                visibleByDefault = label.id in VISIBLE_BY_DEFAULT,
            )
        }
    }
    val markerGroups = groups.map { group ->
        MarkerGroup(
            id = group.id.toString(),
            name = group.name,
            categoryIds = group.children.map { it.id.toString() },
        )
    }
    return MarkerCatalog(
        categories = categories.associateBy { it.id },
        groupsByMap = mapIds.associateWith { markerGroups },
        mapNames = mapNames,
    )
}

/** The points on the maps' worlds, the ones of the categories shown at first ahead: the map shows a limited number. */
internal fun GenshinSnapshot.toLocations(catalog: MarkerCatalog): List<MapLocation> {
    val projections = maps.associate { it.id to HoyolabProjection(it.detail) }
    val entrances = groups.flatMap { it.floors }.associate { it.id to it.entranceIds }
    val locations = ArrayList<MapLocation>(points.size)
    for (i in 0 until points.size) {
        val projection = projections[points.mapIds[i]] ?: continue
        val categoryId = points.labelIds[i].toString()
        val category = catalog.categories[categoryId]
        val groupId = points.groupIds[i]
        val floorId = points.floorIds[i]
        locations += MapLocation(
            id = points.ids[i],
            mapId = points.mapIds[i],
            categoryId = categoryId,
            name = category?.name.orEmpty(),
            description = "",
            lat = projection.lat(points.ys[i]),
            lng = projection.lng(points.xs[i]),
            dungeon = if (groupId == 0) {
                null
            } else {
                LocationDungeon(
                    dungeonId = groupId,
                    floorId = floorId.takeIf { it != 0 },
                    entranceIds = entrances[floorId].orEmpty(),
                )
            },
            pin = category?.pin,
        )
    }
    return locations.sortedBy { catalog.categories[it.categoryId]?.visibleByDefault != true }
}
