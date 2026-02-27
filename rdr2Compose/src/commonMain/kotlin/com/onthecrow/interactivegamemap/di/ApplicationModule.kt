package com.onthecrow.interactivegamemap.di

import com.onthecrow.interactivegamemap.map.MapDestination
import com.onthecrow.interactivegamemap.map.di.mapLogicModule
import com.onthecrow.interactivegamemap.map.di.mapModule
import com.onthecrow.interactivegamemap.map.di.rdr2MapModule
import com.onthecrow.interactivegamemap.network.di.networkModule
import com.onthecrow.interactivegamemap.network.di.remoteConfigModule
import com.onthecrow.nomadrates.navigation.Destination
import com.onthecrow.nomadrates.navigation.di.StartDestination
import com.onthecrow.nomadrates.navigation.di.navigationModule
import kotlinx.serialization.json.Json
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationModule = module {
    single { Json { ignoreUnknownKeys = true } }
    single(StartDestination) { MapDestination } bind Destination::class
    includes(
        navigationModule,
        networkModule,
        remoteConfigModule,
        mapModule,
        rdr2MapModule,
        mapLogicModule,
    )
}
