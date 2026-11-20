package com.onthecrow.interactivegamemap.map.common

/**
 * The rules every update of a game's data passes before it replaces the data on the device: a source that answers
 * with nothing, or with a part of what it had, is more likely broken than emptied.
 */
object UpdateGuard {
    /** The least part of the current items an update keeps. */
    const val MIN_KEPT_SHARE = 0.5

    /** Why [newCount] items shouldn't replace [currentCount] ones, null when they may. */
    fun rejectReason(newCount: Int, currentCount: Int?): String? = when {
        newCount <= 0 -> "empty"
        currentCount != null && currentCount > 0 && newCount < currentCount * MIN_KEPT_SHARE ->
            "shrank from $currentCount to $newCount"
        else -> null
    }
}
