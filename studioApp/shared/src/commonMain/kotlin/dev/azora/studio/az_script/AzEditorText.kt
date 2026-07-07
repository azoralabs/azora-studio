package dev.azora.studio.az_script

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Pure text-editing operations backing the `.az` code editor — all functions
 * map a [TextFieldValue] to a new one, so every behavior is unit-testable
 * without Compose:
 *
 * - [processEdit] — smart Enter (indent-preserving, `{`-aware, brace-block
 *   expansion), bracket/quote auto-close, type-over of closing chars, and
 *   pair-aware backspace.
 * - [indentSelection] / [outdentSelection] — Tab / Shift+Tab block indenting.
 * - [toggleLineComment] — `//` on/off for the line or selection.
 * - [duplicateLine], [deleteLine], [moveLine] — whole-line operations.
 * - [matchingBracket] — offsets of the bracket pair at the caret.
 */
object AzEditorText {

    const val INDENT = "    "

    private val OPENERS = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"')
    private val CLOSERS = setOf(')', ']', '}')

    // -----------------------------------------------------------------
    // Typing pipeline
    // -----------------------------------------------------------------

    /**
     * Post-processes a proposed edit (old → user-typed new value). Returns the
     * value the editor should actually apply.
     */
    fun processEdit(
        old: TextFieldValue,
        proposed: TextFieldValue,
        indent: String = INDENT,
        autoCloseBrackets: Boolean = true,
        smartIndent: Boolean = true,
    ): TextFieldValue {
        val inserted = singleInsertedChar(old, proposed)
        if (inserted != null) {
            val at = proposed.selection.start - 1
            val typesOver = autoCloseBrackets && (inserted in CLOSERS || inserted == '"') && old.selection.collapsed &&
                old.selection.start < old.text.length && old.text[old.selection.start] == inserted
            when {
                inserted == '\n' && smartIndent -> return smartNewline(old, indent)
                // Type-over: the closing char is already there — just step past it.
                typesOver -> return old.copy(selection = TextRange(old.selection.start + 1))
                autoCloseBrackets && inserted in OPENERS.keys -> return autoClose(old, proposed, inserted, at)
            }
        }
        if (autoCloseBrackets && isSingleBackspace(old, proposed)) {
            val removedAt = proposed.selection.start
            val removed = old.text[removedAt]
            val closing = OPENERS[removed]
            if (closing != null && removedAt < proposed.text.length && proposed.text[removedAt] == closing) {
                // Deleting an opener with its untouched partner right after removes both.
                val text = proposed.text.removeRange(removedAt, removedAt + 1)
                return TextFieldValue(text, TextRange(removedAt))
            }
        }
        return proposed
    }

    private fun singleInsertedChar(old: TextFieldValue, proposed: TextFieldValue): Char? {
        if (!old.selection.collapsed || !proposed.selection.collapsed) return null
        if (proposed.text.length != old.text.length + 1) return null
        val at = proposed.selection.start - 1
        if (at < 0 || at != old.selection.start) return null
        // Everything except the inserted char must be unchanged.
        if (proposed.text.substring(0, at) != old.text.substring(0, at)) return null
        if (proposed.text.substring(at + 1) != old.text.substring(at)) return null
        return proposed.text[at]
    }

    private fun isSingleBackspace(old: TextFieldValue, proposed: TextFieldValue): Boolean {
        if (!old.selection.collapsed || !proposed.selection.collapsed) return false
        if (proposed.text.length != old.text.length - 1) return false
        val at = proposed.selection.start
        return at == old.selection.start - 1 &&
            proposed.text.substring(0, at) == old.text.substring(0, at) &&
            proposed.text.substring(at) == old.text.substring(at + 1)
    }

    private fun smartNewline(old: TextFieldValue, indent: String): TextFieldValue {
        val text = old.text
        val cursor = old.selection.start
        val before = if (old.selection.collapsed) text.substring(0, cursor)
            else text.substring(0, old.selection.min)
        val after = if (old.selection.collapsed) text.substring(cursor)
            else text.substring(old.selection.max)

        val lineStart = before.lastIndexOf('\n') + 1
        val lineIndent = before.substring(lineStart).takeWhile { it == ' ' || it == '\t' }
        val opensBlock = before.trimEnd().endsWith('{')
        val closesNext = after.trimStart(' ', '\t').startsWith('}')

        return when {
            opensBlock && closesNext -> {
                // `{|}` → indented body line with `}` back on its own line.
                val insert = "\n${lineIndent}$indent\n$lineIndent"
                TextFieldValue(before + insert + after.trimStart(' ', '\t'), TextRange(before.length + 1 + lineIndent.length + indent.length))
            }
            opensBlock -> {
                val insert = "\n${lineIndent}$indent"
                TextFieldValue(before + insert + after, TextRange(before.length + insert.length))
            }
            else -> {
                val insert = "\n$lineIndent"
                TextFieldValue(before + insert + after, TextRange(before.length + insert.length))
            }
        }
    }

    private fun autoClose(old: TextFieldValue, proposed: TextFieldValue, opener: Char, at: Int): TextFieldValue {
        val closer = OPENERS.getValue(opener)
        val next = proposed.text.getOrNull(at + 1)
        // Quotes: don't double up when re-typing an existing string boundary.
        if (opener == '"' && next == '"') return proposed
        // Only pair when the next char won't glue onto the closer (end, space, closer…).
        val pairs = next == null || next == ' ' || next == '\n' || next == '\t' || next in CLOSERS || next == ',' || next == ';'
        if (!pairs) return proposed
        val text = proposed.text.substring(0, at + 1) + closer + proposed.text.substring(at + 1)
        return TextFieldValue(text, TextRange(at + 1))
    }

    // -----------------------------------------------------------------
    // Block operations
    // -----------------------------------------------------------------

    /** Tab: indents the selected lines (or inserts one indent at the caret). */
    fun indentSelection(value: TextFieldValue, indent: String = INDENT): TextFieldValue {
        if (value.selection.collapsed) {
            val at = value.selection.start
            val text = value.text.substring(0, at) + indent + value.text.substring(at)
            return TextFieldValue(text, TextRange(at + indent.length))
        }
        return transformLines(value) { line -> indent + line }
    }

    /** Shift+Tab: removes one level of indentation from the selected lines. */
    fun outdentSelection(value: TextFieldValue, indent: String = INDENT): TextFieldValue =
        transformLines(value) { line ->
            when {
                line.startsWith(indent) -> line.removePrefix(indent)
                else -> line.trimStart(' ', '\t').let { trimmed ->
                    if (trimmed.length == line.length) line else trimmed
                }
            }
        }

    /** Cmd+/: comments the selected lines with `// ` (or uncomments them all). */
    fun toggleLineComment(value: TextFieldValue): TextFieldValue {
        val (start, end) = lineSpan(value)
        val block = value.text.substring(start, end)
        val lines = block.split('\n')
        val meaningful = lines.filter { it.isNotBlank() }
        val allCommented = meaningful.isNotEmpty() && meaningful.all { it.trimStart().startsWith("//") }
        val replaced = lines.joinToString("\n") { line ->
            when {
                line.isBlank() -> line
                allCommented -> {
                    val i = line.indexOf("//")
                    line.removeRange(i, minOf(i + if (line.startsWith("// ", i)) 3 else 2, line.length))
                }
                else -> {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    indent + "// " + line.substring(indent.length)
                }
            }
        }
        val text = value.text.substring(0, start) + replaced + value.text.substring(end)
        return TextFieldValue(text, TextRange(start, start + replaced.length))
    }

    /** Cmd+D: duplicates the current line (or the selected lines) below. */
    fun duplicateLine(value: TextFieldValue): TextFieldValue {
        val (start, end) = lineSpan(value)
        val block = value.text.substring(start, end)
        val text = value.text.substring(0, end) + "\n" + block + value.text.substring(end)
        val shift = block.length + 1
        return TextFieldValue(text, TextRange(value.selection.start + shift, value.selection.end + shift))
    }

    /** Cmd+Shift+K: deletes the current line (or the selected lines). */
    fun deleteLine(value: TextFieldValue): TextFieldValue {
        val (start, end) = lineSpan(value)
        val cutEnd = if (end < value.text.length) end + 1 else end
        val cutStart = if (cutEnd == end && start > 0) start - 1 else start
        val text = value.text.removeRange(cutStart, cutEnd)
        return TextFieldValue(text, TextRange(minOf(start, text.length)))
    }

    /** Alt+Up/Down: swaps the selected line block with its neighbour. */
    fun moveLine(value: TextFieldValue, up: Boolean): TextFieldValue {
        val (start, end) = lineSpan(value)
        val text = value.text
        if (up) {
            if (start == 0) return value
            val prevStart = text.lastIndexOf('\n', start - 2) + 1
            val prev = text.substring(prevStart, start - 1)
            val block = text.substring(start, end)
            val rebuilt = text.substring(0, prevStart) + block + "\n" + prev + text.substring(end)
            val shift = -(prev.length + 1)
            return TextFieldValue(rebuilt, TextRange(value.selection.start + shift, value.selection.end + shift))
        } else {
            if (end >= text.length) return value
            val nextEnd = text.indexOf('\n', end + 1).let { if (it == -1) text.length else it }
            val next = text.substring(end + 1, nextEnd)
            val block = text.substring(start, end)
            val rebuilt = text.substring(0, start) + next + "\n" + block + text.substring(nextEnd)
            val shift = next.length + 1
            return TextFieldValue(rebuilt, TextRange(value.selection.start + shift, value.selection.end + shift))
        }
    }

    // -----------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------

    /** Offsets of the bracket at/before the caret and its partner, or null. */
    fun matchingBracket(text: String, cursor: Int): Pair<Int, Int>? {
        val candidates = listOfNotNull(
            cursor.takeIf { it < text.length },
            (cursor - 1).takeIf { it >= 0 }
        )
        for (index in candidates) {
            val c = text[index]
            if (c in "([{") {
                val closer = OPENERS.getValue(c)
                var depth = 1
                for (i in index + 1 until text.length) {
                    if (text[i] == c) depth++
                    if (text[i] == closer && --depth == 0) return index to i
                }
            }
            if (c in CLOSERS) {
                val opener = when (c) { ')' -> '('; ']' -> '['; else -> '{' }
                var depth = 1
                for (i in index - 1 downTo 0) {
                    if (text[i] == c) depth++
                    if (text[i] == opener && --depth == 0) return i to index
                }
            }
        }
        return null
    }

    /** 1-based (line, column) of [offset] in [text]. */
    fun lineColumn(text: String, offset: Int): Pair<Int, Int> {
        val at = offset.coerceIn(0, text.length)
        val before = text.substring(0, at)
        val line = before.count { it == '\n' } + 1
        val column = at - (before.lastIndexOf('\n') + 1) + 1
        return line to column
    }

    // ----- internals -----

    /** Start/end offsets of the full lines covered by the selection (end excludes `\n`). */
    private fun lineSpan(value: TextFieldValue): Pair<Int, Int> {
        val text = value.text
        val start = text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(0)).let {
            if (value.selection.min == 0) 0 else it + 1
        }
        val end = text.indexOf('\n', value.selection.max).let { if (it == -1) text.length else it }
        return start to end
    }

    private fun transformLines(value: TextFieldValue, transform: (String) -> String): TextFieldValue {
        val (start, end) = lineSpan(value)
        val block = value.text.substring(start, end)
        val replaced = block.split('\n').joinToString("\n", transform = transform)
        val text = value.text.substring(0, start) + replaced + value.text.substring(end)
        return TextFieldValue(text, TextRange(start, start + replaced.length))
    }
}
