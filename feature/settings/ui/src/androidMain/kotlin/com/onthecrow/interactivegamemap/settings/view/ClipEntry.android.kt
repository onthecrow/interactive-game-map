package com.onthecrow.interactivegamemap.settings.view

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

internal actual fun plainTextClipEntry(text: String) = ClipEntry(ClipData.newPlainText(null, text))
