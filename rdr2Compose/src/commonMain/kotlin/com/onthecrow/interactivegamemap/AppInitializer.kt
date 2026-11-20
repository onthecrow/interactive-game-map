package com.onthecrow.interactivegamemap

/** Starts the app: from the Application on Android, from the App of SwiftUI on iOS. */
object AppInitializer {
    fun initialize(platform: Platform) {
        AppShell.initialize(platform, rdr2Flavor)
    }

    /** iOS only: Android injects the synchronizer into its Application and starts it there. */
    fun startSync() {
        AppShell.startSync()
    }
}
