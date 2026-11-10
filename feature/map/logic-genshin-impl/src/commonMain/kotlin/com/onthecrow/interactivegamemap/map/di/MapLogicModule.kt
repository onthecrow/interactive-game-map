package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.MapsRepository
import com.onthecrow.interactivegamemap.map.common.RemoteConfigMapsRepository
import com.onthecrow.interactivegamemap.map.common.di.contentStoreModule
import com.onthecrow.interactivegamemap.map.data.GenshinContent
import com.onthecrow.interactivegamemap.map.data.GenshinLocationsRepository
import com.onthecrow.interactivegamemap.map.data.GenshinMapsRepository
import com.onthecrow.interactivegamemap.map.data.HoyolabSyncTask
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabApi
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabConfigProvider
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabTileUrlResolver
import com.onthecrow.interactivegamemap.map.tiles.TileUrlResolver
import com.onthecrow.interactivegamemap.sync.SyncTask
import org.koin.dsl.bind
import org.koin.dsl.module

/** The data of HoYoLAB's map for the logic of the map, see mapDomainModule; downloaded and kept by its sync task. */
val mapLogicModule = module {
    includes(contentStoreModule)
    single { GenshinContent(get()) }
    single { HoyolabConfigProvider(get(), get()) }
    single { HoyolabApi(get(), get()) }
    single<LocationsRepository> { GenshinLocationsRepository(get()) }
    single<MapsRepository> { GenshinMapsRepository(RemoteConfigMapsRepository(get(), get()), get()) }
    single<SyncTask> { HoyolabSyncTask(get(), get(), get(), get()) }
    single { HoyolabTileUrlResolver() } bind TileUrlResolver::class
}
