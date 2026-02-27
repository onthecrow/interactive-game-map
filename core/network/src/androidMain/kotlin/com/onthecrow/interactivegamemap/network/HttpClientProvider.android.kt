package com.onthecrow.interactivegamemap.network

import com.onthecrow.interactivegamemap.network.util.installJson
import com.onthecrow.interactivegamemap.network.util.installLogging
import com.onthecrow.interactivegamemap.network.util.installTimeout
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
    installTimeout()
    installLogging()
    installJson()
}
