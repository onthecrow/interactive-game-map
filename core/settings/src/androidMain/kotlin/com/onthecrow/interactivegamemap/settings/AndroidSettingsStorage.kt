package com.onthecrow.interactivegamemap.settings

import android.content.Context
import android.content.res.Resources

private const val PREFERENCES_NAME = "app_settings"

internal class AndroidSettingsStorage(context: Context) : SettingsStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? {
        return preferences.getString(key, null)
    }

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}

internal actual fun systemLanguageTags(): List<String> {
    // The language of the app changes the configuration of the app, not the system's one
    val locales = Resources.getSystem().configuration.locales
    return (0 until locales.size()).map { locales[it].toLanguageTag() }
}
