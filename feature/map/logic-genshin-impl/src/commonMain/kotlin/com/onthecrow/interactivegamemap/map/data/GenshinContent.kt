package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.common.ContentFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val META_FILE = "genshin_meta.json"
private const val SNAPSHOT_FILE = "genshin_points.json"

private fun labelsFile(language: String) = "genshin_labels_$language.json"

/**
 * The data of the game on the device: the files of the last update, and in memory what has been read of them.
 * [revision] grows with every update saved, even one of the same version, like the weekly full download: the
 * repositories read the data again then.
 */
internal class GenshinContent(private val files: ContentFileStore) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private var meta: SnapshotMeta? = null
    private var metaRead = false
    private var snapshot: SnapshotHolder? = null
    private var labels: SnapshotLabels? = null

    private val mutableRevision = MutableStateFlow(0)
    val revision: StateFlow<Int> = mutableRevision.asStateFlow()

    suspend fun meta(): SnapshotMeta? = mutex.withLock { readMeta() }

    // Main-safe: the points are megabytes of JSON, the maps' flow is collected on the main thread
    suspend fun snapshot(): GenshinSnapshot? = withContext(Dispatchers.Default) { readSnapshot() }

    private suspend fun readSnapshot(): GenshinSnapshot? = mutex.withLock {
        snapshot?.let { return@withLock it.value }
        val read = files.read(SNAPSHOT_FILE)?.let { json.decodeFromString<GenshinSnapshot>(it) }
        snapshot = SnapshotHolder(read)
        read
    }

    /** The names in [language], or in another downloaded language until those come. */
    suspend fun labels(language: String): SnapshotLabels? = withContext(Dispatchers.Default) { readLabels(language) }

    private suspend fun readLabels(language: String): SnapshotLabels? = mutex.withLock {
        labels?.takeIf { it.language == language }?.let { return@withLock it }
        val languages = listOf(language) + readMeta()?.languages.orEmpty()
        val read = languages.firstNotNullOfOrNull { candidate ->
            files.read(labelsFile(candidate))?.let { json.decodeFromString<SnapshotLabels>(it) }
        }
        // A fallback isn't kept: the asked language may come with the next sync
        if (read?.language == language) labels = read
        read
    }

    /** Replaces everything: the points of the update and the names it came with; the other languages go stale. */
    suspend fun save(snapshot: GenshinSnapshot, labels: SnapshotLabels, meta: SnapshotMeta) = mutex.withLock {
        readMeta()?.languages?.filter { it != labels.language }?.forEach { files.delete(labelsFile(it)) }
        files.write(SNAPSHOT_FILE, json.encodeToString(GenshinSnapshot.serializer(), snapshot))
        files.write(labelsFile(labels.language), json.encodeToString(SnapshotLabels.serializer(), labels))
        // The meta last: a launch after a failed write finds the old meta and checks again
        files.write(META_FILE, json.encodeToString(SnapshotMeta.serializer(), meta))
        this.meta = meta
        this.snapshot = SnapshotHolder(snapshot)
        this.labels = labels
        mutableRevision.update { it + 1 }
    }

    /** Adds the names in another language to the points the device has. */
    suspend fun saveLabels(labels: SnapshotLabels, meta: SnapshotMeta) = mutex.withLock {
        files.write(labelsFile(labels.language), json.encodeToString(SnapshotLabels.serializer(), labels))
        files.write(META_FILE, json.encodeToString(SnapshotMeta.serializer(), meta))
        this.meta = meta
        this.labels = labels
        // The names are part of what the screens show
        mutableRevision.update { it + 1 }
    }

    private suspend fun readMeta(): SnapshotMeta? {
        if (!metaRead) {
            meta = files.read(META_FILE)?.let { json.decodeFromString<SnapshotMeta>(it) }
            metaRead = true
        }
        return meta
    }

    private class SnapshotHolder(val value: GenshinSnapshot?)
}
