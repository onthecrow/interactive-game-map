package com.onthecrow.nomadrates.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ScreenResultDispatcherImpl : ScreenResultDispatcher {

    private val _resultFlow: MutableSharedFlow<ScreenResult> = MutableSharedFlow(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val resultFlow: SharedFlow<ScreenResult> get() = _resultFlow.asSharedFlow()

    override fun dispatch(screenResult: ScreenResult) {
        _resultFlow.tryEmit(screenResult)
    }
}
