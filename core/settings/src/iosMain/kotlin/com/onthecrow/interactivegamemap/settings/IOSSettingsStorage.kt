package com.onthecrow.interactivegamemap.settings

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val KEY_APPLE_LANGUAGES = "AppleLanguages"

internal class IOSSettingsStorage : SettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}

internal actual fun systemLanguageTags(): List<String> {
    // The language of the app is the app's own AppleLanguages, they hide the system's ones while they are set
    val defaults = NSUserDefaults.standardUserDefaults
    val appLanguages = NSBundle.mainBundle.bundleIdentifier
        ?.let { defaults.persistentDomainForName(it) }
        ?.get(KEY_APPLE_LANGUAGES)
    defaults.removeObjectForKey(KEY_APPLE_LANGUAGES)
    val systemLanguages = NSLocale.preferredLanguages.filterIsInstance<String>()
    appLanguages?.let { defaults.setObject(it, forKey = KEY_APPLE_LANGUAGES) }
    return systemLanguages
}
