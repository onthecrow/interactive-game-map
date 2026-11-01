package com.onthecrow.interactivegamemap.map

import android.content.Context

private const val PREFERENCES_NAME = "map_preferences"

internal class AndroidPreferencesStorage(context: Context) : PreferencesStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? {
        return preferences.getString(key, null)
    }

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
