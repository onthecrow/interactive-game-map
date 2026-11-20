package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.RoutePlan
import com.onthecrow.interactivegamemap.map.model.RouteStop
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val KEY_MARKER_LIST_SETTINGS = "marker_list_settings"
private const val KEY_MAP_VIEW_SETTINGS = "map_view_settings"
private const val KEY_ROUTE = "route"

internal interface PreferencesStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** The state of the marker list which is kept between launches of the app. */
@Serializable
internal data class MarkerListSettings(
    val collapsedGroupIds: Set<String> = emptySet(),
    val categoryVisibility: Map<String, Boolean> = emptyMap(),
    val cycles: Map<String, Int> = emptyMap(),
)

/** The map which was open at the last launch and where its camera was, [camera] is null at the start of a map. */
@Serializable
internal data class MapViewSettings(
    val mapId: Int? = null,
    val camera: CameraPosition? = null,
)

/**
 * The route the user follows, kept between launches: its stops in order, a location or a fast travel point each, and
 * the page of its popup.
 */
@Serializable
internal data class SavedRoute(
    val mapId: Int,
    val locationIds: List<Int>,
    /** The fast travel point before each location, or -1: a stop is a jump there first. */
    val fastTravelBefore: List<Int>,
    val page: Int = 0,
) {
    fun toPlan(): RoutePlan {
        val stops = locationIds.flatMapIndexed { index, id ->
            listOfNotNull(
                fastTravelBefore.getOrNull(index)?.takeIf { it >= 0 }?.let { RouteStop.FastTravel(it) },
                RouteStop.Location(id),
            )
        }
        return RoutePlan(mapId, stops)
    }

    companion object {
        fun of(plan: RoutePlan, page: Int): SavedRoute {
            val locationIds = mutableListOf<Int>()
            val fastTravelBefore = mutableListOf<Int>()
            var pending = -1
            plan.stops.forEach { stop ->
                when (stop) {
                    is RouteStop.FastTravel -> pending = stop.pointId
                    is RouteStop.Location -> {
                        locationIds += stop.locationId
                        fastTravelBefore += pending
                        pending = -1
                    }
                }
            }
            return SavedRoute(plan.mapId, locationIds, fastTravelBefore, page)
        }
    }
}

internal class MapPreferences(
    private val storage: PreferencesStorage,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getMarkerListSettings(): MarkerListSettings {
        val settings = storage.getString(KEY_MARKER_LIST_SETTINGS) ?: return MarkerListSettings()
        return try {
            json.decodeFromString(settings)
        } catch (_: IllegalArgumentException) {
            MarkerListSettings()
        }
    }

    fun setMarkerListSettings(settings: MarkerListSettings) {
        storage.putString(KEY_MARKER_LIST_SETTINGS, json.encodeToString(settings))
    }

    fun getMapViewSettings(): MapViewSettings {
        val settings = storage.getString(KEY_MAP_VIEW_SETTINGS) ?: return MapViewSettings()
        return try {
            json.decodeFromString(settings)
        } catch (_: IllegalArgumentException) {
            MapViewSettings()
        }
    }

    fun setMapViewSettings(settings: MapViewSettings) {
        storage.putString(KEY_MAP_VIEW_SETTINGS, json.encodeToString(settings))
    }

    fun getSavedRoute(): SavedRoute? {
        val route = storage.getString(KEY_ROUTE)?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            json.decodeFromString<SavedRoute>(route)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Whether [hint] has been shown: each one is shown once. */
    fun isHintShown(hint: MapHint): Boolean = storage.getString(hint.key) == true.toString()

    fun setHintShown(hint: MapHint) {
        storage.putString(hint.key, true.toString())
    }

    /** Null forgets the route. */
    fun setSavedRoute(route: SavedRoute?) {
        storage.putString(KEY_ROUTE, route?.let { json.encodeToString(it) }.orEmpty())
    }
}
