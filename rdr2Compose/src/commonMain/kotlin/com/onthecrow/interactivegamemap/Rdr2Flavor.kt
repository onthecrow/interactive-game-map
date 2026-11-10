package com.onthecrow.interactivegamemap

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.di.mapLogicModule
import com.onthecrow.interactivegamemap.ui.rdr2.di.rdr2UiModule

/** MapSlinger, the Red Dead Redemption 2 map: the data in the build, the look of the game's map. */
val rdr2Flavor = FlavorConfig(
    privacyPolicyUrl = "https://onthecrow.github.io/interactive-game-map/rdr2/privacy-policy/",
    // The areas of an animal in the red of the guides' circles and of the selection
    mapStyle = MapStyle(backgroundColor = Color(0xFFD3B790), areaColor = Color(0xFFB5161B)),
    uiModule = rdr2UiModule,
    logicModule = mapLogicModule,
)
