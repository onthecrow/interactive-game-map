package com.onthecrow.interactivegamemap.sync.di

import com.onthecrow.interactivegamemap.sync.AppForegroundEvents
import com.onthecrow.interactivegamemap.sync.ProcessForegroundEvents
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val syncPlatformModule: Module = module {
    single<AppForegroundEvents> { ProcessForegroundEvents() }
}
