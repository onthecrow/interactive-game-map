package com.onthecrow.interactivegamemap.map

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val KEY_MARKER_LIST_SETTINGS = "marker_list_settings"

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
}
