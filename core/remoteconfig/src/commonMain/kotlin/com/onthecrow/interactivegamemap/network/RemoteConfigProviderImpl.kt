package com.onthecrow.interactivegamemap.network

import com.onthecrow.interactivegamemap.analytics.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal abstract class RemoteConfigProviderImpl : KoinComponent, RemoteConfigProvider {

    // Bumped on every activation, so every key is read again
    private val revision = MutableStateFlow(0)
    private val crashReporter: CrashReporter by inject()

    init {
        startBackgroundSync()
    }

    override fun observe(key: String): Flow<String> {
        return revision
            .filter { it > 0 }
            .map { getString(key) }
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
    }

    protected abstract fun getString(key: String): String
    protected abstract fun startBackgroundSync()

    /** A line of the crash reports' log. */
    protected fun log(message: String) {
        crashReporter.log("Remote config: $message")
    }

    protected fun onConfigUpdated() {
        revision.update { it + 1 }
    }
}
