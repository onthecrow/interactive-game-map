package com.onthecrow.interactivegamemap.analytics

import kotlin.concurrent.Volatile

// The limits of Google Analytics for Firebase
internal const val MAX_STRING_LENGTH = 100
internal const val MAX_USER_PROPERTY_LENGTH = 36

/**
 * Something the user did or saw.
 * The common events are in AnalyticsEvents.kt; an app or a platform has events of its own by implementing it too.
 */
interface AnalyticsEvent {
    val name: String

    /** Strings, numbers, booleans and enums; a null value isn't sent. */
    val params: Map<String, Any?>
}

/** The analytics of every app and platform; the events go to each tracker of the platform, see analyticsModule. */
interface Analytics {
    fun log(event: AnalyticsEvent)

    /** @param value null removes the property */
    fun setUserProperty(name: String, value: String?)

    /** Parameters every event gets from now on, a null value removes one. */
    fun setDefaultParams(params: Map<String, Any?>)
}

/** Where the events go: Firebase, the log of a debug build. The values are strings, longs and doubles. */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any>)
    fun setUserProperty(name: String, value: String?)
}

internal class AnalyticsImpl(private val trackers: List<AnalyticsTracker>) : Analytics {

    @Volatile
    private var defaultParams = emptyMap<String, Any>()

    override fun log(event: AnalyticsEvent) {
        val params = defaultParams + normalize(event.params)
        trackers.forEach { it.logEvent(event.name, params) }
    }

    override fun setUserProperty(name: String, value: String?) {
        trackers.forEach { it.setUserProperty(name, value?.take(MAX_USER_PROPERTY_LENGTH)) }
    }

    override fun setDefaultParams(params: Map<String, Any?>) {
        defaultParams = defaultParams - params.keys + normalize(params)
    }
}

/** The values Firebase takes: a string of up to 100 characters, a long or a double. */
internal fun normalize(params: Map<String, Any?>): Map<String, Any> {
    return params.mapNotNull { (key, value) ->
        val normalized: Any = when (value) {
            null -> return@mapNotNull null
            is Boolean -> value.toString()
            is Int -> value.toLong()
            is Long, is Double -> value
            is Float -> value.toDouble()
            is Enum<*> -> value.name.lowercase()
            else -> value.toString().take(MAX_STRING_LENGTH)
        }
        key to normalized
    }.toMap()
}

/** Prints the events, a debug build sends none. */
internal object LogAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any>) {
        println("Analytics: $name $params")
    }

    override fun setUserProperty(name: String, value: String?) {
        println("Analytics: user property $name = $value")
    }
}
