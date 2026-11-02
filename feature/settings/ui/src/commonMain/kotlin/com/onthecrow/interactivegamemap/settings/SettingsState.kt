package com.onthecrow.interactivegamemap.settings

import com.onthecrow.nomadrates.uicore.State

internal data class SettingsState(
    val settings: AppSettings = AppSettings(),
    /** The language following the system gives. */
    val systemLanguage: String = DEFAULT_LANGUAGE,
    /** The choice open in a dialog, null when none is. */
    val dialog: SettingsDialog? = null,
) : State

internal enum class SettingsDialog { THEME, LANGUAGE }
