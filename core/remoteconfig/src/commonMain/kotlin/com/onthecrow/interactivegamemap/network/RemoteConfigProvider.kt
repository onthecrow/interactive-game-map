package com.onthecrow.interactivegamemap.network

import kotlinx.coroutines.flow.Flow

/** The values of Firebase Remote Config by their keys, raw: their readers parse them. */
interface RemoteConfigProvider {
    /**
     * The value of [key] once something has been fetched or activated, then each new one. An empty value, of a key
     * not fetched yet or not set, is skipped.
     */
    fun observe(key: String): Flow<String>
}
