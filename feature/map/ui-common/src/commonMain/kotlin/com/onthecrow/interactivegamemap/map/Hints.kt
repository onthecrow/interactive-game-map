package com.onthecrow.interactivegamemap.map

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// A hint comes when the screen has settled for this long with nothing over it
private val HINT_SETTLE = 1500.milliseconds

// Between one hint going away and the next one coming, so they don't follow one another
private val HINT_PAUSE = 20.seconds

// A hint shown is on the screen once the event of it is through
private val HINT_SHOW_TIMEOUT = 1.seconds

/**
 * A bubble at a control of the map screen telling what it does, shown once ever. [key] keeps that it has been shown;
 * of two hints that could come at the same moment, the one of the higher [priority] does. When each one makes sense,
 * when it's seen and when the user knows its button already are in [MapScreenState.canShowHint],
 * [MapScreenState.isHintVisible] and [MapScreenState.knowsHint], what takes it away in the reducer.
 */
enum class MapHint(internal val key: String, internal val priority: Int) {
    // Routes are the main feature, their hint goes before the others. Its key is from before there were other hints:
    // who saw it doesn't see it again
    ROUTE("route_hint_shown", priority = 2),
    RECENTER("recenter_hint_shown", priority = 1),
}

/**
 * Hints at the controls of a screen, each shown once ever and never two at a time. A hint comes when it makes sense
 * with no other one on the screen and still does a moment later; when more than one does then, the one of the highest
 * [priority]. A hint doesn't wait for the others: one whose control isn't there never holds back the rest. The next
 * one waits for the last to go away, and a pause after that. A hint whose control the user knows already counts as
 * shown without showing.
 *
 * @param priority the higher, the earlier among hints that could come together; the same one goes by the order of
 * [hints]
 * @param isShown whether a hint has been shown, at a launch before this one too
 * @param setShown keeps that a hint has been shown, before it's on the screen
 */
internal class HintManager<H>(
    hints: List<H>,
    priority: (H) -> Int,
    private val isShown: (H) -> Boolean,
    private val setShown: (H) -> Unit,
    private val settle: Duration = HINT_SETTLE,
    private val pause: Duration = HINT_PAUSE,
) {
    private val hints = hints.sortedByDescending(priority)

    /**
     * Shows the hints not shown yet, one by one, until there are none or the caller is cancelled.
     *
     * @param current the hint on the screen in a state, null for none
     * @param canShow whether a hint makes sense in a state: its control is there and nothing is over the screen
     * @param isKnown whether the user knows the control of a hint in a state already, as by having used it
     * @param show puts a hint on the screen, a state with it as [current] comes after
     */
    suspend fun <S> run(
        states: StateFlow<S>,
        current: (S) -> H?,
        canShow: (S, H) -> Boolean,
        isKnown: (S, H) -> Boolean = { _, _ -> false },
        show: (H) -> Unit,
    ) {
        // By priority, the highest first
        var pending = hints.filterNot(isShown)
        fun dropKnown(state: S) {
            val known = pending.filter { isKnown(state, it) }
            known.forEach(setShown)
            pending = pending - known.toSet()
        }
        // The hint of the highest priority that makes sense in a state with no other one on the screen
        fun next(state: S): H? = if (current(state) == null) pending.firstOrNull { canShow(state, it) } else null

        while (pending.isNotEmpty()) {
            val waiting = pending
            val state = states.first { state ->
                waiting.any { isKnown(state, it) } || current(state) == null && waiting.any { canShow(state, it) }
            }
            dropKnown(state)
            if (next(state) == null) continue
            // The screen settles first, then the hint is noticed coming; a hint of a higher priority that makes sense
            // by then goes first
            delay(settle)
            val now = states.value
            dropKnown(now)
            val hint = next(now) ?: continue
            setShown(hint)
            show(hint)
            pending = pending - hint
            // Gone when it was on the screen and isn't any more; one taken away before it was seen is gone too
            withTimeoutOrNull(HINT_SHOW_TIMEOUT) { states.first { current(it) == hint } }
            states.first { current(it) != hint }
            delay(pause)
        }
    }
}
