package com.onthecrow.interactivegamemap.map.common

import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentFileStoreTest {
    private val directory = Files.createTempDirectory("content").resolve("nested")
    private val store = ContentFileStore(Path(directory.toString()))

    @Test
    fun `writes replace and reads give the text back`() = runTest {
        assertNull(store.read("data.json"))
        assertFalse(store.exists("data.json"))

        store.write("data.json", "first")
        store.write("data.json", "second, ünïcödé")
        assertEquals("second, ünïcödé", store.read("data.json"))
        assertTrue(store.exists("data.json"))
        assertFalse(Files.exists(directory.resolve("data.json.tmp")))

        store.delete("data.json")
        assertNull(store.read("data.json"))
    }
}
