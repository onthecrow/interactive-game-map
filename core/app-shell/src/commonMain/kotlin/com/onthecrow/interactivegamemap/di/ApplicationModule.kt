package com.onthecrow.interactivegamemap.di

import com.onthecrow.interactivegamemap.FlavorConfig
import com.onthecrow.interactivegamemap.analytics.di.analyticsModule
import com.onthecrow.interactivegamemap.map.MapDestination
import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.di.mapDomainModule
import com.onthecrow.interactivegamemap.map.di.mapModule
import com.onthecrow.interactivegamemap.network.di.networkModule
import com.onthecrow.interactivegamemap.network.di.remoteConfigModule
import com.onthecrow.interactivegamemap.settings.LegalNotice
import com.onthecrow.interactivegamemap.settings.PrivacyPolicyUrl
import com.onthecrow.interactivegamemap.settings.di.settingsModule
import com.onthecrow.interactivegamemap.settings.di.settingsUiModule
import com.onthecrow.interactivegamemap.sync.di.syncModule
import com.onthecrow.nomadrates.database.di.databaseModule
import com.onthecrow.nomadrates.navigation.Destination
import com.onthecrow.nomadrates.navigation.di.StartDestination
import com.onthecrow.nomadrates.navigation.di.navigationModule
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/** Everything of an app: the shared modules and the ones of its game, see [FlavorConfig]. */
fun applicationModule(flavor: FlavorConfig): Module = module {
    single { Json { ignoreUnknownKeys = true } }
    single(StartDestination) { MapDestination } bind Destination::class
    single { PrivacyPolicyUrl(flavor.privacyPolicyUrl) }
    single { LegalNotice(flavor.legalNotice) }
    single { flavor.mapStyle }
    single { MapTileProvider(get(), get()) }
    includes(
        analyticsModule,
        navigationModule,
        databaseModule,
        networkModule,
        remoteConfigModule,
        settingsModule,
        settingsUiModule,
        syncModule,
        mapModule,
        mapDomainModule,
        flavor.uiModule,
        flavor.logicModule,
    )
}
