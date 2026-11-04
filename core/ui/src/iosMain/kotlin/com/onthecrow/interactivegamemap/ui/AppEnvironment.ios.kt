package com.onthecrow.interactivegamemap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

private const val KEY_APPLE_LANGUAGES = "AppleLanguages"

@Composable
actual fun AppEnvironment(
    language: String,
    isDarkTheme: Boolean?,
    content: @Composable () -> Unit,
) {
    // Compose resources take the first of the preferred languages, which come from AppleLanguages
    remember(language) {
        NSUserDefaults.standardUserDefaults.setObject(listOf(language), forKey = KEY_APPLE_LANGUAGES)
    }

    // The windows pass the style to the status bar and to isSystemInDarkTheme
    LaunchedEffect(isDarkTheme) {
        val style = when (isDarkTheme) {
            true -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            false -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            null -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
        }
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows }
            .filterIsInstance<UIWindow>()
            .forEach { it.overrideUserInterfaceStyle = style }
    }

    content()
}

actual val isDynamicColorSupported: Boolean
    get() = false
