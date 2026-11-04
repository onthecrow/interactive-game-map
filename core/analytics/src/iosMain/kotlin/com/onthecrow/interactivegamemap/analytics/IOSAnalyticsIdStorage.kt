package com.onthecrow.interactivegamemap.analytics

import platform.Foundation.NSUserDefaults

internal object IOSAnalyticsIdStorage : AnalyticsIdStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(): String? {
        return defaults.stringForKey(KEY_ANALYTICS_ID)
    }

    override fun write(id: String) {
        defaults.setObject(id, forKey = KEY_ANALYTICS_ID)
    }
}
