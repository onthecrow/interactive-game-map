package com.onthecrow.interactivegamemap.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long a snackbar stays when it isn't touched. */
val SnackbarDuration = 3.seconds

// A snackbar dragged up by this part of its height, or flung up this fast, goes away
private const val SWIPE_AWAY_PART = 0.3f
private const val SWIPE_AWAY_VELOCITY = 800f

private const val ENTER_MILLIS = 300
private const val EXIT_MILLIS = 220

/**
 * A short message at the top of the screen, over every screen of the app, see [GameSnackbars].
 *
 * @param formatArgs the arguments of [message], as of `stringResource`
 */
@Immutable
data class GameSnackbar(
    val message: StringResource,
    val formatArgs: List<Any> = emptyList(),
    val duration: Duration = SnackbarDuration,
)

/**
 * The snackbars of the app, shown by [GameSnackbarHost] over every screen one at a time, in the order they come; any
 * screen can post one, from its view model too. The one on the screen stays first in the queue until it has gone, so
 * when the screens are made again, as at a turn of an Android phone, it comes again.
 */
class GameSnackbars {
    private val _queue = MutableStateFlow<List<Entry>>(emptyList())
    internal val queue: StateFlow<List<Entry>> = _queue.asStateFlow()

    fun show(snackbar: GameSnackbar) {
        _queue.update { it + Entry(snackbar) }
    }

    internal fun remove(entry: Entry) {
        _queue.update { it - entry }
    }

    /** One posting of a snackbar: the same snackbar posted twice is two of them. */
    internal class Entry(val snackbar: GameSnackbar)
}

/**
 * The snackbar on the screen, until its time is up or it's swiped away. Its time doesn't run while the user holds it;
 * once it's let go it starts again.
 */
internal class ShownSnackbar(val entry: GameSnackbars.Entry) {
    val isHeld = MutableStateFlow(false)
    val isSwipedAway = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun awaitEnd() {
        combine(isHeld, isSwipedAway) { held, swipedAway -> held to swipedAway }
            .transformLatest { (held, swipedAway) ->
                when {
                    swipedAway -> emit(Unit)
                    !held -> {
                        delay(entry.snackbar.duration)
                        emit(Unit)
                    }
                }
            }
            .first()
    }
}

/**
 * The snackbars of [snackbars] at the top of the screen, in the look of the popups of the game: they slide in from
 * above it and go back up, a swipe up takes one away. Over every screen, under the shade of the status bar.
 */
@Composable
fun GameSnackbarHost(snackbars: GameSnackbars, modifier: Modifier = Modifier) {
    val queue by snackbars.queue.collectAsState()
    val first = queue.firstOrNull()
    val visibility = remember { MutableTransitionState(false) }
    // Stays while the snackbar slides away
    var shown by remember { mutableStateOf<ShownSnackbar?>(null) }

    LaunchedEffect(first) {
        val entry = first ?: return@LaunchedEffect
        val snackbar = ShownSnackbar(entry)
        shown = snackbar
        visibility.targetState = true
        snackbar.awaitEnd()
        visibility.targetState = false
        // The next one comes once this one is gone
        snapshotFlow { visibility.isIdle && !visibility.currentState }.first { it }
        snackbars.remove(entry)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visibleState = visibility,
            // From above the screen: the margins over it slide with it
            enter = slideInVertically(tween(ENTER_MILLIS)) { -it } + fadeIn(tween(ENTER_MILLIS)),
            exit = slideOutVertically(tween(EXIT_MILLIS)) { -it } + fadeOut(tween(EXIT_MILLIS)),
        ) {
            shown?.let { SnackbarPanel(it) }
        }
    }
}

@Composable
private fun SnackbarPanel(snackbar: ShownSnackbar) {
    val data = snackbar.entry.snackbar
    var offset by remember(snackbar) { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
    ) {
        GamePanel(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .onSizeChanged { height = it.height }
                // Dragged by where it was, so it follows the finger as it moves; only up
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> offset = (offset + delta).coerceAtMost(0f) },
                    onDragStarted = { snackbar.isHeld.value = true },
                    onDragStopped = { velocity ->
                        if (offset < -height * SWIPE_AWAY_PART || velocity < -SWIPE_AWAY_VELOCITY) {
                            snackbar.isSwipedAway.value = true
                        } else {
                            animate(initialValue = offset, targetValue = 0f) { value, _ -> offset = value }
                        }
                        snackbar.isHeld.value = false
                    },
                )
                .graphicsLayer { translationY = offset }
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = stringResource(data.message, *data.formatArgs.toTypedArray()),
                style = GameMapTheme.typography.title,
                color = GameMapTheme.colors.content,
                textAlign = TextAlign.Center,
            )
        }
    }
}
