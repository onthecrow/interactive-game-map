package com.onthecrow.interactivegamemap.settings

/**
 * The settings an app offers beyond the ones of every app, given by the app for its game.
 *
 * @param hasMapStyle whether the game has a dark map, picked by [AppSettings.mapStyle]
 */
class SettingsOptions(val hasMapStyle: Boolean = false)
