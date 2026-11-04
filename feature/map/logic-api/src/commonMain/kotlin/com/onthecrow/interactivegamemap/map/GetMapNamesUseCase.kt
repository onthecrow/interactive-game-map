package com.onthecrow.interactivegamemap.map

/** The names of the maps in a language, by the ids of the maps; the maps of the remote config keep theirs. */
interface GetMapNamesUseCase {
    suspend operator fun invoke(language: String): Map<Int, String>
}

class GetMapNamesUseCaseImpl(private val repository: LocationsRepository) : GetMapNamesUseCase {
    override suspend operator fun invoke(language: String): Map<Int, String> {
        return repository.getMarkerCatalog(language).mapNames
    }
}
