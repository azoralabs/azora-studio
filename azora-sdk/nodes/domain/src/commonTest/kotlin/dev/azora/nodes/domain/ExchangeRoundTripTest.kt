package dev.azora.nodes.domain

import org.azora.lang.frontend.Lexer
import org.azora.lang.frontend.Parser
import org.azora.lang.frontend.Stmt
import org.azora.lang.frontend.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExchangeRoundTripTest {
    private fun parse(body: String): Stmt = Parser(Lexer("func main() {\n$body\n}").tokenize())
        .parse().functions.single().body.single()

    @Test fun exchangeLocationsSurviveSourceRoundTrips() {
        assertEquals(TokenType.EXCHANGE, Lexer("<>").tokenize().first().type)
        for (source in listOf("a <> b", "a.value <> b.value", "a[i] <> a[j]")) {
            val statement = parse(source)
            assertTrue(statement is Stmt.Exchange)
            val printed = AzSourcePrinter.printStmt(statement)
            assertEquals(source, printed)
            assertEquals(printed, AzSourcePrinter.printStmt(parse(printed)))
        }
    }

    @Test fun exchangeRequiresLocationsAndStatementPosition() {
        assertFailsWith<IllegalStateException> { parse("1 <> a") }
        assertFailsWith<IllegalStateException> { parse("a <> 1") }
        assertFailsWith<IllegalStateException> { parse("var result = (a <> b)") }
    }
}
