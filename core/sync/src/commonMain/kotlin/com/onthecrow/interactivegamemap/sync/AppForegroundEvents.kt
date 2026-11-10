package com.onthecrow.interactivegamemap.sync

/** The moments the app comes to the foreground: its launch and every return from the background. */
interface AppForegroundEvents {
    /** Calls [onForeground] on each of them from now on; from the main thread. */
    fun register(onForeground: () -> Unit)
}
