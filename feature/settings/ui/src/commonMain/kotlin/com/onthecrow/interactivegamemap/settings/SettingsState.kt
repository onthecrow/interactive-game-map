package com.onthecrow.interactivegamemap.settings

import com.onthecrow.nomadrates.uicore.State

internal data class SettingsState(
    val settings: AppSettings = AppSettings(),
    /** The language following the system gives. */
    val systemLanguage: String = DEFAULT_LANGUAGE,
    /** The choice open in a dialog, null when none is. */
    val dialog: SettingsDialog? = null,
    /** The id the analytics and the crash reports go with, the user can copy it to tell it. */
    val analyticsId: String = "",
) : State

internal enum class SettingsDialog { THEME, LANGUAGE }
