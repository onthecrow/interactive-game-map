package com.onthecrow.interactivegamemap.settings

import com.onthecrow.interactivegamemap.map.FoundLocationsExport
import com.onthecrow.nomadrates.uicore.State

internal data class SettingsState(
    val settings: AppSettings = AppSettings(),
    /** The language following the system gives. */
    val systemLanguage: String = DEFAULT_LANGUAGE,
    /** The choice open in a dialog, null when none is. */
    val dialog: SettingsDialog? = null,
    /** The id the analytics and the crash reports go with, the user can copy it to tell it. */
    val analyticsId: String = "",
    /** The found markers of the game, the export saves them. */
    val foundCount: Int = 0,
    /** The file of found markers waiting to be saved where the user picks. */
    val pendingExport: FoundLocationsExport? = null,
    /** How the last export or import went, shown for a moment. */
    val transferMessage: TransferMessage? = null,
) : State

internal enum class SettingsDialog { THEME, MAP_STYLE, LANGUAGE }

internal sealed interface TransferMessage {
    data class Exported(val count: Int) : TransferMessage
    data object ExportFailed : TransferMessage
    data class Imported(val added: Int, val total: Int) : TransferMessage
    data object NotAFile : TransferMessage
    data object OtherGame : TransferMessage

    /** Under which of the two settings it's shown. */
    val isExport: Boolean
        get() = this is Exported || this is ExportFailed
}
