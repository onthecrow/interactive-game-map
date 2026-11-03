package com.onthecrow.interactivegamemap.analytics.di

import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsId
import com.onthecrow.interactivegamemap.analytics.AnalyticsImpl
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.analytics.CrashReporterImpl
import com.onthecrow.interactivegamemap.analytics.FirebaseAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.IOSAnalyticsIdStorage
import com.onthecrow.interactivegamemap.analytics.LogAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.LogCrashReportTracker
import com.onthecrow.interactivegamemap.analytics.readOrCreate
import kotlin.experimental.ExperimentalNativeApi
import org.koin.dsl.module

@OptIn(ExperimentalNativeApi::class)
internal actual val analyticsPlatformModule = module {
    single { IOSAnalyticsIdStorage.readOrCreate() }
    single<Analytics> {
        AnalyticsImpl(
            trackers = listOfNotNull(
                FirebaseAnalyticsTracker,
                LogAnalyticsTracker.takeIf { Platform.isDebugBinary },
            ),
            userId = get<AnalyticsId>().value,
        )
    }
    // Crashlytics isn't reached from Kotlin yet, a release build reports nothing of its own
    single<CrashReporter> {
        CrashReporterImpl(
            trackers = listOfNotNull(LogCrashReportTracker.takeIf { Platform.isDebugBinary }),
            userId = get<AnalyticsId>().value,
        )
    }
}
