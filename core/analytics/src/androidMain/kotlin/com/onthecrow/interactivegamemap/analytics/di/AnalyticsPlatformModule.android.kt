package com.onthecrow.interactivegamemap.analytics.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsId
import com.onthecrow.interactivegamemap.analytics.AnalyticsImpl
import com.onthecrow.interactivegamemap.analytics.AndroidAnalyticsIdStorage
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.analytics.CrashReporterImpl
import com.onthecrow.interactivegamemap.analytics.FirebaseAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.FirebaseCrashReportTracker
import com.onthecrow.interactivegamemap.analytics.LogAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.LogCrashReportTracker
import com.onthecrow.interactivegamemap.analytics.readOrCreate
import org.koin.dsl.module

private val Context.isDebuggable: Boolean
    get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

internal actual val analyticsPlatformModule = module {
    single { AndroidAnalyticsIdStorage(get()).readOrCreate() }
    single<Analytics> {
        val context: Context = get()
        AnalyticsImpl(
            trackers = listOfNotNull(
                FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context)),
                LogAnalyticsTracker.takeIf { context.isDebuggable },
            ),
            userId = get<AnalyticsId>().value,
        )
    }
    // At the launch, so a crash before anything reports goes with the id too
    single<CrashReporter>(createdAtStart = true) {
        CrashReporterImpl(
            trackers = listOfNotNull(
                FirebaseCrashReportTracker(FirebaseCrashlytics.getInstance()),
                LogCrashReportTracker.takeIf { get<Context>().isDebuggable },
            ),
            userId = get<AnalyticsId>().value,
        )
    }
}
