package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import interactivegamemap.feature.map.logic_rdr2_impl.generated.resources.Res
import kotlinx.serialization.json.Json

internal class LocalDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getLocations(): List<Location> {
        return json.decodeFromString(Res.readBytes("files/markers.json").decodeToString())
    }

    suspend fun getMarkerCatalog(): MarkerCatalog {
        return json.decodeFromString(Res.readBytes("files/marker_catalog.json").decodeToString())
    }
}
