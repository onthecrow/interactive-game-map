package com.onthecrow.interactivegamemap.di

import com.onthecrow.interactivegamemap.analytics.di.analyticsModule
import com.onthecrow.interactivegamemap.map.MapDestination
import com.onthecrow.interactivegamemap.map.di.genshinMapModule
import com.onthecrow.interactivegamemap.map.di.mapLogicModule
import com.onthecrow.interactivegamemap.map.di.mapModule
import com.onthecrow.interactivegamemap.network.di.networkModule
import com.onthecrow.interactivegamemap.network.di.remoteConfigModule
import com.onthecrow.interactivegamemap.settings.PrivacyPolicyUrl
import com.onthecrow.interactivegamemap.settings.di.settingsModule
import com.onthecrow.interactivegamemap.settings.di.settingsUiModule
import com.onthecrow.interactivegamemap.ui.genshin.di.genshinUiModule
import com.onthecrow.nomadrates.database.di.databaseModule
import com.onthecrow.nomadrates.navigation.Destination
import com.onthecrow.nomadrates.navigation.di.StartDestination
import com.onthecrow.nomadrates.navigation.di.navigationModule
import kotlinx.serialization.json.Json
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single(StartDestination) { MapDestination } bind Destination::class
    single { PrivacyPolicyUrl("https://onthecrow.github.io/interactive-game-map/genshin/privacy-policy/") }
    includes(
        analyticsModule,
        navigationModule,
        databaseModule,
        networkModule,
        remoteConfigModule,
        settingsModule,
        settingsUiModule,
        genshinUiModule,
        mapModule,
        genshinMapModule,
        mapLogicModule,
    )
}
