package com.onthecrow.interactivegamemap.ui

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

// The scrims of the navigation bar with buttons, the same as the default ones of enableEdgeToEdge
private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
actual fun AppEnvironment(
    language: String,
    isDarkTheme: Boolean?,
    content: @Composable () -> Unit,
) {
    val isDark = isDarkTheme ?: isSystemInDarkTheme()
    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(activity, isDark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
            navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { isDark },
        )
    }

    val systemConfiguration = LocalConfiguration.current
    val resources = LocalContext.current.resources
    val configuration = remember(systemConfiguration, language, isDarkTheme) {
        val locale = Locale.forLanguageTag(language)
        // Compose resources take the default locale, the strings of Material components take the resources'
        Locale.setDefault(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(
            Configuration(resources.configuration).apply { setLocale(locale) },
            resources.displayMetrics,
        )
        Configuration(systemConfiguration).apply {
            setLocale(locale)
            if (isDarkTheme != null) {
                val nightMode = if (isDarkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
        }
    }

    // isSystemInDarkTheme reads the configuration
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}

actual val isDynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
