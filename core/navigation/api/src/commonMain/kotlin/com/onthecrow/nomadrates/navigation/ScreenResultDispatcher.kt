package com.onthecrow.nomadrates.navigation

import kotlinx.coroutines.flow.SharedFlow

interface ScreenResultDispatcher {
    val resultFlow: SharedFlow<ScreenResult?>
    fun dispatch(screenResult: ScreenResult)
}
