package com.onthecrow.interactivegamemap.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.json.Json

private const val KEY_APP_SETTINGS = "app_settings"

internal interface SettingsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun update(transform: (AppSettings) -> AppSettings)
}

internal class SettingsRepositoryImpl(
    private val storage: SettingsStorage,
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val _settings = MutableStateFlow(read())

    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun update(transform: (AppSettings) -> AppSettings) {
        val settings = _settings.updateAndGet(transform)
        storage.putString(KEY_APP_SETTINGS, json.encodeToString(settings))
    }

    private fun read(): AppSettings {
        val settings = storage.getString(KEY_APP_SETTINGS) ?: return AppSettings()
        return try {
            json.decodeFromString(settings)
        } catch (_: IllegalArgumentException) {
            AppSettings()
        }
    }
}
