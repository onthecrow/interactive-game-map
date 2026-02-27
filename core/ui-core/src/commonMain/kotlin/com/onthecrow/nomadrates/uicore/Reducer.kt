package com.onthecrow.nomadrates.uicore

interface Reducer<S: State, E: Event> {
    suspend fun reduce(state: S, event: E): S
}
