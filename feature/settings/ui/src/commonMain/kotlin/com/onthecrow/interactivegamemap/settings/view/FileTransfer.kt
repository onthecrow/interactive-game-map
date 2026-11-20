package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.runtime.Composable

/** Saves a file where the user picks, and reads a file the user picks, with the system's own pickers. */
internal interface FileTransfer {
    fun save(fileName: String, content: String)
    fun open()
}

/**
 * @param onSaved after a save: true when the file is written, false when it couldn't be; nothing when the user cancels
 * @param onOpened the text of the file picked, empty when it couldn't be read; nothing when the user cancels
 */
@Composable
internal expect fun rememberFileTransfer(onSaved: (Boolean) -> Unit, onOpened: (String) -> Unit): FileTransfer
