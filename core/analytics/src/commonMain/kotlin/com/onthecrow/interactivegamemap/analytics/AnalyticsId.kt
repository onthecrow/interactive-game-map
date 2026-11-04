package com.onthecrow.interactivegamemap.analytics

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal const val KEY_ANALYTICS_ID = "analytics_id"

/**
 * A random id of the app's installation, the analytics and the crash reports go with it: the reports of one
 * installation can be put together, and the user can tell it from the settings, so theirs are found.
 * It's made of nothing of the device or the user and stays until the app's data is cleared.
 */
class AnalyticsId internal constructor(val value: String)

internal interface AnalyticsIdStorage {
    fun read(): String?
    fun write(id: String)
}

@OptIn(ExperimentalUuidApi::class)
internal fun AnalyticsIdStorage.readOrCreate(): AnalyticsId {
    return AnalyticsId(read() ?: Uuid.random().toString().also(::write))
}
