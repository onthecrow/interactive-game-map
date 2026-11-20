package com.onthecrow.interactivegamemap.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapStyleSettingTest {
    private val storage = object : SettingsStorage {
        val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }
    }

    @Test
    fun `the map follows the theme by default`() {
        assertTrue(MapStyleMode.THEME.isDark(isDarkTheme = true))
        assertFalse(MapStyleMode.THEME.isDark(isDarkTheme = false))
    }

    @Test
    fun `a picked map style wins over the theme`() {
        assertFalse(MapStyleMode.STANDARD.isDark(isDarkTheme = true))
        assertTrue(MapStyleMode.DARK.isDark(isDarkTheme = false))
    }

    @Test
    fun `settings saved before the map style get the default one`() {
        storage.putString("app_settings", """{"theme":"DARK","clusterMarkers":false}""")

        val settings = SettingsRepositoryImpl(storage).settings.value

        assertEquals(MapStyleMode.THEME, settings.mapStyle)
        assertEquals(ThemeMode.DARK, settings.theme)
        assertFalse(settings.clusterMarkers)
    }

    @Test
    fun `the picked map style is kept`() {
        SettingsRepositoryImpl(storage).update { it.copy(mapStyle = MapStyleMode.DARK) }

        assertEquals(MapStyleMode.DARK, SettingsRepositoryImpl(storage).settings.value.mapStyle)
    }
}
