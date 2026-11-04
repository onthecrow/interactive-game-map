package com.onthecrow.interactivegamemap.map.view

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkerDescriptionTest {
    private class Marker(val id: Int, val description: String)

    private val markers = Json.parseToJsonElement(
        File("../logic-rdr2-impl/src/commonMain/composeResources/files/markers.json").readText()
    ).jsonArray.map {
        val marker = it.jsonObject
        Marker(
            id = marker.getValue("id").jsonPrimitive.int,
            description = marker.getValue("description").jsonPrimitive.contentOrNull.orEmpty(),
        )
    }

    private fun AnnotatedString.styled(text: String) = spanStyles.single { subSequence(it.start, it.end).text == text }

    private fun AnnotatedString.links() = getLinkAnnotations(0, length).map {
        subSequence(it.start, it.end).text to (it.item as LinkAnnotation.Clickable).tag
    }

    @Test
    fun `styles are applied and markers are removed`() {
        val text = parseDescription("**Bold** and *star* and _underscore_ and ***both***").single()

        assertEquals("Bold and star and underscore and both", text.text)
        assertEquals(FontWeight.Bold, text.styled("Bold").item.fontWeight)
        assertEquals(FontStyle.Italic, text.styled("star").item.fontStyle)
        assertEquals(FontStyle.Italic, text.styled("underscore").item.fontStyle)
        assertEquals(FontWeight.Bold, text.styled("both").item.fontWeight)
        assertEquals(FontStyle.Italic, text.styled("both").item.fontStyle)
    }

    @Test
    fun `links work inside of other styles`() {
        val clicks = mutableListOf<String>()
        val text = parseDescription("**See [the cabin](https://a.b?c_d=1_2).**", onLinkClick = { clicks += it }).single()

        assertEquals("See the cabin.", text.text)
        assertEquals(listOf("the cabin" to "https://a.b?c_d=1_2"), text.links())
        (text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Clickable).linkInteractionListener
            ?.onClick(LinkAnnotation.Clickable("", null, null))
        assertEquals(listOf("https://a.b?c_d=1_2"), clicks)
    }

    @Test
    fun `underscores and asterisks inside of words stay`() {
        assertEquals("snake_case_name 5*3*2", parseDescription("snake_case_name 5*3*2").single().text)
    }

    @Test
    fun `blank lines split paragraphs and lines are kept`() {
        val paragraphs = parseDescription("**A:**  \r\nfirst\nsecond\r\n\r\n- one\n- two\n  \n\nlast\n  ")

        assertEquals(3, paragraphs.size)
        assertEquals(3, paragraphs[0].paragraphStyles.size)
        assertEquals("A:firstsecond", paragraphs[0].text)
        assertEquals("• one• two", paragraphs[1].text)
        assertTrue(paragraphs[1].paragraphStyles.all { it.item.textIndent != null })
        assertEquals("last", paragraphs[2].text)
    }

    @Test
    fun `spoilers are displayed as text`() {
        assertEquals("Bear inside.", parseDescription("||_Bear inside._||").single().text)
        // A real description ends this way
        assertEquals("Bear inside.", parseDescription("||_Bear inside._|").single().text)
    }

    @Test
    fun `links are told apart`() {
        assertEquals(DescriptionLink.Location(880), parseDescriptionLink("https://rdr2map.com?locationIds=880"))
        assertEquals(DescriptionLink.Search("dreamcatcher"), parseDescriptionLink("https://rdr2map.com?search=dreamcatcher"))
        assertEquals(DescriptionLink.External("https://youtu.be/x"), parseDescriptionLink("https://youtu.be/x"))
        // The map of another game on the same network of sites
        val otherGame = "https://mapgenie.io/red-dead-redemption/maps/world?locationIds=1"
        assertEquals(DescriptionLink.External(otherGame), parseDescriptionLink(otherGame))
    }

    @Test
    fun `no markup is left in real descriptions`() {
        val text = markers.flatMap { parseDescription(it.description) }.joinToString("\n") { it.text }

        listOf("*", "_", "[", "]", "|", "`").forEach { marker ->
            assertFalse(marker in text, "\"$marker\" is left at: ${text.substringAfter(marker).take(60)}")
        }
    }

    @Test
    fun `real links lead to markers of the app`() {
        val ids = markers.map { it.id }.toSet()
        val links = markers.flatMap { parseDescription(it.description) }.flatMap { it.links() }.map { (_, url) -> url }
        val parsed = links.map(::parseDescriptionLink)

        assertEquals(280, links.size)
        assertEquals(277, parsed.count { it is DescriptionLink.Location })
        assertEquals(1, parsed.count { it is DescriptionLink.Search })
        assertEquals(2, parsed.count { it is DescriptionLink.External })
        assertEquals(emptyList(), parsed.filterIsInstance<DescriptionLink.Location>().filter { it.id !in ids })
    }
}
