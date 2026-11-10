package com.onthecrow.interactivegamemap.network

import android.content.Context
import android.content.pm.ApplicationInfo
import com.onthecrow.interactivegamemap.network.util.installJson
import com.onthecrow.interactivegamemap.network.util.installLogging
import com.onthecrow.interactivegamemap.network.util.installTimeout
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Dispatcher
import org.koin.core.scope.Scope

// The map loads many small tiles from one host at once, the default limit is 5
private const val MAX_REQUESTS_PER_HOST = 32

actual fun provideHttpClient(logRequests: Boolean): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            dispatcher(Dispatcher().apply { maxRequestsPerHost = MAX_REQUESTS_PER_HOST })
        }
    }
    installTimeout()
    if (logRequests) installLogging()
    installJson()
}

internal actual fun Scope.isDebugBuild(): Boolean {
    return get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
