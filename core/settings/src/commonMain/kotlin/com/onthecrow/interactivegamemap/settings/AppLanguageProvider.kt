package com.onthecrow.interactivegamemap.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The languages the interface is translated to, the markers of Genshin Impact are translated to them too. */
val SUPPORTED_LANGUAGES = listOf("en", "de", "es", "fr", "ja", "ko", "pt", "ru", "zh")

/** The language of the app when the system's languages aren't supported. */
const val DEFAULT_LANGUAGE = "en"

/** The system's language tags in the order of the user's preference. */
internal expect fun systemLanguageTags(): List<String>

/**
 * The language of the app: the one picked in the settings, otherwise the first supported one of the system's
 * languages, otherwise [DEFAULT_LANGUAGE].
 */
class AppLanguageProvider internal constructor(
    private val settingsRepository: SettingsRepository,
    private val readSystemLanguages: () -> List<String>,
) {
    private val systemLanguages = MutableStateFlow(readSystemLanguages())

    val language: Flow<String> = combine(settingsRepository.settings, systemLanguages) { settings, system ->
        resolveLanguage(settings.language, system)
    }.distinctUntilChanged()

    /** What following the system gives. */
    val systemLanguage: Flow<String> = systemLanguages
        .map { resolveLanguage(null, it) }
        .distinctUntilChanged()

    val currentLanguage: String
        get() = resolveLanguage(settingsRepository.settings.value.language, systemLanguages.value)

    /** The system's languages can change while the app runs, Android recreates the activity then. */
    fun refreshSystemLanguages() {
        systemLanguages.value = readSystemLanguages()
    }
}

internal fun resolveLanguage(picked: String?, systemLanguages: List<String>): String {
    picked?.takeIf { it in SUPPORTED_LANGUAGES }?.let { return it }
    return systemLanguages.firstNotNullOfOrNull { tag ->
        // The tags may have a script and a region, like zh-Hans-CN
        tag.substringBefore('-').substringBefore('_').lowercase().takeIf { it in SUPPORTED_LANGUAGES }
    } ?: DEFAULT_LANGUAGE
}
