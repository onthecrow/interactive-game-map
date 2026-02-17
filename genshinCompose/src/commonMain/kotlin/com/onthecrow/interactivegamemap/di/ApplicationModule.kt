package com.onthecrow.interactivegamemap.di


import com.onthecrow.interactivegamemap.map.MapDestination
import com.onthecrow.interactivegamemap.map.di.mapModule
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
        mapModule,
    )
}
