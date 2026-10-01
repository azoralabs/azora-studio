package dev.azora.nodes.domain

import org.azora.lang.frontend.Expr
import org.azora.lang.frontend.Lexer
import org.azora.lang.frontend.Parser
import org.azora.lang.frontend.Stmt
import org.azora.lang.frontend.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DescendingRangeRoundTripTest {
    private fun parse(body: String): Stmt = Parser(Lexer("func main() {\n$body\n}").tokenize())
        .parse().functions.single().body.single()

    @Test fun descendingRangesSurviveSourceRoundTrips() {
        for (bounds in listOf("5>..0", "0>..0", "-1>..-4")) {
            val loop = parse("for i in $bounds {}") as Stmt.For
            assertTrue((loop.iterable as Expr.Range).descending)
            val printed = AzSourcePrinter.printStmt(loop)
            assertTrue(">.." in printed)
            assertTrue(((parse(printed) as Stmt.For).iterable as Expr.Range).descending)
        }
    }

    @Test fun reverseIsANameAndCannotModifyLoops() {
        assertEquals(TokenType.IDENTIFIER, Lexer("reverse").tokenize().first().type)
        assertFailsWith<IllegalStateException> { parse("reverse for i in 0..<3 {}") }
    }
}
