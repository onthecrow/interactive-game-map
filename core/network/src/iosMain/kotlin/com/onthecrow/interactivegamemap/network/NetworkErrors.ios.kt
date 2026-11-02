package com.onthecrow.interactivegamemap.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

internal actual fun Throwable.platformErrorType(): String? {
    val code = (this as? DarwinHttpRequestException)?.origin?.code ?: return null
    return when (code) {
        NSURLErrorTimedOut -> "timeout"
        NSURLErrorNotConnectedToInternet,
        NSURLErrorNetworkConnectionLost,
        NSURLErrorCannotFindHost,
        NSURLErrorCannotConnectToHost,
        NSURLErrorDNSLookupFailed,
        NSURLErrorDataNotAllowed,
        NSURLErrorInternationalRoamingOff -> "no_network"
        else -> "url_error_$code"
    }
}
