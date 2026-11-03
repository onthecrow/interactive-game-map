package com.onthecrow.interactivegamemap.settings.view

import androidx.compose.ui.platform.ClipEntry

/** A text to copy, each platform makes its own. */
internal expect fun plainTextClipEntry(text: String): ClipEntry
