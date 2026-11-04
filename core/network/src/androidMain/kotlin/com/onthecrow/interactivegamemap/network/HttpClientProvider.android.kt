package com.onthecrow.interactivegamemap.network

import com.onthecrow.interactivegamemap.network.util.installJson
import com.onthecrow.interactivegamemap.network.util.installLogging
import com.onthecrow.interactivegamemap.network.util.installTimeout
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dispatcher

// The map loads many small tiles from one host at once, the default limit is 5
private const val MAX_REQUESTS_PER_HOST = 32

actual fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            dispatcher(Dispatcher().apply { maxRequestsPerHost = MAX_REQUESTS_PER_HOST })
        }
    }
    installTimeout()
    installLogging()
    installJson()
}
