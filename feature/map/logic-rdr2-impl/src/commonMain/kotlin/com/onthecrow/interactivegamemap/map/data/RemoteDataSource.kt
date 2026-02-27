package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.model.Location
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

internal class RemoteDataSource(
    private val client: HttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun getLocations(): List<Location> {
        val response = client.get("$BASE_URL/rdr2_markers.json").bodyAsText()
        return json.decodeFromString(response)
    }

    private companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/onthecrow/interactive-game-map/refs/heads/main/config"
    }
}