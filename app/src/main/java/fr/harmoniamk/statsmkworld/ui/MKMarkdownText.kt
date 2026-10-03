package fr.harmoniamk.statsmkworld.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Rendu Markdown minimal (#152) des textes officiels MKCentral, sans dépendance : titres `#`,
 * séparateurs `***`/`---`, listes `-`/`*`/`1.`, images seules sur leur ligne, gras `**`, italique
 * `*`, liens `[texte](url)` et URL nues cliquables. Un saut de ligne simple est conservé (les
 * textes MKCentral s'en servent comme retour à la ligne) ; `lines()` absorbe les `\r\n`. Le reste
 * est affiché tel quel.
 */
@Composable
fun MKMarkdownText(markdown: String, modifier: Modifier = Modifier, textColor: Color = Colors.white) {
    val blocks = remember(markdown) { MarkdownBlockParser().parse(markdown) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownLine(
                    text = block.text,
                    font = Fonts.NunitoBD,
                    fontSize = if (block.level <= 2) 16 else 14,
                    textColor = textColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
                is MarkdownBlock.Paragraph -> MarkdownLine(text = block.text, textColor = textColor)
                is MarkdownBlock.ListItem -> Row {
                    MarkdownLine(text = AnnotatedString(block.marker), textColor = textColor, modifier = Modifier.width(20.dp))
                    MarkdownLine(text = block.text, textColor = textColor, modifier = Modifier.weight(1f))
                }
                MarkdownBlock.Rule -> Box(Modifier.fillMaxWidth().height(1.dp).background(Colors.white30))
                is MarkdownBlock.Image -> AsyncImage(
                    model = block.url,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    text: AnnotatedString,
    textColor: Color,
    modifier: Modifier = Modifier,
    font: Font = Fonts.NunitoRG,
    fontSize: Int = 13
) {
    Text(
        text = text,
        color = textColor,
        fontFamily = FontFamily(font),
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 5).sp,
        modifier = modifier
    )
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : MarkdownBlock
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock
    data class ListItem(val marker: String, val text: AnnotatedString) : MarkdownBlock
    data class Image(val url: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
private val ruleRegex = Regex("""^(\*{3,}|-{3,}|_{3,})$""")
private val bulletRegex = Regex("""^[-*+]\s+(.*)$""")
private val orderedRegex = Regex("""^(\d+)\.\s+(.*)$""")
private val imageRegex = Regex("""^!\[[^\]]*]\((\S+)\)$""")

// Ordre des alternatives = priorité : lien, gras, italique, URL nue.
private val inlineRegex = Regex("""\[([^\]]+)]\((\S+?)\)|\*\*(.+?)\*\*|\*(.+?)\*|(https?://[^\s)]+)""")

private val linkStyles = TextLinkStyles(SpanStyle(color = Colors.blue, textDecoration = TextDecoration.Underline))

/** Découpe en blocs ; une instance par texte (état de paragraphe en cours). */
private class MarkdownBlockParser {
    private val blocks = mutableListOf<MarkdownBlock>()
    private val paragraph = mutableListOf<String>()

    fun parse(markdown: String): List<MarkdownBlock> {
        markdown.lines().map { it.trim() }.forEach { line ->
            val heading = headingRegex.find(line)
            val bullet = bulletRegex.find(line)
            val ordered = orderedRegex.find(line)
            val image = imageRegex.find(line)
            when {
                line.isEmpty() -> flushParagraph()
                heading != null -> addBlock(MarkdownBlock.Heading(heading.groupValues[1].length, parseInline(heading.groupValues[2])))
                // Avant les puces : `***` serait sinon lu comme une puce `*`.
                ruleRegex.matches(line) -> addBlock(MarkdownBlock.Rule)
                bullet != null -> addBlock(MarkdownBlock.ListItem("•", parseInline(bullet.groupValues[1])))
                ordered != null -> addBlock(MarkdownBlock.ListItem("${ordered.groupValues[1]}.", parseInline(ordered.groupValues[2])))
                image != null -> addBlock(MarkdownBlock.Image(image.groupValues[1]))
                else -> paragraph += line
            }
        }
        flushParagraph()
        return blocks
    }

    private fun addBlock(block: MarkdownBlock) {
        flushParagraph()
        blocks += block
    }

    private fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(parseInline(paragraph.joinToString("\n")))
            paragraph.clear()
        }
    }
}

private fun parseInline(text: String): AnnotatedString = buildAnnotatedString { appendInline(text) }

private fun AnnotatedString.Builder.appendInline(text: String) {
    var cursor = 0
    inlineRegex.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val (label, url, bold, italic, bareUrl) = match.destructured
        when {
            label.isNotEmpty() -> withLink(LinkAnnotation.Url(url, linkStyles)) { append(label) }
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(bold) }
            italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(italic) }
            else -> {
                // Ponctuation finale exclue de l'URL nue (« …?usp=sharing. »).
                val trimmedUrl = bareUrl.trimEnd('.', ',', ';', ':', '!', '?')
                withLink(LinkAnnotation.Url(trimmedUrl, linkStyles)) { append(trimmedUrl) }
                append(bareUrl.removePrefix(trimmedUrl))
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
