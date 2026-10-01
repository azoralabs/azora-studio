package dev.azora.nodes.domain

import org.azora.lang.frontend.Lexer
import org.azora.lang.frontend.Parser
import org.azora.lang.frontend.Stmt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CollectionLiteralRoundTripTest {
    private fun parse(body: String): Stmt = Parser(Lexer("func main() {\n$body\n}").tokenize())
        .parse().functions.single().body.single()

    @Test fun shapesAndTypeContextSurviveRoundTrips() {
        for (source in listOf(
            "fin x: Array<Int> = [1, 2, 3]",
            "fin x: List<Int> = []",
            "fin x: Set<Int> = [1, 1]",
            "fin x: Map<String, Int> = [:]",
            "fin x = [\n1: 2,\n3: 4,\n]",
            "fin x = [[1], [2]][1][0]",
        )) {
            val printed = AzSourcePrinter.printStmt(parse(source))
            assertEquals(printed, AzSourcePrinter.printStmt(parse(printed)))
            if ("[:]" in source) assertTrue("[:]" in printed)
        }
    }

    @Test fun mixedShapesRemainInvalid() {
        for (source in listOf("[1, 2: 3]", "[1: 2, 3]", "[:1]", "![1, 2]")) {
            assertFailsWith<IllegalStateException> { parse("fin x = $source") }
        }
    }
}
