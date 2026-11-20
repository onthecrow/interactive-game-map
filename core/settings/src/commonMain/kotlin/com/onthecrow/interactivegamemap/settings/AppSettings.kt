package com.onthecrow.interactivegamemap.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** The colors of the system's theme, they exist from Android 12. */
    val dynamicColor: Boolean = true,
    /** The tag of the language picked by the user, null to follow the system. */
    val language: String? = null,
    val hideMarkersWhileMoving: Boolean = false,
    val clusterMarkers: Boolean = true,
    /** The look of the map, in an app whose game has a dark map. */
    val mapStyle: MapStyleMode = MapStyleMode.THEME,
)

@Serializable
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    /** Null when the system decides. */
    val isDark: Boolean?
        get() = when (this) {
            SYSTEM -> null
            LIGHT -> false
            DARK -> true
        }
}

@Serializable
enum class MapStyleMode {
    /** The dark map in the dark theme, the usual one in the light theme. */
    THEME,
    STANDARD,
    DARK;

    fun isDark(isDarkTheme: Boolean): Boolean = when (this) {
        THEME -> isDarkTheme
        STANDARD -> false
        DARK -> true
    }
}
