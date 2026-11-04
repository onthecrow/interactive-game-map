package com.onthecrow.interactivegamemap.map

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Where the camera looks, it is put back when the map view is created again. */
data class CameraPosition(
    val lat: Double,
    val lng: Double,
    val zoom: Float,
)

internal enum class CameraCommand { ZOOM_IN, ZOOM_OUT }

/** Moves the camera of the map view at the requests of the controls over the map. */
internal class MapCameraController {
    private val _commands = MutableSharedFlow<CameraCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<CameraCommand> = _commands.asSharedFlow()

    fun zoomIn() {
        _commands.tryEmit(CameraCommand.ZOOM_IN)
    }

    fun zoomOut() {
        _commands.tryEmit(CameraCommand.ZOOM_OUT)
    }
}
