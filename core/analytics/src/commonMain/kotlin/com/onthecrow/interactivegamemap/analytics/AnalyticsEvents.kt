package com.onthecrow.interactivegamemap.analytics

import kotlin.time.Duration

// The events of every app and platform. The privacy policy, docs/privacy-policy, tells what they send: keep it in step

/** The map open on the screen, a default parameter of the events. */
const val PARAM_MAP_ID = "map_id"

/** A screen is shown: at the launch, on the way to another screen and back from the background. */
data class ScreenViewEvent(
    val screenName: String,
    /** The screen left for this one, [ScreenTracker.BACKGROUND] or null at the launch. */
    val previousScreen: String?,
) : AnalyticsEvent {
    override val name get() = "screen_view"
    override val params
        get() = mapOf(
            "screen_name" to screenName,
            // Compose screens have no classes of their own, Firebase would put the activity there
            "screen_class" to screenName,
            "previous_screen" to previousScreen,
        )
}

enum class MapLoadSource { LAUNCH, MAP_SWITCH, RELOAD }

/** The first tiles of a map are on the screen. */
data class MapReadyEvent(
    val source: MapLoadSource,
    /** From the moment the map got its tile layer to the first tile drawn. */
    val loadTime: Duration,
    /** From the start of the app, only at the launch. */
    val launchTime: Duration?,
    /** The tiles that couldn't be loaded before. */
    val tilesFailed: Int,
    /** Whether the map screen was left while the tiles were loading, the time includes the time away then. */
    val wasHidden: Boolean,
) : AnalyticsEvent {
    override val name get() = "map_ready"
    override val params
        get() = mapOf(
            "source" to source,
            "load_ms" to loadTime.inWholeMilliseconds,
            "launch_ms" to launchTime?.inWholeMilliseconds,
            "tiles_failed" to tilesFailed,
            "was_hidden" to wasHidden,
        )
}

/** The user leaves the map screen: for another screen or the background, which closing the app goes through. */
data class MapTimeEvent(
    /** From the moment the map screen was shown. */
    val screenTime: Duration,
    /** From map_ready, or from the moment the screen was shown with the map ready; null while it isn't. */
    val readyTime: Duration?,
    /** The screen the user went to or [ScreenTracker.BACKGROUND]. */
    val exitTo: String,
    /** Whether a map was open, there is none until the remote config comes. */
    val hasMap: Boolean,
    /** Whether the screen was shown for the first time since the start of the app. */
    val isFirstVisit: Boolean,
    /** The tiles of the open map since it got its tile layer. */
    val tilesLoaded: Int,
    val tilesFailed: Int,
    /** Why the last tile couldn't be loaded: timeout, no_network, http_403 and so on. */
    val tileError: String?,
) : AnalyticsEvent {
    override val name get() = "map_time"
    override val params
        get() = mapOf(
            "screen_ms" to screenTime.inWholeMilliseconds,
            "ready_ms" to readyTime?.inWholeMilliseconds,
            "is_ready" to (readyTime != null),
            "exit_to" to exitTo,
            "has_map" to hasMap,
            "is_first_visit" to isFirstVisit,
            "tiles_loaded" to tilesLoaded,
            "tiles_failed" to tilesFailed,
            "tile_error" to tileError,
        )
}

enum class SearchType { MAP, MARKER_LIST, LINK }

/** A search term the user stopped at. */
data class SearchEvent(
    val type: SearchType,
    val term: String,
    /** The markers found on the map, or the categories in the marker list. */
    val resultsCount: Int,
) : AnalyticsEvent {
    override val name get() = "search"
    override val params
        get() = mapOf(
            "search_type" to type,
            "search_term" to term,
            "results_count" to resultsCount,
        )
}

/** Found is pressed on the card of a marker. */
data class MarkerFoundChangeEvent(
    val locationId: Int,
    val categoryId: String?,
    /** The new state. */
    val isFound: Boolean,
    /** The found markers of the game after the change. */
    val foundTotal: Int,
) : AnalyticsEvent {
    override val name get() = "marker_found_change"
    override val params
        get() = mapOf(
            "location_id" to locationId,
            "category_id" to categoryId,
            "is_found" to isFound,
            "found_total" to foundTotal,
        )
}

enum class AnalyticsSetting { HIDE_MARKERS_WHILE_MOVING, CLUSTER_MARKERS, THEME, DYNAMIC_COLOR, LANGUAGE }

/** The user changes a setting. */
data class SettingChangeEvent(
    val setting: AnalyticsSetting,
    /** A boolean, an enum or a string: the new value. */
    val value: Any,
) : AnalyticsEvent {
    override val name get() = "setting_change"
    override val params
        get() = mapOf(
            "setting_name" to setting,
            "setting_value" to value,
        )
}
