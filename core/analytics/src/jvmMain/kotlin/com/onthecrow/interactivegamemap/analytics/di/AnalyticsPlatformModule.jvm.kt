package com.onthecrow.interactivegamemap.analytics.di

import com.onthecrow.interactivegamemap.analytics.Analytics
import com.onthecrow.interactivegamemap.analytics.AnalyticsId
import com.onthecrow.interactivegamemap.analytics.AnalyticsIdStorage
import com.onthecrow.interactivegamemap.analytics.AnalyticsImpl
import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.analytics.CrashReporterImpl
import com.onthecrow.interactivegamemap.analytics.LogAnalyticsTracker
import com.onthecrow.interactivegamemap.analytics.LogCrashReportTracker
import com.onthecrow.interactivegamemap.analytics.readOrCreate
import org.koin.dsl.module

// Nothing is kept, each start is a new installation
private object MemoryAnalyticsIdStorage : AnalyticsIdStorage {
    private var id: String? = null

    override fun read() = id

    override fun write(id: String) {
        this.id = id
    }
}

internal actual val analyticsPlatformModule = module {
    single { MemoryAnalyticsIdStorage.readOrCreate() }
    single<Analytics> { AnalyticsImpl(listOf(LogAnalyticsTracker), get<AnalyticsId>().value) }
    single<CrashReporter> { CrashReporterImpl(listOf(LogCrashReportTracker), get<AnalyticsId>().value) }
}
