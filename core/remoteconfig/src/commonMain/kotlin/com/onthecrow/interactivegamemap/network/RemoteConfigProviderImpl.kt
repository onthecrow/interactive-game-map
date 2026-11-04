package com.onthecrow.interactivegamemap.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import kotlin.String

internal abstract class RemoteConfigProviderImpl : KoinComponent, RemoteConfigProvider {

    private val json = Json { ignoreUnknownKeys = true }
    private val _configDataFlow = MutableStateFlow<RemoteConfig?>(null)

    init {
        startBackgroundSync()
    }

    override fun getRemoteConfigFlow(): Flow<RemoteConfig> {
        return _configDataFlow.filterNotNull()
            .distinctUntilChanged()
    }

    protected abstract fun getString(key: String): String
    protected abstract fun startBackgroundSync()

    protected fun onConfigUpdated(configField: String? = null) {
        when (configField) {
            // todo
            else -> updateWholeConfig()
        }
    }

    private fun updateWholeConfig() {
        try {
            _configDataFlow.value = json.decodeFromString<RemoteConfig>(getString(KEY_MAPS))
        } catch (e: IllegalArgumentException) {
            // Nothing has been fetched yet or the value is malformed, keep the current config
            println("Can't parse remote config: ${e.message}")
        }
    }

    companion object Companion {
        protected const val KEY_MAPS = "maps"
    }
}
