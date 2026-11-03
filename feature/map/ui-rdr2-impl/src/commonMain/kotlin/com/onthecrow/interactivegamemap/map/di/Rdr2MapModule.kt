package com.onthecrow.interactivegamemap.map.di

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.MapTileProvider
import org.koin.dsl.module

val rdr2MapModule = module {
    single { MapTileProvider(get()) }
    // The areas of an animal in the red of the guides' circles and of the selection
    single { MapStyle(backgroundColor = Color(0xFFD3B790), areaColor = Color(0xFFB5161B)) }
}
