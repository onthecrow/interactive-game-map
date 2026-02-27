package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.GetLocationsUseCase
import com.onthecrow.interactivegamemap.map.LocationsRepository
import com.onthecrow.interactivegamemap.map.GetLocationsUseCaseImpl
import com.onthecrow.interactivegamemap.map.data.LocationsRepositoryImpl
import com.onthecrow.interactivegamemap.map.data.RemoteDataSource
import org.koin.dsl.module

val mapLogicModule = module {
    single<GetLocationsUseCase> { GetLocationsUseCaseImpl(get()) }
    single<LocationsRepository> { LocationsRepositoryImpl(get()) }
    single { RemoteDataSource(get()) }
}