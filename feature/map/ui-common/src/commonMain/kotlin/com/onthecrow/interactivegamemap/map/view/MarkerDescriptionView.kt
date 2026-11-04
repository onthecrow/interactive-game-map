package com.onthecrow.interactivegamemap.map.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.onthecrow.interactivegamemap.ui.theme.GameMapTheme
import androidx.compose.ui.unit.sp

// ***bold italic***, **bold**, *italic*, _italic_, [text](url)
private val InlineMarkup = Regex(
    """\*\*\*(.+?)\*\*\*""" +
        """|\*\*(.+?)\*\*""" +
        """|(?<![*\w])\*([^*\s][^*]*?)\*(?![*\w])""" +
        """|(?<![\w_])_([^_\s][^_]*?)_(?![\w_])""" +
        """|\[([^\]]*)\]\(([^)]*)\)"""
)
private val BlankLine = Regex("""\n\s*\n""")

// Links of the source site which open something on the map instead of a browser
private val MapLink = Regex("""^https?://(?:www\.)?rdr2map\.com/?\?(?:locationIds=(\d+)|search=([^&]+))""")

private const val BULLET_MARKER = "- "
private const val BULLET = "• "
private const val SPOILER_MARKER = "||"

private val BoldStyle = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicStyle = SpanStyle(fontStyle = FontStyle.Italic)
private val LineStyle = ParagraphStyle()
private val BulletStyle = ParagraphStyle(textIndent = TextIndent(restLine = 10.sp))

internal sealed interface DescriptionLink {
    data class Location(val id: Int) : DescriptionLink
    data class Search(val query: String) : DescriptionLink
    data class External(val url: String) : DescriptionLink
}

internal fun parseDescriptionLink(url: String): DescriptionLink {
    val (locationId, query) = MapLink.find(url)?.destructured ?: return DescriptionLink.External(url)
    return locationId.toIntOrNull()?.let { DescriptionLink.Location(it) } ?: DescriptionLink.Search(query)
}

/** @return the paragraphs of a description, every line of a paragraph is displayed on its own line */
internal fun parseDescription(
    description: String,
    linkStyles: TextLinkStyles? = null,
    emphasis: SpanStyle = BoldStyle,
    onLinkClick: (String) -> Unit = {},
): List<AnnotatedString> {
    val markup = Markup(linkStyles, emphasis, onLinkClick)
    return description.split(BlankLine)
        .map { paragraph -> paragraph.lines().map { it.trim() }.filter { it.isNotEmpty() } }
        .filter { it.isNotEmpty() }
        .map { lines ->
            buildAnnotatedString {
                lines.forEach { line ->
                    val isBullet = line.startsWith(BULLET_MARKER)
                    withStyle(if (isBullet) BulletStyle else LineStyle) {
                        if (isBullet) append(BULLET)
                        // A spoiler is displayed as a regular text, one of them has a broken end: "|"
                        val text = line.removePrefix(BULLET_MARKER).replace(SPOILER_MARKER, "").removeSuffix("|")
                        appendInline(text, markup)
                    }
                }
            }
        }
}

/** How the inline markup of a description looks and what its links do. */
private class Markup(
    val linkStyles: TextLinkStyles?,
    val emphasis: SpanStyle,
    val onLinkClick: (String) -> Unit,
)

private fun AnnotatedString.Builder.appendInline(text: String, markup: Markup) {
    var position = 0
    InlineMarkup.findAll(text).forEach { match ->
        append(text.substring(position, match.range.first))
        val (boldItalic, bold, italic, underscoreItalic, linkText, linkUrl) = match.destructured
        when {
            boldItalic.isNotEmpty() -> withStyle(markup.emphasis.merge(ItalicStyle)) { appendInline(boldItalic, markup) }
            bold.isNotEmpty() -> withStyle(markup.emphasis) { appendInline(bold, markup) }
            italic.isNotEmpty() -> withStyle(ItalicStyle) { appendInline(italic, markup) }
            underscoreItalic.isNotEmpty() -> withStyle(ItalicStyle) { appendInline(underscoreItalic, markup) }
            else -> withLink(LinkAnnotation.Clickable(linkUrl, markup.linkStyles) { markup.onLinkClick(linkUrl) }) {
                appendInline(linkText, markup)
            }
        }
        position = match.range.last + 1
    }
    append(text.substring(position))
}

@Composable
internal fun MarkerDescriptionView(
    description: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {},
) {
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val linkColor = GameMapTheme.colors.link
    val emphasis = GameMapTheme.typography.emphasis
    val paragraphs = remember(description, linkColor, emphasis) {
        parseDescription(
            description = description,
            linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
            emphasis = emphasis,
            onLinkClick = { currentOnLinkClick(it) },
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        paragraphs.forEach { paragraph ->
            Text(
                text = paragraph,
                style = GameMapTheme.typography.body,
                color = GameMapTheme.colors.content,
            )
        }
    }
}
