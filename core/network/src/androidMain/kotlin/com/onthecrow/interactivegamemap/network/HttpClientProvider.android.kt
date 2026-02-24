package com.onthecrow.interactivegamemap.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

actual fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout.Plugin) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 45_000
        socketTimeoutMillis = 30_000
    }
    install(Logging.Companion) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}