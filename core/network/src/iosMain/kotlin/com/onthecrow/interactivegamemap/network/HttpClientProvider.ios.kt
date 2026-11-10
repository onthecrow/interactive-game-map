package com.onthecrow.interactivegamemap.network

import com.onthecrow.interactivegamemap.network.util.installJson
import com.onthecrow.interactivegamemap.network.util.installLogging
import com.onthecrow.interactivegamemap.network.util.installTimeout
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import org.koin.core.scope.Scope

actual fun provideHttpClient(logRequests: Boolean): HttpClient = HttpClient(Darwin) {
    installTimeout()
    if (logRequests) installLogging()
    installJson()
}

@OptIn(ExperimentalNativeApi::class)
internal actual fun Scope.isDebugBuild(): Boolean = Platform.isDebugBinary
