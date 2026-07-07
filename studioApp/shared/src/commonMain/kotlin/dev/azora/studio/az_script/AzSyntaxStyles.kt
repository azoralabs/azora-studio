package dev.azora.studio.az_script

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Colors azora source text using [AzHighlightSpan]s from the language server
 * and tints lines that carry diagnostics. Pure styling — the text and offsets
 * are unchanged ([OffsetMapping.Identity]).
 *
 * Spans may be one keystroke stale (they are computed asynchronously), so all
 * ranges are clamped to the current text length.
 */
class AzSyntaxTransformation(
    private val spans: List<AzHighlightSpan>,
    private val errorLines: Set<Int>,
    private val warningLines: Set<Int>,
    /** Line the debugger is paused on — drawn over error/warning tints. */
    private val debugLine: Int? = null,
    /** Caret line — faint background so the eye finds the cursor. */
    private val cursorLine: Int? = null,
    /** Offsets of a matched bracket pair to emphasize. */
    private val bracketPair: Pair<Int, Int>? = null,
    /** Find-bar matches as (start, end) offsets; [activeMatch] indexes into it. */
    private val searchMatches: List<Pair<Int, Int>> = emptyList(),
    private val activeMatch: Int = -1,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val styled = AnnotatedString.Builder(text.text).apply {
            // Current line first, so diagnostic/debug tints paint over it.
            if (cursorLine != null && cursorLine !in errorLines && cursorLine != debugLine) {
                lineRange(text.text, cursorLine)?.let { (start, end) ->
                    addStyle(SpanStyle(background = CURSOR_LINE_BG), start, end)
                }
            }
            for (span in spans) {
                val start = span.start.coerceIn(0, text.length)
                val end = span.end.coerceIn(start, text.length)
                if (start == end) continue
                colorOf(span.type)?.let { addStyle(SpanStyle(color = it), start, end) }
            }
            if (errorLines.isNotEmpty() || warningLines.isNotEmpty() || debugLine != null) {
                applyLineBackgrounds(text.text)
            }
            for ((index, match) in searchMatches.withIndex()) {
                val start = match.first.coerceIn(0, text.length)
                val end = match.second.coerceIn(start, text.length)
                if (start == end) continue
                addStyle(SpanStyle(background = if (index == activeMatch) SEARCH_ACTIVE_BG else SEARCH_BG), start, end)
            }
            bracketPair?.let { (a, b) ->
                for (offset in listOf(a, b)) {
                    if (offset in 0 until text.length) {
                        addStyle(SpanStyle(background = BRACKET_BG), offset, offset + 1)
                    }
                }
            }
        }.toAnnotatedString()
        return TransformedText(styled, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.applyLineBackgrounds(text: String) {
        var line = 1
        var lineStart = 0
        var i = 0
        while (i <= text.length) {
            val atEnd = i == text.length
            if (atEnd || text[i] == '\n') {
                when {
                    line == debugLine -> addStyle(SpanStyle(background = DEBUG_BG), lineStart, i)
                    line in errorLines -> addStyle(SpanStyle(background = ERROR_BG), lineStart, i)
                    line in warningLines -> addStyle(SpanStyle(background = WARNING_BG), lineStart, i)
                }
                line++
                lineStart = i + 1
            }
            i++
        }
    }

    private fun colorOf(type: String): Color? = when (type) {
        "keyword" -> AzoraPalette.AccentPurple
        "string" -> AzoraPalette.AccentGreen
        "char" -> AzoraPalette.AccentGreen
        "interpolation" -> AzoraPalette.AccentTeal
        "number" -> AzoraPalette.AccentCyan
        "comment" -> AzoraPalette.Neutral50
        "function" -> AzoraPalette.AccentBlue
        "type" -> AzoraPalette.AccentYellow
        "annotation" -> AzoraPalette.AccentOrange
        else -> null
    }

    /** Start/end offsets of a 1-based [line] in [text], or null when out of range. */
    private fun lineRange(text: String, line: Int): Pair<Int, Int>? {
        var current = 1
        var start = 0
        var i = 0
        while (i <= text.length) {
            if (i == text.length || text[i] == '\n') {
                if (current == line) return start to i
                current++
                start = i + 1
            }
            i++
        }
        return null
    }

    private companion object {
        val ERROR_BG = Color(0x33FF5555)
        val WARNING_BG = Color(0x26FFD866)
        val DEBUG_BG = Color(0x3346A6FF)
        val CURSOR_LINE_BG = Color(0x14FFFFFF)
        val BRACKET_BG = Color(0x5546A6FF)
        val SEARCH_BG = Color(0x40C8A038)
        val SEARCH_ACTIVE_BG = Color(0x80E0A030)
    }
}
