package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.MapsRepository
import com.onthecrow.interactivegamemap.map.common.RemoteConfigMapsRepository
import com.onthecrow.interactivegamemap.map.data.LocalDataSource
import com.onthecrow.interactivegamemap.map.LocationsRepositoryImpl
import com.onthecrow.interactivegamemap.sync.NoopSyncTask
import com.onthecrow.interactivegamemap.sync.SyncTask
import org.koin.dsl.module

/** The data of the game, all in the build, for the logic of the map, see mapDomainModule. */
val mapLogicModule = module {
    single<LocationsRepository> { LocationsRepositoryImpl(get()) }
    single<MapsRepository> { RemoteConfigMapsRepository(get(), get()) }
    single { LocalDataSource() }
    // Everything is in the build
    single<SyncTask> { NoopSyncTask }
}
