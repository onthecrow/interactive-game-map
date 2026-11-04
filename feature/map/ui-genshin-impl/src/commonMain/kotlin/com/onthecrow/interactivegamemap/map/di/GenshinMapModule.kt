package com.onthecrow.interactivegamemap.map.di

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.MapTileProvider
import org.koin.dsl.module

val genshinMapModule = module {
    single { MapTileProvider(get()) }
    single { MapStyle(backgroundColor = Color(0xFF000000)) }
}
