package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.FoundLocationsRepository
import com.onthecrow.interactivegamemap.map.FoundLocationsRepositoryImpl
import com.onthecrow.interactivegamemap.map.GetFoundLocationIdsUseCase
import com.onthecrow.interactivegamemap.map.GetFoundLocationIdsUseCaseImpl
import com.onthecrow.interactivegamemap.map.SetLocationFoundUseCase
import com.onthecrow.interactivegamemap.map.SetLocationFoundUseCaseImpl
import org.koin.dsl.module

val foundLocationsModule = module {
    single<FoundLocationsRepository> { FoundLocationsRepositoryImpl(get()) }
    single<GetFoundLocationIdsUseCase> { GetFoundLocationIdsUseCaseImpl(get()) }
    single<SetLocationFoundUseCase> { SetLocationFoundUseCaseImpl(get()) }
}
