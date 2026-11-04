package com.onthecrow.interactivegamemap.analytics

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Sends nothing in a debug build, the manifest of the app turns the collection off there. */
internal class FirebaseCrashReportTracker(private val crashlytics: FirebaseCrashlytics) : CrashReportTracker {

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setKeys(keys: Map<String, Any>) {
        val values = CustomKeysAndValues.Builder()
        keys.forEach { (key, value) ->
            when (value) {
                is Long -> values.putLong(key, value)
                is Double -> values.putDouble(key, value)
                else -> values.putString(key, value.toString())
            }
        }
        crashlytics.setCustomKeys(values.build())
    }

    override fun recordNonFatal(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun setUserId(id: String) {
        crashlytics.setUserId(id)
    }
}
