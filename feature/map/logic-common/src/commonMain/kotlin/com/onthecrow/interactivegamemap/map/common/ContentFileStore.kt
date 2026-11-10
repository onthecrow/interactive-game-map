package com.onthecrow.interactivegamemap.map.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

/**
 * The files of the data a game downloads, in a directory of the app's own. A write goes to a temporary file
 * first and replaces the old one at once, so a killed app never leaves half a file.
 */
class ContentFileStore(private val directory: Path) {

    /** The text of [name], null when there is no such file. */
    suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val path = Path(directory, name)
        if (!SystemFileSystem.exists(path)) return@withContext null
        SystemFileSystem.source(path).buffered().use { it.readString() }
    }

    suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        SystemFileSystem.createDirectories(directory)
        val path = Path(directory, name)
        val temporary = Path(directory, "$name.tmp")
        SystemFileSystem.sink(temporary).buffered().use { it.writeString(text) }
        SystemFileSystem.atomicMove(temporary, path)
    }

    suspend fun exists(name: String): Boolean = withContext(Dispatchers.IO) {
        SystemFileSystem.exists(Path(directory, name))
    }

    suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        SystemFileSystem.delete(Path(directory, name), mustExist = false)
    }
}
