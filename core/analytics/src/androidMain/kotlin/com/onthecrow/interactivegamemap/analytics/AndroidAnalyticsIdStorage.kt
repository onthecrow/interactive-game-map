package com.onthecrow.interactivegamemap.analytics

import android.content.Context

private const val PREFERENCES_NAME = "analytics"

internal class AndroidAnalyticsIdStorage(context: Context) : AnalyticsIdStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): String? {
        return preferences.getString(KEY_ANALYTICS_ID, null)
    }

    override fun write(id: String) {
        preferences.edit().putString(KEY_ANALYTICS_ID, id).apply()
    }
}
