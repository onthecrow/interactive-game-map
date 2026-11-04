package com.onthecrow.interactivegamemap.settings

import com.onthecrow.nomadrates.uicore.Event

internal sealed interface SettingsEvent : Event {
    data object OnBackClick : SettingsEvent
    data class OnHideMarkersWhileMovingChange(val enabled: Boolean) : SettingsEvent
    data class OnClusterMarkersChange(val enabled: Boolean) : SettingsEvent
    data object OnThemeClick : SettingsEvent
    data class OnThemeSelect(val theme: ThemeMode) : SettingsEvent
    data class OnDynamicColorChange(val enabled: Boolean) : SettingsEvent
    data object OnLanguageClick : SettingsEvent
    /** @param language null to follow the system */
    data class OnLanguageSelect(val language: String?) : SettingsEvent
    data object OnPrivacyPolicyClick : SettingsEvent
    data object OnDialogDismiss : SettingsEvent
    data class OnSettingsChanged(val settings: AppSettings) : SettingsEvent
    data class OnSystemLanguageChanged(val language: String) : SettingsEvent
}
