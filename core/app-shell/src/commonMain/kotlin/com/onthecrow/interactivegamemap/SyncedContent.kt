package com.onthecrow.interactivegamemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onthecrow.interactivegamemap.sync.SyncPhase
import com.onthecrow.interactivegamemap.sync.SyncState
import com.onthecrow.interactivegamemap.sync.Synchronizer
import com.onthecrow.interactivegamemap.ui.components.ContentPlaceholder
import com.onthecrow.interactivegamemap.ui.components.UpdateToast
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// A quick update passes without a toast; a shown one stays long enough to be read
private val ToastShowDelay = 300.milliseconds
private val ToastMinVisible = 1.seconds
private val ToastDoneVisible = 1500.milliseconds

/**
 * [content] once the app has data to show. Before that, the placeholder with the progress of the first load, or
 * its failure and a retry. The updates of data already there show a toast over [content].
 */
@Composable
fun SyncedContent(synchronizer: Synchronizer, content: @Composable () -> Unit) {
    val state by synchronizer.state.collectAsStateWithLifecycle()
    when (state.hasLocalData) {
        // The splash screen is kept on Android meanwhile; it's a moment
        null -> Box(Modifier.fillMaxSize().background(GameMapTheme.colors.background))
        false -> ContentPlaceholder(
            failed = state.phase is SyncPhase.Failed,
            progress = (state.phase as? SyncPhase.Updating)?.progress,
            onRetry = { synchronizer.syncNow(force = true) },
        )
        true -> Box(Modifier.fillMaxSize()) {
            content()
            UpdateToastHost(state)
        }
    }
}

@Composable
private fun UpdateToastHost(state: SyncState) {
    val current by rememberUpdatedState(state)
    val isUpdating = state.phase is SyncPhase.Updating
    var visible by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var shownAt by remember { mutableStateOf<TimeMark?>(null) }

    LaunchedEffect(isUpdating) {
        if (isUpdating) {
            done = false
            delay(ToastShowDelay)
            visible = true
            shownAt = TimeSource.Monotonic.markNow()
            return@LaunchedEffect
        }
        val shown = shownAt ?: return@LaunchedEffect
        shownAt = null
        if (!visible) return@LaunchedEffect
        val left = ToastMinVisible - shown.elapsedNow()
        if (left.isPositive()) delay(left)
        // A failed update leaves the data as it was, there is nothing to tell
        if (current.phase !is SyncPhase.Failed) {
            done = true
            delay(ToastDoneVisible)
        }
        visible = false
    }

    UpdateToast(
        visible = visible,
        progress = (state.phase as? SyncPhase.Updating)?.progress,
        done = done,
        onDismiss = { visible = false },
    )
}
