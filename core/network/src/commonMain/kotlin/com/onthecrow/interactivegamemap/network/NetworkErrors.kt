package com.onthecrow.interactivegamemap.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

/** Why a request failed in a few words, for the analytics: timeout, no_network or the name of the error. */
fun Throwable.networkErrorType(): String {
    return when (this) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> "timeout"
        else -> platformErrorType() ?: this::class.simpleName ?: "unknown"
    }
}

/** Whether the request failed because of the connection, not because of the app or the server's answer. */
fun Throwable.isNetworkError(): Boolean {
    return this is HttpRequestTimeoutException || this is ConnectTimeoutException || this is SocketTimeoutException ||
        platformErrorType() != null
}

internal expect fun Throwable.platformErrorType(): String?
