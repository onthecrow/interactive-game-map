package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.model.Locale
import com.onthecrow.interactivegamemap.map.model.Location
import com.onthecrow.interactivegamemap.map.model.LocationResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class RemoteDataSource(
    private val client: HttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getLocations(): LocationResponse {
        val response = client.get("$BASE_URL/genshin_markers.json").bodyAsText()
        return json.decodeFromString(response)
    }

    suspend fun getLocale(): Locale {
        val response = client.get("$BASE_URL/genshin_markers_locale.json").bodyAsText()
        return json.decodeFromString(response)
    }

    private companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/onthecrow/interactive-game-map/refs/heads/main/config"
    }
}
