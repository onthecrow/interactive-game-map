package com.onthecrow.interactivegamemap.analytics.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsImpl
import com.onthecrow.interactivegamemap.analytics.FirebaseAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.LogAnalyticsTracker
import org.koin.dsl.module

internal actual val analyticsPlatformModule = module {
    single<Analytics> {
        val context: Context = get()
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        AnalyticsImpl(
            listOfNotNull(
                FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context)),
                LogAnalyticsTracker.takeIf { isDebuggable },
            )
        )
    }
}
