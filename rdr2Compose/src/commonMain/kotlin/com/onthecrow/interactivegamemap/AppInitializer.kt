package com.onthecrow.interactivegamemap

import com.onthecrow.interactivegamemap.di.applicationModule
import org.koin.core.context.startKoin

object AppInitializer {
    fun initialize(platform: Platform) {
        initKoin(platform)
    }
}

private fun initKoin(platform: Platform) = startKoin {
    modules(
        platform.platformModule,
        applicationModule,
    )
}
