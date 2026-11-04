package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun plainTextClipEntry(text: String) = ClipEntry.withPlainText(text)
