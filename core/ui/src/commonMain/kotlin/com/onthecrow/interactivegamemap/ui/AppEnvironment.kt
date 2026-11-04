package com.onthecrow.interactivegamemap.ui

import androidx.compose.runtime.Composable

/**
 * Applies the language and the theme of the app to [content] in place of the system's ones, the system bars
 * follow the theme too. The strings don't follow a change of the language by themselves, the screens are
 * recreated for it by NavigationProvider.
 *
 * @param language a language tag, like `en` or `zh`
 * @param isDarkTheme null to follow the system
 */
@Composable
expect fun AppEnvironment(
    language: String,
    isDarkTheme: Boolean?,
    content: @Composable () -> Unit,
)

/** Whether the system has colors of its own for the theme, Android 12 and later. */
expect val isDynamicColorSupported: Boolean
