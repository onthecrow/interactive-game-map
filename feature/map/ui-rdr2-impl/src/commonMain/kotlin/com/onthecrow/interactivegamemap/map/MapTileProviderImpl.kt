package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import io.ktor.client.HttpClient

class MapTileProviderImpl(
    configProvider: RemoteConfigProvider,
    httpClient: HttpClient,
) : MapTileProvider(configProvider, httpClient) {

    override fun getXYZ(x: Int, y: Int, zoom: Int): Triple<String, String, String> {
        return Triple(x.toString(), y.toString(), zoom.toString())
    }
}
