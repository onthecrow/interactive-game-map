package com.onthecrow.interactivegamemap.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/** Sends nothing in a debug build, the manifest of the app turns the collection off there. */
internal class FirebaseAnalyticsTracker(private val firebase: FirebaseAnalytics) : AnalyticsTracker {

    override fun logEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebase.logEvent(name, bundle)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebase.setUserProperty(name, value)
    }

    override fun setUserId(id: String) {
        firebase.setUserId(id)
    }
}
