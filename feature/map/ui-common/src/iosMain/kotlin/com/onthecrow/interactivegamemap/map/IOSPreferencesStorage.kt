package com.onthecrow.interactivegamemap.map

import platform.Foundation.NSUserDefaults

internal class IOSPreferencesStorage : PreferencesStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
