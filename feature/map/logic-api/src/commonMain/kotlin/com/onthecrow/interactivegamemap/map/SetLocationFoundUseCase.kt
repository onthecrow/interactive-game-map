package com.onthecrow.interactivegamemap.map

interface SetLocationFoundUseCase {
    suspend operator fun invoke(locationId: Int, isFound: Boolean)
}

class SetLocationFoundUseCaseImpl(private val repository: FoundLocationsRepository) : SetLocationFoundUseCase {
    override suspend operator fun invoke(locationId: Int, isFound: Boolean) {
        repository.setLocationFound(locationId, isFound)
    }
}
