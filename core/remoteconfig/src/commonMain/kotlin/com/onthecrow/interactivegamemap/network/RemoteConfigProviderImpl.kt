package com.onthecrow.interactivegamemap.network

import com.onthecrow.interactivegamemap.analytics.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.String

internal abstract class RemoteConfigProviderImpl : KoinComponent, RemoteConfigProvider {

    private val json = Json { ignoreUnknownKeys = true }
    private val _configDataFlow = MutableStateFlow<RemoteConfig?>(null)
    private val crashReporter: CrashReporter by inject()

    init {
        startBackgroundSync()
    }

    override fun getRemoteConfigFlow(): Flow<RemoteConfig> {
        return _configDataFlow.filterNotNull()
            .distinctUntilChanged()
    }

    protected abstract fun getString(key: String): String
    protected abstract fun startBackgroundSync()

    /** A line of the crash reports' log. */
    protected fun log(message: String) {
        crashReporter.log("Remote config: $message")
    }

    protected fun onConfigUpdated(configField: String? = null) {
        when (configField) {
            // todo
            else -> updateWholeConfig()
        }
    }

    private fun updateWholeConfig() {
        val maps = getString(KEY_MAPS)
        try {
            _configDataFlow.value = json.decodeFromString<RemoteConfig>(maps)
        } catch (e: IllegalArgumentException) {
            // Nothing has been fetched yet or the value is malformed, keep the current config
            log("can't parse, ${e.message}")
            // A malformed one leaves every user without the maps
            if (maps.isNotEmpty()) crashReporter.recordNonFatal(e)
        }
    }

    companion object Companion {
        protected const val KEY_MAPS = "maps"
    }
}
