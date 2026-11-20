package com.onthecrow.interactivegamemap

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.MapFeatures
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.TileColorMatrix
import com.onthecrow.interactivegamemap.map.di.mapLogicModule
import com.onthecrow.interactivegamemap.ui.rdr2.di.rdr2UiModule

// The paper of the game's map at the edges of its tiles. Above the flavor, which is set up after it
private val MapPaper = Color(0xFFD3B790)

/** MapSlinger, the Red Dead Redemption 2 map: the data in the build, the look of the game's map, routes on its roads. */
val rdr2Flavor = FlavorConfig(
    gameId = "rdr2",
    privacyPolicyUrl = "https://onthecrow.github.io/interactive-game-map/rdr2/privacy-policy/",
    // The areas of an animal and the routes in the red of the guides' circles and of the selection
    mapStyle = MapStyle(
        backgroundColor = MapPaper,
        areaColor = Color(0xFFB5161B),
        // The map at night: dark paper and pale ink, in the warm greys of the dark theme
        darkTiles = TileColorMatrix.byBrightness(
            light = MapPaper to Color(0xFF34312D),
            dark = Color(0xFF3A3530) to Color(0xFFBEB7AC),
        ),
    ),
    uiModule = rdr2UiModule,
    logicModule = mapLogicModule,
    mapFeatures = MapFeatures(routes = true),
)
