package com.onthecrow.interactivegamemap.map

import kotlinx.coroutines.flow.Flow

interface GetFoundLocationIdsUseCase {
    operator fun invoke(): Flow<Set<Int>>
}

class GetFoundLocationIdsUseCaseImpl(private val repository: FoundLocationsRepository) : GetFoundLocationIdsUseCase {
    override operator fun invoke(): Flow<Set<Int>> {
        return repository.getFoundLocationIds()
    }
}
