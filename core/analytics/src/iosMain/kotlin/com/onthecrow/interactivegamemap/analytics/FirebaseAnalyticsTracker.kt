package com.onthecrow.interactivegamemap.analytics

import kotlinx.cinterop.ExperimentalForeignApi
import platform.FirebaseAnalytics.FIRAnalytics

/** Sends nothing in a debug build, iOSApp turns the collection off there. */
@OptIn(ExperimentalForeignApi::class)
internal object FirebaseAnalyticsTracker : AnalyticsTracker {

    override fun logEvent(name: String, params: Map<String, Any>) {
        // The longs and doubles reach Objective-C as NSNumber
        FIRAnalytics.logEventWithName(name, parameters = params.mapKeys { it.key as Any? })
    }

    override fun setUserProperty(name: String, value: String?) {
        FIRAnalytics.setUserPropertyString(value, forName = name)
    }
}
