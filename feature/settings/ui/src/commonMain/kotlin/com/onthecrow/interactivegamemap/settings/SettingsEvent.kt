package com.onthecrow.interactivegamemap.settings

import com.onthecrow.interactivegamemap.map.FoundLocationsExport
import com.onthecrow.nomadrates.uicore.Event

internal sealed interface SettingsEvent : Event {
    data object OnBackClick : SettingsEvent
    data class OnHideMarkersWhileMovingChange(val enabled: Boolean) : SettingsEvent
    data class OnClusterMarkersChange(val enabled: Boolean) : SettingsEvent
    data object OnThemeClick : SettingsEvent
    data class OnThemeSelect(val theme: ThemeMode) : SettingsEvent
    data object OnMapStyleClick : SettingsEvent
    data class OnMapStyleSelect(val mapStyle: MapStyleMode) : SettingsEvent
    data class OnDynamicColorChange(val enabled: Boolean) : SettingsEvent
    data object OnLanguageClick : SettingsEvent
    /** @param language null to follow the system */
    data class OnLanguageSelect(val language: String?) : SettingsEvent
    data object OnDialogDismiss : SettingsEvent
    data class OnSettingsChanged(val settings: AppSettings) : SettingsEvent
    data class OnSystemLanguageChanged(val language: String) : SettingsEvent
    data class OnAnalyticsIdLoaded(val id: String) : SettingsEvent
    data class OnFoundCountChanged(val count: Int) : SettingsEvent
    data object OnExportClick : SettingsEvent
    data class OnExportReady(val export: FoundLocationsExport) : SettingsEvent
    /** The screen has asked where to save [OnExportReady]'s file; [isSaved] comes once it's written. */
    data object OnExportHandedOver : SettingsEvent
    data class OnExportSaved(val isSaved: Boolean) : SettingsEvent
    /** The text of the file the user picked to import. */
    data class OnImportFileRead(val content: String) : SettingsEvent
    data class OnTransferMessage(val message: TransferMessage?) : SettingsEvent
}
