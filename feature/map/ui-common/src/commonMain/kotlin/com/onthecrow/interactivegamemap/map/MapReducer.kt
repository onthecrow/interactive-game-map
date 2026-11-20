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
            is MapEvent.OnSearchLinkClick -> state.copy(searchValue = event.query).withoutSelection()
            is MapEvent.OnConfigLoaded -> {
                val maps = event.config.maps
                val saved = event.savedMapView.takeIf { state.currentMap == null }
                val currentMap = maps.firstOrNull { it.id == state.currentMap?.id }
                    ?: maps.firstOrNull { it.id == saved?.mapId }
                    ?: maps.firstOrNull { it.id == event.config.defaultMapId }
                state.copy(
                    maps = maps,
                    currentMap = currentMap,
                    camera = if (saved != null && currentMap?.id == saved.mapId) saved.camera else state.camera,
                )
            }
            is MapEvent.OnMapSelect -> {
                if (event.map.id == state.currentMap?.id) {
                    state
                } else {
                    // A route is of its map
                    state.withoutRoute().copy(
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
            is MapEvent.OnFoundLocationsLoaded -> state.copy(foundLocationIds = event.locationIds)
            // Found on the page of the route goes on to the next one, the view model marks the location
            is MapEvent.OnFoundClick -> {
                val isCurrent = state.isRoutePagerVisible && state.selectedLocation?.id == event.locationId
                if (isCurrent && event.locationId !in state.foundLocationIds) state.atRoutePage(state.routePage + 1) else state
            }
            // The view model handles these, the state changes when the data comes back
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
                isMarkerListSettingsLoaded = true,
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
            is MapEvent.OnFitBounds -> state.copy(cameraFit = CameraFit(event.bounds, (state.cameraFit?.id ?: 0) + 1))
            is MapEvent.OnRouteClick -> if (state.route != null) state.copy(isRouteExitConfirmVisible = true) else state
            is MapEvent.OnRouteExitConfirm -> state.withoutRoute().withoutSelection()
            is MapEvent.OnRouteExitDismiss -> state.copy(isRouteExitConfirmVisible = false)
            // With everything found there is nowhere to go, the dialog only closes
            is MapEvent.OnRouteContinue -> state.copy(isRouteExitConfirmVisible = false)
                .atRoutePage(state.routeLocations.indexOfFirst { it.id !in state.foundLocationIds })
            is MapEvent.OnRouteAvailabilityChange -> state.copy(isRouteAvailable = event.isAvailable)
            is MapEvent.OnRouteCandidatesCounted -> state.copy(routeCandidateCount = event.count)
            is MapEvent.OnRouteBuildProgress -> state.copy(routeBuild = event.progress)
            is MapEvent.OnRouteBuildCancel,
            is MapEvent.OnRouteBuildFailed -> state.copy(routeBuild = null)
            is MapEvent.OnRouteLoaded -> {
                if (event.locations.isEmpty()) {
                    state.withoutRoute()
                } else {
                    state.copy(route = event.route, routeLocations = event.locations, routeBuild = null)
                        .atRoutePage(event.page.coerceIn(event.locations.indices))
                }
            }
            is MapEvent.OnRouteLocationsLoaded -> {
                val selected = state.selectedLocation?.let { selected -> event.locations.firstOrNull { it.id == selected.id } }
                state.copy(
                    routeLocations = event.locations,
                    routePage = state.routePage.coerceIn(0, (event.locations.size - 1).coerceAtLeast(0)),
                    selectedLocation = selected ?: state.selectedLocation,
                )
            }
            is MapEvent.OnRoutePageChange -> state.atRoutePage(event.page)
            // The page shown is of a hidden location, the popup goes with the route
            is MapEvent.OnRouteHidden -> (if (state.isRoutePagerVisible) state.withoutSelection() else state).withoutRoute()
            is MapEvent.OnRouteStopClick -> state.copy(selectedRouteStop = event.number)
        }
    }

    // A location of the route opens its page, any other one its own popup; the camera goes to it either way
    private fun MapScreenState.withSelection(location: MapLocation): MapScreenState {
        val page = if (route != null) routeLocations.indexOfFirst { it.id == location.id } else -1
        return copy(
            selectedLocation = location,
            selectedFloorId = location.dungeon?.floorId,
            entrances = emptyList(),
            focusedLocation = location,
            routePage = if (page >= 0) page else routePage,
            selectedRouteStop = null,
        )
    }

    /** The location of [page] of the route selected and the camera taken to it; past the ends nothing changes. */
    private fun MapScreenState.atRoutePage(page: Int): MapScreenState {
        val location = routeLocations.getOrNull(page) ?: return this
        return withSelection(location).copy(routePage = page)
    }

    private fun MapScreenState.withoutRoute(): MapScreenState {
        return copy(
            route = null,
            routeLocations = emptyList(),
            routePage = 0,
            isRouteExitConfirmVisible = false,
            selectedRouteStop = null,
        )
    }

    private fun MapScreenState.withoutSelection(): MapScreenState {
        return copy(
            selectedLocation = null,
            selectedFloorId = null,
            entrances = emptyList(),
            focusedLocation = null,
            selectedRouteStop = null,
        )
    }
}
