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
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val styled = AnnotatedString.Builder(text.text).apply {
            for (span in spans) {
                val start = span.start.coerceIn(0, text.length)
                val end = span.end.coerceIn(start, text.length)
                if (start == end) continue
                colorOf(span.type)?.let { addStyle(SpanStyle(color = it), start, end) }
            }
            if (errorLines.isNotEmpty() || warningLines.isNotEmpty()) {
                applyLineBackgrounds(text.text)
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
                when (line) {
                    in errorLines -> addStyle(SpanStyle(background = ERROR_BG), lineStart, i)
                    in warningLines -> addStyle(SpanStyle(background = WARNING_BG), lineStart, i)
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

    private companion object {
        val ERROR_BG = Color(0x33FF5555)
        val WARNING_BG = Color(0x26FFD866)
    }
}
