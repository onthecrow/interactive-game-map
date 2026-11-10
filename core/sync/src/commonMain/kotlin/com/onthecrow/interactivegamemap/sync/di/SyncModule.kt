package com.onthecrow.interactivegamemap.sync.di

import com.onthecrow.interactivegamemap.sync.DefaultSynchronizer
import com.onthecrow.interactivegamemap.sync.SyncPolicy
import com.onthecrow.interactivegamemap.sync.Synchronizer
import org.koin.core.module.Module
import org.koin.dsl.module

/** The synchronizer of the app; the app binds its [com.onthecrow.interactivegamemap.sync.SyncTask]. */
val syncModule: Module = module {
    includes(syncPlatformModule)
    single<Synchronizer> {
        DefaultSynchronizer(
            task = get(),
            foregroundEvents = get(),
            crashReporter = get(),
            policy = getOrNull<SyncPolicy>() ?: SyncPolicy(),
        )
    }
}

internal expect val syncPlatformModule: Module
