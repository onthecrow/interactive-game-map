package com.onthecrow.interactivegamemap.analytics.di

import com.onthecrow.interactivegamemap.analytics.ScreenTracker
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Gives [com.onthecrow.interactivegamemap.analytics.Analytics] and
 * [com.onthecrow.interactivegamemap.analytics.CrashReporter] with the trackers of the platform, and
 * [com.onthecrow.interactivegamemap.analytics.AnalyticsId].
 */
internal expect val analyticsPlatformModule: Module

val analyticsModule = module {
    includes(analyticsPlatformModule)
    single { ScreenTracker(get(), get()) }
}
