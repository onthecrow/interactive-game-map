package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.model.Animal
import com.onthecrow.interactivegamemap.map.model.AnimalCompendium
import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import interactivegamemap.feature.map.logic_rdr2_impl.generated.resources.Res
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

// The images of the app the catalog names, the pictograms and the pins are vector drawables
private const val FILES_DIRECTORY = "files/"
private const val VECTOR_DRAWABLE_EXTENSION = ".xml"
private const val PNG_MIME_TYPE = "image/png"

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

    /** The url of an icon, a data: URL of the image for a file of the app, a files/ one, which can't be downloaded. */
    suspend fun getIconUrl(url: String): String {
        if (!url.startsWith(FILES_DIRECTORY)) return url
        val mimeType = if (url.endsWith(VECTOR_DRAWABLE_EXTENSION)) MarkerIcon.VECTOR_DRAWABLE_MIME_TYPE else PNG_MIME_TYPE
        return "data:$mimeType;base64," + Base64.encode(Res.readBytes(url))
    }
}
