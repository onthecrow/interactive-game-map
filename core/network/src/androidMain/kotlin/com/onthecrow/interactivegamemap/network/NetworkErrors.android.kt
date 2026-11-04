package com.onthecrow.interactivegamemap.network

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal actual fun Throwable.platformErrorType(): String? {
    return when (this) {
        is SocketTimeoutException -> "timeout"
        is UnknownHostException, is ConnectException, is NoRouteToHostException -> "no_network"
        is SSLException -> "ssl"
        else -> null
    }
}
