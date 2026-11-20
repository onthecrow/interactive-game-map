package com.onthecrow.interactivegamemap.settings.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val JSON_MIME_TYPE = "application/json"

// A file sent through a messenger may come without its type, so the picker offers more than JSON
private val OPEN_MIME_TYPES = arrayOf(JSON_MIME_TYPE, "text/plain", "application/octet-stream")

/** The system's document pickers: a new document is created where the user picks, any document is read. */
@Composable
internal actual fun rememberFileTransfer(onSaved: (Boolean) -> Unit, onOpened: (String) -> Unit): FileTransfer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnSaved by rememberUpdatedState(onSaved)
    val currentOnOpened by rememberUpdatedState(onOpened)
    // The content waits for the place the user picks
    var pendingContent by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
        val content = pendingContent
        pendingContent = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        scope.launch {
            val isSaved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.encodeToByteArray()) } != null
                }.getOrDefault(false)
            }
            currentOnSaved(isSaved)
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            currentOnOpened(content.orEmpty())
        }
    }

    return remember(saveLauncher, openLauncher) {
        object : FileTransfer {
            override fun save(fileName: String, content: String) {
                pendingContent = content
                saveLauncher.launch(fileName)
            }

            override fun open() {
                openLauncher.launch(OPEN_MIME_TYPES)
            }
        }
    }
}
