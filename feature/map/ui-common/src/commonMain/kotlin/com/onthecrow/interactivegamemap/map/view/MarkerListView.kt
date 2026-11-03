package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapScreenState
import com.onthecrow.interactivegamemap.map.MarkerIconLoader
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapMarkerCategory
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.ui.components.GameCategoryItem
import com.onthecrow.interactivegamemap.ui.components.GameCycleCategory
import com.onthecrow.interactivegamemap.ui.components.GameCycleTab
import com.onthecrow.interactivegamemap.ui.components.GameGroupHeader
import com.onthecrow.interactivegamemap.ui.components.GameSearchField
import com.onthecrow.interactivegamemap.ui.components.GameTextButton
import com.onthecrow.interactivegamemap.ui.components.GameVisibilityActions
import com.onthecrow.interactivegamemap.ui.components.SearchFieldPlacement
import com.onthecrow.interactivegamemap.ui.components.TextButtonGlyph
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_hide_all
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_nothing_found
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_search_hint
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_show_all
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun MarkerListView(
    state: MapScreenState,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
) {
    val categoryItem = GameMapTheme.components.categoryItem
    Column(
        modifier = modifier.fillMaxWidth().imePadding(),
    ) {
        GameSearchField(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            hint = stringResource(Res.string.marker_list_search_hint),
            value = state.markerListQuery,
            onValueChange = { onEvent(MapEvent.OnMarkerListQueryChange(it)) },
            onClearClick = { onEvent(MapEvent.OnMarkerListQueryChange("")) },
            placement = SearchFieldPlacement.Panel,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(categoryItem.spacing),
        ) {
            if (state.isMarkerListSearching) {
                foundCategoryItems(state, onEvent)
            } else {
                groupItems(state, onEvent)
            }
            item {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun LazyListScope.foundCategoryItems(
    state: MapScreenState,
    onEvent: (MapEvent) -> Unit,
) {
    val categories = state.foundCategories
    if (categories.isEmpty()) {
        item {
            Text(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                text = stringResource(Res.string.marker_list_nothing_found),
                textAlign = TextAlign.Center,
                style = GameMapTheme.typography.body,
                color = GameMapTheme.colors.contentSecondary,
            )
        }
    }
    categoryItems(keyPrefix = "found", categories, state, onEvent)
}

private fun LazyListScope.groupItems(
    state: MapScreenState,
    onEvent: (MapEvent) -> Unit,
) {
    item {
        GameVisibilityActions {
            GameTextButton(
                text = stringResource(Res.string.marker_list_show_all),
                glyph = TextButtonGlyph.Confirm,
                onClick = { onEvent(MapEvent.OnCategoriesVisibilityChange(true)) },
            )
            GameTextButton(
                text = stringResource(Res.string.marker_list_hide_all),
                glyph = TextButtonGlyph.Cancel,
                onClick = { onEvent(MapEvent.OnCategoriesVisibilityChange(false)) },
            )
        }
    }
    state.markerGroups.forEach { group ->
        val isCollapsed = state.isGroupCollapsed(group.id)
        item(key = "group-${group.id}") {
            GameGroupHeader(
                title = group.name,
                description = group.description,
                expanded = !isCollapsed,
                onClick = { onEvent(MapEvent.OnGroupClick(group.id)) },
            )
        }
        if (!isCollapsed) {
            categoryItems(keyPrefix = group.id, group.categories, state, onEvent)
        }
    }
}

private fun LazyListScope.categoryItems(
    keyPrefix: String,
    categories: List<MapMarkerCategory>,
    state: MapScreenState,
    onEvent: (MapEvent) -> Unit,
) {
    val (cycleCategories, plainCategories) = categories.partition { it.category.hasCycles }
    items(
        items = plainCategories.chunked(2),
        key = { row -> "$keyPrefix-${row.first().category.id}" },
    ) { row ->
        val categoryItem = GameMapTheme.components.categoryItem
        Row(
            modifier = Modifier.padding(horizontal = categoryItem.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(categoryItem.spacing),
        ) {
            row.forEach { category ->
                CategoryView(
                    modifier = Modifier.weight(1f),
                    category = category,
                    count = category.count,
                    isVisible = state.isCategoryVisible(category.category),
                    onClick = { onEvent(MapEvent.OnCategoryClick(category.category.id)) },
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
    items(
        items = cycleCategories,
        key = { category -> "$keyPrefix-${category.category.id}" },
    ) { category ->
        val selectedCycle = state.selectedCycle(category.category.id)
        GameCycleCategory(
            modifier = Modifier.padding(horizontal = GameMapTheme.components.categoryItem.horizontalPadding),
        ) {
            CategoryView(
                modifier = Modifier.weight(1f),
                category = category,
                count = category.countByCycle[selectedCycle] ?: 0,
                isVisible = state.isCategoryVisible(category.category),
                standalone = false,
                onClick = { onEvent(MapEvent.OnCategoryClick(category.category.id)) },
            )
            LocationsFilter.CYCLES.forEach { cycle ->
                GameCycleTab(
                    text = cycle.toString(),
                    selected = cycle == selectedCycle,
                    onClick = { onEvent(MapEvent.OnCycleClick(category.category.id, cycle)) },
                )
            }
        }
    }
}

@Composable
private fun CategoryView(
    category: MapMarkerCategory,
    count: Int,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    standalone: Boolean = true,
    onClick: () -> Unit = {},
) {
    GameCategoryItem(
        modifier = modifier,
        // Some names hold non-breaking spaces, which would break them inside a word instead
        name = category.category.name.replace('\u00A0', ' '),
        count = count.toString().takeIf { category.category.showsCount },
        visible = isVisible,
        standalone = standalone,
        onClick = onClick,
    ) {
        MarkerIconView(modifier = Modifier.fillMaxSize(), icon = category.category.icon)
    }
}

@Composable
private fun MarkerIconView(
    icon: MarkerIcon,
    modifier: Modifier = Modifier,
) {
    val iconLoader = koinInject<MarkerIconLoader>()
    val image by produceState<ImageBitmap?>(null, icon.url) {
        value = iconLoader.load(icon.url)
    }

    Box(modifier = modifier) {
        image?.let { bitmap ->
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = BitmapPainter(
                    image = bitmap,
                    srcOffset = IntOffset(icon.x ?: 0, icon.y ?: 0),
                    srcSize = IntSize(icon.width ?: bitmap.width, icon.height ?: bitmap.height),
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }
}
