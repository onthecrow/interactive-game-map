package com.onthecrow.interactivegamemap.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import kotlin.String

internal abstract class RemoteConfigProviderImpl : KoinComponent, RemoteConfigProvider {

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
        _configDataFlow.update {
            RemoteConfig(
                tilesUrl = getString(key = KEY_TILES_URL),
                initialZoom = getString(KEY_INITIAL_ZOOM).toInt(),
                maxZoom = getString(KEY_MAX_ZOOM).toInt(),
                minZoom = getString(KEY_MIN_ZOOM).toInt(),
                startLat = getString(KEY_START_LAT).toInt(),
                startLng = getString(KEY_START_LNG).toInt(),
            )
        }
    }

    companion object Companion {
        protected const val KEY_INITIAL_ZOOM = "initial_zoom"
        protected const val KEY_MAX_ZOOM = "max_zoom"
        protected const val KEY_MIN_ZOOM = "min_zoom"
        protected const val KEY_START_LAT = "start_lat"
        protected const val KEY_START_LNG = "start_lng"
        protected const val KEY_TILES_URL = "tiles_url"
    }
}
