package com.onthecrow.interactivegamemap

import com.onthecrow.interactivegamemap.map.MapStyle
import org.koin.core.module.Module

/**
 * What makes an app of the shell the app of its game; everything else is shared.
 *
 * @param uiModule the look of the game for GameMapTheme, its core:ui-<game> module
 * @param logicModule the data of the game: its repositories and its sync task, its logic-<game>-impl module
 * @param legalNotice the lines the settings show under the links, like the copyright of the game's owner
 */
class FlavorConfig(
    val privacyPolicyUrl: String,
    val mapStyle: MapStyle,
    val uiModule: Module,
    val logicModule: Module,
    val legalNotice: List<String> = emptyList(),
)
