package com.onthecrow.interactivegamemap.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun `picked language wins over the system`() {
        assertEquals("de", resolveLanguage("de", listOf("ru-RU")))
    }

    @Test
    fun `first supported system language is used`() {
        assertEquals("ru", resolveLanguage(null, listOf("it-IT", "ru-RU", "en-US")))
    }

    @Test
    fun `script and region are ignored`() {
        assertEquals("zh", resolveLanguage(null, listOf("zh-Hans-CN")))
    }

    @Test
    fun `unsupported languages give the default one`() {
        assertEquals(DEFAULT_LANGUAGE, resolveLanguage("id", listOf("it-IT", "uk-UA")))
    }
}
