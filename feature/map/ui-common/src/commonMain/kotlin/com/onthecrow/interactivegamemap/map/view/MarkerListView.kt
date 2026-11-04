package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.map.MapEvent
import com.onthecrow.interactivegamemap.map.MapScreenState
import com.onthecrow.interactivegamemap.map.MarkerIconLoader
import com.onthecrow.interactivegamemap.map.model.LocationsFilter
import com.onthecrow.interactivegamemap.map.model.MapMarkerCategory
import com.onthecrow.interactivegamemap.map.model.MapMarkerGroup
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.ui.view.SearchFieldView
import interactivegamemap.feature.map.ui_common.generated.resources.Res
import interactivegamemap.feature.map.ui_common.generated.resources.ic_expand_more
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_hide_all
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_nothing_found
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_search_hint
import interactivegamemap.feature.map.ui_common.generated.resources.marker_list_show_all
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject

private const val HIDDEN_CATEGORY_ALPHA = 0.5f

@Composable
internal fun MarkerListView(
    state: MapScreenState,
    modifier: Modifier = Modifier,
    onEvent: (MapEvent) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().imePadding(),
    ) {
        SearchFieldView(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            hint = stringResource(Res.string.marker_list_search_hint),
            value = state.markerListQuery,
            onValueChange = { onEvent(MapEvent.OnMarkerListQueryChange(it)) },
            onClearClick = { onEvent(MapEvent.OnMarkerListQueryChange("")) },
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                style = MaterialTheme.typography.bodyMedium,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = { onEvent(MapEvent.OnCategoriesVisibilityChange(true)) }) {
                Text(stringResource(Res.string.marker_list_show_all))
            }
            TextButton(onClick = { onEvent(MapEvent.OnCategoriesVisibilityChange(false)) }) {
                Text(stringResource(Res.string.marker_list_hide_all))
            }
        }
    }
    state.markerGroups.forEach { group ->
        val isCollapsed = state.isGroupCollapsed(group.id)
        item(key = "group-${group.id}") {
            GroupHeaderView(
                group = group,
                isCollapsed = isCollapsed,
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
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
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
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryView(
                modifier = Modifier.weight(1f),
                category = category,
                count = category.countByCycle[selectedCycle] ?: 0,
                isVisible = state.isCategoryVisible(category.category),
                onClick = { onEvent(MapEvent.OnCategoryClick(category.category.id)) },
            )
            LocationsFilter.CYCLES.forEach { cycle ->
                CycleView(
                    cycle = cycle,
                    isSelected = cycle == selectedCycle,
                    onClick = { onEvent(MapEvent.OnCycleClick(category.category.id, cycle)) },
                )
            }
        }
    }
}

@Composable
private fun GroupHeaderView(
    group: MapMarkerGroup,
    isCollapsed: Boolean,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
            )
            group.description?.takeIf { !isCollapsed }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Icon(
            modifier = Modifier.rotate(if (isCollapsed) 0f else 180f),
            imageVector = vectorResource(Res.drawable.ic_expand_more),
            contentDescription = null,
        )
    }
}

@Composable
private fun CategoryView(
    category: MapMarkerCategory,
    count: Int,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .alpha(if (isVisible) 1f else HIDDEN_CATEGORY_ALPHA)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarkerIconView(icon = category.category.icon)
        Text(
            modifier = Modifier.weight(1f),
            text = category.category.name,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (isVisible) null else TextDecoration.LineThrough,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CycleView(
    cycle: Int,
    isSelected: Boolean,
    onClick: () -> Unit = {},
) {
    Text(
        modifier = Modifier
            .size(width = 28.dp, height = 40.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        text = cycle.toString(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (isSelected) FontWeight.Bold else null,
        textDecoration = if (isSelected) TextDecoration.Underline else null,
    )
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

    Box(modifier = modifier.size(24.dp)) {
        image?.let { bitmap ->
            Image(
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
