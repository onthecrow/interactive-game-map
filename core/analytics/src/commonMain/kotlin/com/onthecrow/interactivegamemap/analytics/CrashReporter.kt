package com.onthecrow.interactivegamemap.analytics

/** The screen the user sees, or [ScreenTracker.BACKGROUND]: a key of the crash reports. */
const val KEY_SCREEN = "screen"

/**
 * What a crash report tells besides the stack: a log of what the app did, its state as keys, and the errors it went
 * on after. The log and the keys go with the next crash or non-fatal error of the session.
 * Firebase Crashlytics on Android; iOS sends nothing of it yet.
 */
interface CrashReporter {
    /** A line of the log, a session keeps its last 64 KB. */
    fun log(message: String)

    /** Strings, numbers, booleans and enums; a null value empties a key, a key can't be removed. At most 64 keys. */
    fun setKeys(keys: Map<String, Any?>)

    /** An error the app went on after. A session keeps only its last 8, so it's for the rare ones. */
    fun recordNonFatal(throwable: Throwable)
}

/** Where the reports go: Crashlytics, the log of a debug build. The values of the keys are strings, longs and doubles. */
interface CrashReportTracker {
    fun log(message: String)
    fun setKeys(keys: Map<String, Any>)
    fun recordNonFatal(throwable: Throwable)
    fun setUserId(id: String)
}

/** @param userId the [AnalyticsId], the reports go with it from the start */
internal class CrashReporterImpl(private val trackers: List<CrashReportTracker>, userId: String) : CrashReporter {

    init {
        trackers.forEach { it.setUserId(userId) }
    }

    override fun log(message: String) {
        trackers.forEach { it.log(message) }
    }

    override fun setKeys(keys: Map<String, Any?>) {
        val values = normalize(keys.mapValues { it.value ?: "" })
        trackers.forEach { it.setKeys(values) }
    }

    override fun recordNonFatal(throwable: Throwable) {
        trackers.forEach { it.recordNonFatal(throwable) }
    }
}

/** Prints the reports, a debug build sends none. */
internal object LogCrashReportTracker : CrashReportTracker {
    override fun log(message: String) {
        println("Crash log: $message")
    }

    override fun setKeys(keys: Map<String, Any>) {
        println("Crash keys: $keys")
    }

    override fun recordNonFatal(throwable: Throwable) {
        println("Crash non-fatal: ${throwable.stackTraceToString()}")
    }

    override fun setUserId(id: String) {
        println("Crash user id: $id")
    }
}
