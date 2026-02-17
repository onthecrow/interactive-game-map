package com.onthecrow.interactivegamemap.map.di

import com.onthecrow.interactivegamemap.map.MapDestination
import com.onthecrow.interactivegamemap.map.MapScreen
import com.onthecrow.nomadrates.navigation.registerScreen
import org.koin.dsl.module

val mapModule = module {
//    single { CurrencyListReducer() }

//    viewModelOf(::CurrencyListViewModel)

    registerScreen<MapDestination> { _, modifier ->
//        val viewModel: CurrencyListViewModel = koinViewModel()
//        val state by viewModel.state.collectAsStateWithLifecycle()

        MapScreen(modifier = modifier)

//        CurrencyListScreen(
//            state = state,
//            eventFlow = viewModel.eventFlow,
//            modifier = modifier,
//            onEvent = viewModel::onEvent,
//        )
    }
}
