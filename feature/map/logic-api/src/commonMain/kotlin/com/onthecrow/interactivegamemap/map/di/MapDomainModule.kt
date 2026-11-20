package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.GetLocationsByIdsUseCase
import com.onthecrow.interactivegamemap.map.GetLocationsByIdsUseCaseImpl
import com.onthecrow.interactivegamemap.map.GetLocationsUseCase
import com.onthecrow.interactivegamemap.map.GetLocationsUseCaseImpl
import com.onthecrow.interactivegamemap.map.GetMapsUseCase
import com.onthecrow.interactivegamemap.map.GetMapsUseCaseImpl
import com.onthecrow.interactivegamemap.map.GetMarkerGroupsUseCase
import com.onthecrow.interactivegamemap.map.GetMarkerGroupsUseCaseImpl
import com.onthecrow.interactivegamemap.map.ObserveContentUpdatesUseCase
import com.onthecrow.interactivegamemap.map.ObserveContentUpdatesUseCaseImpl
import com.onthecrow.interactivegamemap.map.tiles.CenteredTileUrlResolver
import com.onthecrow.interactivegamemap.map.tiles.TileUrlResolver
import com.onthecrow.interactivegamemap.map.tiles.TileUrlResolvers
import com.onthecrow.interactivegamemap.map.tiles.XyzTileUrlResolver
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * The logic of the map, one for every game. A game's module gives the repositories and its sync task, and may add a
 * [TileUrlResolver] of its own scheme.
 */
val mapDomainModule = module {
    includes(foundLocationsModule)
    single<GetLocationsUseCase> { GetLocationsUseCaseImpl(get()) }
    single<GetLocationsByIdsUseCase> { GetLocationsByIdsUseCaseImpl(get()) }
    single<GetMarkerGroupsUseCase> { GetMarkerGroupsUseCaseImpl(get()) }
    single<GetMapsUseCase> { GetMapsUseCaseImpl(get(), get()) }
    single<ObserveContentUpdatesUseCase> { ObserveContentUpdatesUseCaseImpl(get()) }
    single { CenteredTileUrlResolver() } bind TileUrlResolver::class
    single { XyzTileUrlResolver() } bind TileUrlResolver::class
    single { TileUrlResolvers(getAll()) }
}
