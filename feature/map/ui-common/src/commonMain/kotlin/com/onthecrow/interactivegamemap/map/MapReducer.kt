package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.nomadrates.uicore.Reducer

internal class MapReducer: Reducer<MapScreenState, MapEvent> {
    override suspend fun reduce(
        state: MapScreenState,
        event: MapEvent
    ): MapScreenState {
        return when (event) {
            is MapEvent.OnClearSearchClick -> state.copy(searchValue = "")
            is MapEvent.OnSearchValueChange -> state.copy(searchValue = event.value)
            is MapEvent.OnConfigLoaded -> {
                val maps = event.config.maps
                state.copy(
                    maps = maps,
                    currentMap = maps.firstOrNull { it.id == state.currentMap?.id }
                        ?: maps.firstOrNull { it.id == event.config.defaultMapId },
                )
            }
            is MapEvent.OnMapSelect -> {
                if (event.map.id == state.currentMap?.id) {
                    state
                } else {
                    state.copy(
                        currentMap = event.map,
                        locations = emptyList(),
                        clusters = emptyList(),
                        markerGroups = emptyList(),
                        viewport = null,
                        camera = null,
                        showAllDungeons = false,
                    ).withoutSelection()
                }
            }
            is MapEvent.OnCameraIdle -> state.copy(viewport = event.viewport, camera = event.camera)
            is MapEvent.OnLocationsLoaded -> state.copy(locations = event.locations, clusters = event.clusters)
            is MapEvent.OnMarkerGroupsLoaded -> state.copy(markerGroups = event.groups)
            is MapEvent.OnLocationClick -> state.withSelection(event.location)
            is MapEvent.OnLocationLinkLoaded -> state.withSelection(event.location)
                .copy(focusedLocation = event.location)
            is MapEvent.OnFoundLocationsLoaded -> state.copy(foundLocationIds = event.locationIds)
            // The view model handles these, the state changes when the data comes back
            is MapEvent.OnFoundClick,
            is MapEvent.OnLocationLinkClick,
            is MapEvent.OnSettingsClick -> state
            // The names of the selected location are in the previous language
            is MapEvent.OnLanguageChange -> state.copy(language = event.language).withoutSelection()
            is MapEvent.OnSettingsChange -> state.copy(
                hideMarkersWhileMoving = event.hideMarkersWhileMoving,
                clusterMarkers = event.clusterMarkers,
            )
            is MapEvent.OnEntrancesLoaded -> state.copy(entrances = event.entrances)
            is MapEvent.OnFloorClick -> state.copy(selectedFloorId = event.floorId)
            is MapEvent.OnLocationDismiss -> state.withoutSelection()
            is MapEvent.OnDungeonsClick -> state.copy(
                showAllDungeons = !state.isDungeonsActive,
                selectedFloorId = null,
            )
            is MapEvent.OnMarkerListClick -> state.copy(isMarkerListVisible = true)
            is MapEvent.OnMarkerListDismiss -> state.copy(isMarkerListVisible = false, markerListQuery = "")
            is MapEvent.OnMarkerListQueryChange -> state.copy(markerListQuery = event.value)
            is MapEvent.OnMarkerListSettingsLoaded -> state.copy(
                collapsedGroupIds = event.settings.collapsedGroupIds,
                categoryVisibility = event.settings.categoryVisibility,
                cycles = event.settings.cycles,
            )
            is MapEvent.OnGroupClick -> state.copy(
                collapsedGroupIds = if (state.isGroupCollapsed(event.groupId)) {
                    state.collapsedGroupIds - event.groupId
                } else {
                    state.collapsedGroupIds + event.groupId
                },
            )
            is MapEvent.OnCategoryClick -> {
                val category = state.markerGroups.asSequence()
                    .flatMap { it.categories }
                    .firstOrNull { it.category.id == event.categoryId }
                    ?: return state
                state.copy(
                    categoryVisibility = state.categoryVisibility +
                        (event.categoryId to !state.isCategoryVisible(category.category)),
                )
            }
            is MapEvent.OnCategoriesVisibilityChange -> state.copy(
                categoryVisibility = state.categoryVisibility + state.markerGroups
                    .flatMap { it.categories }
                    .associate { it.category.id to event.visible },
            )
            is MapEvent.OnCycleClick -> state.copy(cycles = state.cycles + (event.categoryId to event.cycle))
        }
    }

    private fun MapScreenState.withSelection(location: MapLocation): MapScreenState {
        return copy(
            selectedLocation = location,
            selectedFloorId = location.dungeon?.floorId,
            entrances = emptyList(),
            focusedLocation = null,
        )
    }

    private fun MapScreenState.withoutSelection(): MapScreenState {
        return copy(selectedLocation = null, selectedFloorId = null, entrances = emptyList(), focusedLocation = null)
    }
}
