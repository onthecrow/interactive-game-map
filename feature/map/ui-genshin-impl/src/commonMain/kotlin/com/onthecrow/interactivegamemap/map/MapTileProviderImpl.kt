package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.network.RemoteConfigProvider
import io.ktor.client.HttpClient

class MapTileProviderImpl(
    configProvider: RemoteConfigProvider,
    httpClient: HttpClient,
) : MapTileProvider(configProvider, httpClient) {

    override fun getXYZ(x: Int, y: Int, zoom: Int): Triple<String, String, String> {
        return when (zoom) {
            10 -> Triple((x - 512).toString(), (y - 512).inv().toString(), zoom.toString())
            11 -> Triple((x - 1024).toString(), (y - 1024).inv().toString(), zoom.toString())
            12 -> Triple((x - 2048).toString(), (y - 2048).inv().toString(), zoom.toString())
            13 -> Triple((x - 4096).toString(), (y - 4096).inv().toString(), zoom.toString())
            14 -> Triple((x - 8192).toString(), (y - 8192).inv().toString(), zoom.toString())
            15 -> Triple((x - 16384).toString(), (y - 16384).inv().toString(), zoom.toString())
            else -> Triple(x.toString(), y.inv().toString(), zoom.toString()) /*throw IllegalStateException()*/
        }
    }
}
