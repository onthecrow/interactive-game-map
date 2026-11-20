package com.onthecrow.interactivegamemap

import androidx.compose.ui.graphics.Color
import com.onthecrow.interactivegamemap.map.MapStyle
import com.onthecrow.interactivegamemap.map.di.mapLogicModule
import com.onthecrow.interactivegamemap.ui.genshin.di.genshinUiModule

/** The Genshin Impact map: the data of HoYoLAB's map, the game's look. */
val genshinFlavor = FlavorConfig(
    privacyPolicyUrl = "https://onthecrow.github.io/interactive-game-map/genshin/privacy-policy/",
    mapStyle = MapStyle(backgroundColor = Color(0xFF000000)),
    uiModule = genshinUiModule,
    logicModule = mapLogicModule,
    // HoYoverse lets fans use the game's materials for free with this notice, see its Legal FAQ
    legalNotice = listOf(
        "© All rights reserved by COGNOSPHERE. Other properties belong to their respective owners.",
        "An unofficial fan app, not affiliated with or endorsed by HoYoverse.",
    ),
)
