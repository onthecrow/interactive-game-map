package com.onthecrow.interactivegamemap

import com.onthecrow.interactivegamemap.di.applicationModule
import com.onthecrow.interactivegamemap.sync.Synchronizer
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/** Starts an app of the shell; each app calls it with its [FlavorConfig] as early as it can. */
object AppShell {
    fun initialize(platform: Platform, flavor: FlavorConfig) {
        startKoin {
            modules(
                platform.platformModule,
                applicationModule(flavor),
            )
        }
    }

    /** For iOS, which has no Application to inject the synchronizer into: right after [initialize]. */
    fun startSync() {
        KoinPlatform.getKoin().get<Synchronizer>().start()
    }
}
