package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class FormatAction {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    H1,
    H2,
    H3,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKBOX,
    QUOTE,
    DIVIDER,
    CLEAR
}

data class FormattedBlock(
    val id: Int,
    val rawText: String,
    val annotated: AnnotatedString,
    val isCheckbox: Boolean = false,
    val isChecked: Boolean = false,
    val isDivider: Boolean = false,
    val isQuote: Boolean = false,
    val isHeading1: Boolean = false,
    val isHeading2: Boolean = false,
    val isHeading3: Boolean = false
)

object RichTextParser {

    /**
     * Applies markdown formatting to the current selection of a TextFieldValue.
     */
    fun applyFormat(current: TextFieldValue, action: FormatAction): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        val selectedText = if (start != end) text.substring(start, end) else ""

        return when (action) {
            FormatAction.BOLD -> wrapSelection(text, start, end, "**", "**", "bold")
            FormatAction.ITALIC -> wrapSelection(text, start, end, "*", "*", "italic")
            FormatAction.UNDERLINE -> wrapSelection(text, start, end, "<u>", "</u>", "underline")
            FormatAction.STRIKETHROUGH -> wrapSelection(text, start, end, "~~", "~~", "strikethrough")
            FormatAction.H1 -> prefixLine(text, start, end, "# ")
            FormatAction.H2 -> prefixLine(text, start, end, "## ")
            FormatAction.H3 -> prefixLine(text, start, end, "### ")
            FormatAction.BULLET_LIST -> prefixLine(text, start, end, "- ")
            FormatAction.NUMBERED_LIST -> prefixLine(text, start, end, "1. ")
            FormatAction.CHECKBOX -> prefixLine(text, start, end, "[ ] ")
            FormatAction.QUOTE -> prefixLine(text, start, end, "> ")
            FormatAction.DIVIDER -> {
                val insert = "\n---\n"
                val newText = text.substring(0, start) + insert + text.substring(end)
                val newPos = start + insert.length
                TextFieldValue(newText, androidx.compose.ui.text.TextRange(newPos))
            }
            FormatAction.CLEAR -> {
                // Strip markdown tokens from selected text
                val cleaned = selectedText
                    .replace(Regex("""[*_~#>`]"""), "")
                    .replace("<u>", "").replace("</u>", "")
                    .replace("[ ] ", "").replace("[x] ", "")
                val newText = text.substring(0, start) + cleaned + text.substring(end)
                TextFieldValue(newText, androidx.compose.ui.text.TextRange(start, start + cleaned.length))
            }
        }
    }

    private fun wrapSelection(
        text: String,
        start: Int,
        end: Int,
        prefix: String,
        suffix: String,
        defaultWord: String
    ): TextFieldValue {
        val content = if (start == end) defaultWord else text.substring(start, end)
        val replacement = "$prefix$content$suffix"
        val newText = text.substring(0, start) + replacement + text.substring(end)
        val newSelectionStart = start + prefix.length
        val newSelectionEnd = newSelectionStart + content.length
        return TextFieldValue(newText, androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd))
    }

    private fun prefixLine(
        text: String,
        start: Int,
        end: Int,
        prefix: String
    ): TextFieldValue {
        // Find line start
        val lineStart = text.lastIndexOf('\n', start - 1).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val shift = prefix.length
        return TextFieldValue(newText, androidx.compose.ui.text.TextRange(start + shift, end + shift))
    }

    /**
     * Toggles a checkbox in the raw markdown text given its line index.
     */
    fun toggleCheckboxAtLine(fullText: String, lineIndex: Int): String {
        val lines = fullText.split("\n").toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            lines[lineIndex] = when {
                line.trimStart().startsWith("[ ] ") -> line.replaceFirst("[ ] ", "[x] ")
                line.trimStart().startsWith("[x] ") -> line.replaceFirst("[x] ", "[ ] ")
                line.trimStart().startsWith("[X] ") -> line.replaceFirst("[X] ", "[ ] ")
                else -> line
            }
        }
        return lines.joinToString("\n")
    }

    /**
     * Parses the full text into segmented blocks for smooth lazy scrolling of long documents.
     */
    fun parseToBlocks(fullText: String, textColor: Color = TextPrimary): List<FormattedBlock> {
        val lines = fullText.split("\n")
        val blocks = ArrayList<FormattedBlock>(lines.size)

        for ((index, rawLine) in lines.withIndex()) {
            val trimmed = rawLine.trim()
            val isDivider = trimmed == "---" || trimmed == "***" || trimmed == "___"
            val isHeading1 = trimmed.startsWith("# ")
            val isHeading2 = trimmed.startsWith("## ")
            val isHeading3 = trimmed.startsWith("### ")
            val isQuote = trimmed.startsWith("> ")
            val isCheckbox = trimmed.startsWith("[ ] ") || trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")
            val isChecked = trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")

            val annotated = buildAnnotatedStringFromLine(
                rawLine = rawLine,
                textColor = textColor,
                isH1 = isHeading1,
                isH2 = isHeading2,
                isH3 = isHeading3,
                isQuote = isQuote
            )

            blocks.add(
                FormattedBlock(
                    id = index,
                    rawText = rawLine,
                    annotated = annotated,
                    isCheckbox = isCheckbox,
                    isChecked = isChecked,
                    isDivider = isDivider,
                    isQuote = isQuote,
                    isHeading1 = isHeading1,
                    isHeading2 = isHeading2,
                    isHeading3 = isHeading3
                )
            )
        }
        return blocks
    }

    /**
     * Builds an AnnotatedString from a single line of markdown text.
     */
    fun buildAnnotatedStringFromLine(
        rawLine: String,
        textColor: Color = TextPrimary,
        isH1: Boolean = false,
        isH2: Boolean = false,
        isH3: Boolean = false,
        isQuote: Boolean = false
    ): AnnotatedString {
        var cleanLine = rawLine

        // Strip heading / quote / checkbox prefixes for clean rendering in display mode
        if (isH1) cleanLine = cleanLine.removePrefix("# ").trimStart()
        else if (isH2) cleanLine = cleanLine.removePrefix("## ").trimStart()
        else if (isH3) cleanLine = cleanLine.removePrefix("### ").trimStart()
        else if (isQuote) cleanLine = cleanLine.removePrefix("> ").trimStart()
        else if (cleanLine.trimStart().startsWith("[ ] ")) cleanLine = cleanLine.replaceFirst("[ ] ", "")
        else if (cleanLine.trimStart().startsWith("[x] ") || cleanLine.trimStart().startsWith("[X] ")) {
            cleanLine = cleanLine.replaceFirst(Regex("""\[[xX]]\s*"""), "")
        }

        return buildAnnotatedString {
            append(cleanLine)

            // Base heading styles
            when {
                isH1 -> addStyle(
                    SpanStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    0,
                    length
                )
                isH2 -> addStyle(
                    SpanStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    ),
                    0,
                    length
                )
                isH3 -> addStyle(
                    SpanStyle(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    ),
                    0,
                    length
                )
                isQuote -> addStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary
                    ),
                    0,
                    length
                )
            }

            // Inline formatting: Bold **text**
            val boldRegex = Regex("""\*\*(.*?)\*\*""")
            for (match in boldRegex.findAll(cleanLine)) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }

            // Inline formatting: Italic *text*
            val italicRegex = Regex("""(?<!\*)\*([^*]+)\*(?!\*)""")
            for (match in italicRegex.findAll(cleanLine)) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }

            // Inline formatting: Strikethrough ~~text~~
            val strikeRegex = Regex("""~~(.*?)~~""")
            for (match in strikeRegex.findAll(cleanLine)) {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), match.range.first, match.range.last + 1)
            }

            // Inline formatting: Underline <u>text</u>
            val underlineRegex = Regex("""<u>(.*?)</u>""")
            for (match in underlineRegex.findAll(cleanLine)) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
            }
        }
    }
}
