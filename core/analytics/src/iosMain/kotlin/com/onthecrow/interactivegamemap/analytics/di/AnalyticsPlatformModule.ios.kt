package com.onthecrow.interactivegamemap.analytics.di

import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsImpl
import com.onthecrow.interactivegamemap.analytics.FirebaseAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.LogAnalyticsTracker
import kotlin.experimental.ExperimentalNativeApi
import org.koin.dsl.module

@OptIn(ExperimentalNativeApi::class)
internal actual val analyticsPlatformModule = module {
    single<Analytics> {
        AnalyticsImpl(
            listOfNotNull(
                FirebaseAnalyticsTracker,
                LogAnalyticsTracker.takeIf { Platform.isDebugBinary },
            )
        )
    }
}
