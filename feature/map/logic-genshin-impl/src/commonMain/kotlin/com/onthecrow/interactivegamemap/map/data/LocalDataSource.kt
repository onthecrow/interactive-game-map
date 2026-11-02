package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.model.LocationResponse
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerLocale
import interactivegamemap.feature.map.logic_genshin_impl.generated.resources.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.MissingResourceException

internal class LocalDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getLocations(): LocationResponse {
        return json.decodeFromString(Res.readBytes("files/markers.json").decodeToString())
    }

    suspend fun getMarkerCatalog(): MarkerCatalog {
        return json.decodeFromString(Res.readBytes("files/marker_catalog.json").decodeToString())
    }

    /** Null for English, the language of the catalog, and for the languages the names aren't translated to. */
    suspend fun getLocale(language: String): MarkerLocale? {
        val bytes = try {
            Res.readBytes("files/locales/$language.json")
        } catch (_: MissingResourceException) {
            return null
        }
        return json.decodeFromString(bytes.decodeToString())
    }
}
