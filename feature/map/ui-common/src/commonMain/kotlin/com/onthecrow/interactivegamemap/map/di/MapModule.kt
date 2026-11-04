package com.onthecrow.interactivegamemap.map.di

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onthecrow.interactivegamemap.map.MapDestination
import com.onthecrow.interactivegamemap.map.MapPreferences
import com.onthecrow.interactivegamemap.map.MapReducer
import com.onthecrow.interactivegamemap.map.MapScreen
import com.onthecrow.interactivegamemap.map.view.MapView
import com.onthecrow.interactivegamemap.map.MapViewModel
import com.onthecrow.interactivegamemap.map.MarkerIconLoader
import com.onthecrow.interactivegamemap.map.MarkerPinLoader
import com.onthecrow.nomadrates.navigation.registerScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mapModule = module {
    includes(mapPlatformModule)
    single { MapPreferences(get()) }
    single { MapReducer() }
    single { MarkerIconLoader(get()) }
    single { MarkerPinLoader(get()) }
    viewModelOf(::MapViewModel)

    registerScreen<MapDestination> { _, modifier ->
        val viewModel: MapViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        MapScreen(
            modifier = modifier,
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}
