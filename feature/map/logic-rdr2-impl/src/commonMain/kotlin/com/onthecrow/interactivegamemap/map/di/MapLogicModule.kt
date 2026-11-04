package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.GetLocationsByIdsUseCase
import com.onthecrow.interactivegamemap.map.GetLocationsByIdsUseCaseImpl
import com.onthecrow.interactivegamemap.map.GetLocationsUseCase
import com.onthecrow.interactivegamemap.map.GetLocationsUseCaseImpl
import com.onthecrow.interactivegamemap.map.GetMarkerGroupsUseCase
import com.onthecrow.interactivegamemap.map.GetMarkerGroupsUseCaseImpl
import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.LocationsRepositoryImpl
import com.onthecrow.interactivegamemap.map.data.LocalDataSource
import org.koin.dsl.module

val mapLogicModule = module {
    includes(foundLocationsModule)
    single<GetLocationsUseCase> { GetLocationsUseCaseImpl(get()) }
    single<GetLocationsByIdsUseCase> { GetLocationsByIdsUseCaseImpl(get()) }
    single<GetMarkerGroupsUseCase> { GetMarkerGroupsUseCaseImpl(get()) }
    single<LocationsRepository> { LocationsRepositoryImpl(get()) }
    single { LocalDataSource() }
}
