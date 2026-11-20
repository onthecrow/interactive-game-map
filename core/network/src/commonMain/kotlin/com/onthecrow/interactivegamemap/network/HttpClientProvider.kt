package com.onthecrow.interactivegamemap.network

import io.ktor.client.HttpClient
import org.koin.core.scope.Scope

/** @param logRequests every request goes to the log, only for debug builds: the map loads hundreds of tiles */
expect fun provideHttpClient(logRequests: Boolean): HttpClient

/** Whether the app is a debug build, the same check as the debug trackers of the analytics. */
internal expect fun Scope.isDebugBuild(): Boolean
