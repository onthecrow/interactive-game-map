package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.MapTileProvider
import org.koin.dsl.module

val rdr2MapModule = module {
    single { MapTileProvider(get()) }
}
