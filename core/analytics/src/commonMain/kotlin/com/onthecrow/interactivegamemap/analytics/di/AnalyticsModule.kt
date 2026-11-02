package com.onthecrow.interactivegamemap.analytics.di

import com.onthecrow.interactivegamemap.analytics.ScreenTracker
import org.koin.core.module.Module
import org.koin.dsl.module

/** Gives [com.onthecrow.interactivegamemap.analytics.Analytics] with the trackers of the platform. */
internal expect val analyticsPlatformModule: Module

val analyticsModule = module {
    includes(analyticsPlatformModule)
    single { ScreenTracker(get()) }
}
