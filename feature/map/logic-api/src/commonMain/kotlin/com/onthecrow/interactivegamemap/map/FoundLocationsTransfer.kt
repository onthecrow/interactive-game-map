package com.onthecrow.interactivegamemap.map

import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/** The game of the app, like rdr2 or genshin: a file of found markers is of one game, their ids mean nothing in another. */
data class GameId(val value: String)

/**
 * A file of the found markers, the same for every app and platform, so a file of one phone opens on another.
 *
 * Users keep these files to move their progress, so every file ever exported must keep importing: never rename or
 * drop a field or change what a version means; a new format is a new [version] the importer reads beside the older
 * ones. The ids in [found] are the ids of the markers and [game] is FlavorConfig.gameId, both must stay stable.
 * FoundLocationsTransferTest holds files of every version, they are never edited to make it pass.
 *
 * @param format tells this file from any other JSON
 * @param version of the format, a newer app reads every older one
 */
@Serializable
internal class FoundLocationsFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val game: String,
    @SerialName("exported_at")
    val exportedAt: String,
    val found: List<Int>,
) {
    companion object {
        const val FORMAT = "interactive-game-map/found-markers"
        const val VERSION = 1
    }
}

/** The found markers as a file to keep or to move to another device. */
class FoundLocationsExport(
    /** Like rdr2-found-markers-2026-11-20.json */
    val fileName: String,
    val content: String,
    val count: Int,
)

sealed interface FoundLocationsImport {
    /** [added] markers weren't found before, of [total] in the file. */
    data class Done(val added: Int, val total: Int) : FoundLocationsImport

    /** The file isn't a file of found markers, or of a newer version of the format. */
    data object NotAFile : FoundLocationsImport

    /** The file is of [game], not of the game of the app. */
    data class OtherGame(val game: String) : FoundLocationsImport
}

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = true
}

interface ExportFoundLocationsUseCase {
    suspend operator fun invoke(): FoundLocationsExport
}

class ExportFoundLocationsUseCaseImpl(
    private val repository: FoundLocationsRepository,
    private val gameId: GameId,
    private val clock: Clock = Clock.System,
) : ExportFoundLocationsUseCase {
    override suspend operator fun invoke(): FoundLocationsExport {
        val found = repository.getFoundLocationIds().first().sorted()
        val now = clock.now().toString()
        val file = FoundLocationsFile(game = gameId.value, exportedAt = now, found = found)
        return FoundLocationsExport(
            fileName = "${gameId.value}-found-markers-${now.take(10)}.json",
            content = json.encodeToString(file),
            count = found.size,
        )
    }
}

/** Adds the found markers of a file to the ones there are; none of them is unmarked. */
interface ImportFoundLocationsUseCase {
    suspend operator fun invoke(content: String): FoundLocationsImport
}

class ImportFoundLocationsUseCaseImpl(
    private val repository: FoundLocationsRepository,
    private val gameId: GameId,
) : ImportFoundLocationsUseCase {
    override suspend operator fun invoke(content: String): FoundLocationsImport {
        val file = try {
            json.decodeFromString<FoundLocationsFile>(content)
        } catch (_: SerializationException) {
            return FoundLocationsImport.NotAFile
        } catch (_: IllegalArgumentException) {
            return FoundLocationsImport.NotAFile
        }
        if (file.format != FoundLocationsFile.FORMAT || file.version > FoundLocationsFile.VERSION) {
            return FoundLocationsImport.NotAFile
        }
        if (file.game != gameId.value) return FoundLocationsImport.OtherGame(file.game)
        val ids = file.found.toSet()
        return FoundLocationsImport.Done(added = repository.addFoundLocations(ids), total = ids.size)
    }
}
