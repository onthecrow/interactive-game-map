package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.MapTileProvider
import com.onthecrow.interactivegamemap.map.MapTileProviderImpl
import org.koin.dsl.module

val genshinMapModule = module {
    single<MapTileProvider> { MapTileProviderImpl(get(), get()) }
}
