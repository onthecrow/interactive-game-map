package com.onthecrow.interactivegamemap.map.di

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.MapTileProvider
import org.koin.dsl.module

val rdr2MapModule = module {
    single { MapTileProvider(get()) }
    single { MapStyle(backgroundColor = Color(0xFFD3B790)) }
}
