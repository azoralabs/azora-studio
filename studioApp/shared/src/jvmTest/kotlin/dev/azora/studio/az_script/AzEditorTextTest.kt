package dev.azora.studio.az_script

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AzEditorTextTest {

    private fun value(text: String, cursor: Int) = TextFieldValue(text, TextRange(cursor))

    /** Simulates typing [c] at the cursor and running the edit pipeline. */
    private fun type(text: String, cursor: Int, c: Char): TextFieldValue {
        val old = value(text, cursor)
        val proposed = TextFieldValue(
            text.substring(0, cursor) + c + text.substring(cursor),
            TextRange(cursor + 1)
        )
        return AzEditorText.processEdit(old, proposed)
    }

    // ---- smart enter ----

    @Test fun enterKeepsIndent() {
        val result = type("    var x = 1", 13, '\n')
        assertEquals("    var x = 1\n    ", result.text)
        assertEquals(18, result.selection.start)
    }

    @Test fun enterAfterBraceIndents() {
        val result = type("func main() {", 13, '\n')
        assertEquals("func main() {\n    ", result.text)
    }

    @Test fun enterBetweenBracesExpandsBlock() {
        val result = type("func main() {}", 13, '\n')
        assertEquals("func main() {\n    \n}", result.text)
        assertEquals(18, result.selection.start) // caret on the indented body line
    }

    // ---- auto close / type-over / pair delete ----

    @Test fun openParenAutoCloses() {
        val result = type("println", 7, '(')
        assertEquals("println()", result.text)
        assertEquals(8, result.selection.start)
    }

    @Test fun closingParenTypesOver() {
        val result = type("println()", 8, ')')
        assertEquals("println()", result.text)
        assertEquals(9, result.selection.start)
    }

    @Test fun noAutoCloseBeforeIdentifier() {
        val result = type("(name", 0, '(')
        assertEquals("((name", result.text)
    }

    @Test fun backspaceRemovesEmptyPair() {
        val old = value("println()", 8)
        val proposed = value("println)", 7) // user deleted '('
        val result = AzEditorText.processEdit(old, proposed)
        assertEquals("println", result.text)
        assertEquals(7, result.selection.start)
    }

    @Test fun quoteAutoClosesAndTypesOver() {
        val opened = type("x = ", 4, '"')
        assertEquals("x = \"\"", opened.text)
        val closed = type(opened.text, opened.selection.start, '"')
        assertEquals("x = \"\"", closed.text)
        assertEquals(6, closed.selection.start)
    }

    // ---- block ops ----

    @Test fun tabIndentsSelectionLines() {
        val v = TextFieldValue("a\nb\nc", TextRange(0, 5))
        assertEquals("    a\n    b\n    c", AzEditorText.indentSelection(v).text)
    }

    @Test fun shiftTabOutdents() {
        val v = TextFieldValue("    a\n    b", TextRange(0, 11))
        assertEquals("a\nb", AzEditorText.outdentSelection(v).text)
    }

    @Test fun commentToggleOnAndOff() {
        val v = TextFieldValue("var x = 1\nvar y = 2", TextRange(0, 19))
        val commented = AzEditorText.toggleLineComment(v)
        assertEquals("// var x = 1\n// var y = 2", commented.text)
        val uncommented = AzEditorText.toggleLineComment(commented)
        assertEquals("var x = 1\nvar y = 2", uncommented.text)
    }

    @Test fun duplicateLineCopiesBelow() {
        val result = AzEditorText.duplicateLine(value("aaa\nbbb", 1))
        assertEquals("aaa\naaa\nbbb", result.text)
        assertEquals(5, result.selection.start)
    }

    @Test fun deleteLineRemovesIt() {
        assertEquals("bbb", AzEditorText.deleteLine(value("aaa\nbbb", 1)).text)
        assertEquals("aaa", AzEditorText.deleteLine(value("aaa\nbbb", 5)).text)
    }

    @Test fun moveLineSwapsNeighbours() {
        assertEquals("bbb\naaa\nccc", AzEditorText.moveLine(value("aaa\nbbb\nccc", 5), up = true).text)
        assertEquals("bbb\naaa\nccc", AzEditorText.moveLine(value("aaa\nbbb\nccc", 1), up = false).text)
        assertEquals("aaa\nbbb", AzEditorText.moveLine(value("aaa\nbbb", 1), up = true).text)
    }

    // ---- queries ----

    @Test fun bracketMatching() {
        val text = "func f(a: [Int]) { }"
        assertEquals(6 to 15, AzEditorText.matchingBracket(text, 6))
        assertEquals(6 to 15, AzEditorText.matchingBracket(text, 16)) // caret just after ')'
        assertEquals(17 to 19, AzEditorText.matchingBracket(text, 17))
        assertNull(AzEditorText.matchingBracket("abc", 1))
    }

    @Test fun lineColumn() {
        assertEquals(1 to 1, AzEditorText.lineColumn("ab\ncd", 0))
        assertEquals(2 to 2, AzEditorText.lineColumn("ab\ncd", 4))
    }
}
