package com.onthecrow.interactivegamemap.map.view

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapScreenState
import com.onthecrow.interactivegamemap.map.findDungeon
import com.onthecrow.interactivegamemap.ui.components.GameMapButton
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.route_next
import interactivegamemap.feature.map.ui_common.generated.resources.route_position
import interactivegamemap.feature.map.ui_common.generated.resources.route_previous
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// The popups next to the current one peek in from the sides by this much less the space between them
private val PeekPadding = 32.dp
private val PageSpacing = 8.dp
private val PopupMargin = 16.dp
// The arrows sit in the bottom corners of the screen, over the edges of the popups
private val ArrowsMargin = 4.dp

// A drag faster than this, in dp a second, goes on to the next popup even when it moved less than half of one
private const val FLING_VELOCITY = 400f

// A drag past the first or the last popup gives way this far, in popups
private const val OVERSCROLL = 0.25f

/**
 * The popups of the locations of the route side by side: the current one in the middle, the edges of the previous
 * and the next ones at the sides. A drag of a popup, an arrow or a tap on a side one goes to another location, the
 * camera follows; Found goes on to the next one.
 *
 * Only the popups take touches: the space around them, above a lower one too, belongs to the map under them. That's
 * why it's a layout of its own and not a pager, which takes the touches of all of its area.
 */
@Composable
internal fun RoutePagerView(
    state: MapScreenState,
    onEvent: (MapEvent) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locations = state.routeLocations
    val page = state.routePage
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentPage by rememberUpdatedState(page)
    val scope = rememberCoroutineScope()
    val scroll = remember { PagesScroll(page.toFloat(), scope) }
    scroll.pageCount = locations.size

    // The page of the state, from an arrow, Found or the end of a drag, brings its popup to the middle
    LaunchedEffect(page) {
        if (scroll.position != page.toFloat()) scroll.animateTo(page, velocity = 0f)
    }

    // The popups on the screen and one past each side, so a drag finds them there
    val visible by remember {
        derivedStateOf {
            val first = floor(scroll.position).toInt() - 1
            val last = ceil(scroll.position).toInt() + 1
            (first..last).filter { it in 0 until scroll.pageCount }
        }
    }
    val onSettled: (Int) -> Unit = { target ->
        if (target != currentPage) currentOnEvent(MapEvent.OnRoutePageChange(target))
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Layout(
            modifier = Modifier.fillMaxWidth().padding(vertical = PopupMargin),
            content = {
                visible.forEach { index ->
                    val location = locations[index]
                    key(location.id) {
                        Box(modifier = Modifier.dragsPages(scroll, onSettled)) {
                            LocationView(
                                title = location.name,
                                description = location.description,
                                floors = state.currentMap.findDungeon(location)?.floors.orEmpty(),
                                selectedFloorId = state.selectedFloorId.takeIf { index == page },
                                locationFloorId = location.dungeon?.floorId,
                                isFound = location.id in state.foundLocationIds,
                                position = stringResource(Res.string.route_position, index + 1, locations.size),
                                onFloorClick = { onEvent(MapEvent.OnFloorClick(it)) },
                                onExitClick = { onEvent(MapEvent.OnLocationDismiss) },
                                onFoundClick = { onEvent(MapEvent.OnFoundClick(location.id)) },
                                onLinkClick = onLinkClick,
                            )
                            // A popup at the side only comes to the middle, its buttons wait until it's there
                            if (index != page) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onEvent(MapEvent.OnRoutePageChange(index)) },
                                        ),
                                )
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val peek = PeekPadding.roundToPx()
            val spacing = PageSpacing.roundToPx()
            val width = constraints.maxWidth
            val cardWidth = (width - 2 * peek).coerceAtLeast(0)
            scroll.pageSize = (cardWidth + spacing).toFloat().coerceAtLeast(1f)
            val cards = measurables.map { it.measure(Constraints(minWidth = cardWidth, maxWidth = cardWidth)) }
            val height = cards.maxOfOrNull { it.height } ?: 0
            val indices = visible
            layout(width, height) {
                // Read here, a drag moves the popups without composing them again
                val position = scroll.position
                cards.forEachIndexed { k, card ->
                    val index = indices.getOrNull(k) ?: return@forEachIndexed
                    val x = peek + ((index - position) * scroll.pageSize).roundToInt()
                    card.place(x, height - card.height)
                }
            }
        }
        GameMapButton(
            modifier = Modifier.align(Alignment.BottomStart).padding(ArrowsMargin),
            icon = GameMapTheme.icons.previous,
            contentDescription = stringResource(Res.string.route_previous),
            enabled = page > 0,
            onClick = { onEvent(MapEvent.OnRoutePageChange(page - 1)) },
        )
        GameMapButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(ArrowsMargin),
            icon = GameMapTheme.icons.next,
            contentDescription = stringResource(Res.string.route_next),
            enabled = page < locations.lastIndex,
            onClick = { onEvent(MapEvent.OnRoutePageChange(page + 1)) },
        )
    }
}

/** Where the popups are, in popups from the first one: a drag moves it, an animation brings it to a whole one. */
private class PagesScroll(initial: Float, private val scope: CoroutineScope) {
    var position by mutableFloatStateOf(initial)
        private set
    var pageCount = 0
    /** The width of a popup and the space after it, in pixels. */
    var pageSize = 1f
    private var animation: Job? = null

    fun stop() {
        animation?.cancel()
    }

    fun dragBy(pixels: Float) {
        position = (position + pixels / pageSize).coerceIn(-OVERSCROLL, pageCount - 1 + OVERSCROLL)
    }

    /** @param velocity of the popups, in pixels a second toward the next ones */
    fun animateTo(target: Int, velocity: Float, onEnd: () -> Unit = {}) {
        animation?.cancel()
        animation = scope.launch {
            animate(
                initialValue = position,
                targetValue = target.toFloat(),
                initialVelocity = velocity / pageSize,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> position = value }
            onEnd()
        }
    }
}

/**
 * A drag of this popup moves all of them; when it ends, the nearest one comes to the middle, or the next one the way
 * of a fast drag, and [onSettled] gets it.
 *
 * The popup moves under the finger as it drags, so the drag is measured in the root's coordinates.
 */
private fun Modifier.dragsPages(scroll: PagesScroll, onSettled: (Int) -> Unit): Modifier {
    val placed = Placed()
    return onGloballyPositioned { placed.coordinates = it }.pointerInput(scroll) {
        val flingVelocity = FLING_VELOCITY.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                ?: return@awaitEachGesture
            fun inRoot(change: PointerInputChange): Offset =
                placed.coordinates?.takeIf { it.isAttached }?.localToRoot(change.position) ?: change.position

            scroll.stop()
            val start = scroll.position
            var last = inRoot(drag)
            val tracker = VelocityTracker().apply { addPosition(drag.uptimeMillis, last) }
            horizontalDrag(drag.id) { change ->
                val position = inRoot(change)
                scroll.dragBy(last.x - position.x)
                last = position
                tracker.addPosition(change.uptimeMillis, position)
                change.consume()
            }
            // A finger moving left goes on to the next popup
            val velocity = tracker.calculateVelocity().x
            val position = scroll.position
            val target = when {
                velocity < -flingVelocity && position >= start -> floor(position).toInt() + 1
                velocity > flingVelocity && position <= start -> ceil(position).toInt() - 1
                else -> position.roundToInt()
            }.coerceIn(0, (scroll.pageCount - 1).coerceAtLeast(0))
            scroll.animateTo(target, velocity = -velocity) { onSettled(target) }
        }
    }
}

private class Placed {
    var coordinates: LayoutCoordinates? = null
}
