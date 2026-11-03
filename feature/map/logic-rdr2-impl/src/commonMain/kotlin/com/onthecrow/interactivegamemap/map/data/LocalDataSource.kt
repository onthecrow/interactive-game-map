package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.model.Animal
import com.onthecrow.interactivegamemap.map.model.AnimalCompendium
import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import interactivegamemap.feature.map.logic_rdr2_impl.generated.resources.Res
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

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

    suspend fun getAnimals(): List<Animal> {
        return json.decodeFromString<AnimalCompendium>(Res.readBytes("files/animals.json").decodeToString()).animals
    }

    /** The pin of the animals comes with the app, the source's sprite has none for them. */
    suspend fun getAnimalPinUrl(): String {
        return "data:image/png;base64," + Base64.encode(Res.readBytes("files/animal_pin.png"))
    }
}
