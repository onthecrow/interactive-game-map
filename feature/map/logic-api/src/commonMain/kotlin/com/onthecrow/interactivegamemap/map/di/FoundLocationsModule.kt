package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.ExportFoundLocationsUseCase
import com.onthecrow.interactivegamemap.map.ExportFoundLocationsUseCaseImpl
import com.onthecrow.interactivegamemap.map.FoundLocationsRepository
import com.onthecrow.interactivegamemap.map.FoundLocationsRepositoryImpl
import com.onthecrow.interactivegamemap.map.GetFoundLocationIdsUseCase
import com.onthecrow.interactivegamemap.map.GetFoundLocationIdsUseCaseImpl
import com.onthecrow.interactivegamemap.map.ImportFoundLocationsUseCase
import com.onthecrow.interactivegamemap.map.ImportFoundLocationsUseCaseImpl
import com.onthecrow.interactivegamemap.map.SetLocationFoundUseCase
import com.onthecrow.interactivegamemap.map.SetLocationFoundUseCaseImpl
import org.koin.dsl.module

/** The found markers, the same in every app; the exports and the imports need the [GameId] of the app. */
val foundLocationsModule = module {
    single<FoundLocationsRepository> { FoundLocationsRepositoryImpl(get()) }
    single<GetFoundLocationIdsUseCase> { GetFoundLocationIdsUseCaseImpl(get()) }
    single<SetLocationFoundUseCase> { SetLocationFoundUseCaseImpl(get()) }
    single<ExportFoundLocationsUseCase> { ExportFoundLocationsUseCaseImpl(get(), get()) }
    single<ImportFoundLocationsUseCase> { ImportFoundLocationsUseCaseImpl(get(), get()) }
}
