package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.sync.Synchronizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/** Every update of the game's data applied from now on: the markers and their groups are to be read again. */
interface ObserveContentUpdatesUseCase {
    operator fun invoke(): Flow<Unit>
}

class ObserveContentUpdatesUseCaseImpl(private val synchronizer: Synchronizer) : ObserveContentUpdatesUseCase {
    override fun invoke(): Flow<Unit> {
        return synchronizer.state
            .map { it.updates }
            .distinctUntilChanged()
            // The data read so far already has the updates before
            .drop(1)
            .map { }
    }
}
