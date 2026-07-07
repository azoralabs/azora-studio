package dev.azora.nodes.domain

import dev.azora.canvas.domain.type.AzoraNodeType
import org.azora.lang.frontend.Lexer
import org.azora.lang.frontend.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Round-trip tests for the .az ↔ .azn converters:
 * source → graph ([AzToNodesConverter]) → source ([NodesToAzConverter]).
 *
 * The regenerated source must parse and preserve the program's structure. It
 * is normalized (formatting/comments are not preserved), so assertions check
 * parse validity plus structural content, not byte equality with the input.
 */
class AzNodesRoundTripTest {

    private fun toGraph(source: String) : AzToNodesResult.Success {
        val result = AzToNodesConverter().convert(source, "Test")
        assertIs<AzToNodesResult.Success>(result, "az → nodes failed: ${(result as? AzToNodesResult.Failure)?.errors}")
        return result
    }

    private fun roundTrip(source: String): String {
        val graph = toGraph(source).graph
        val out = NodesToAzConverter().convert(graph)
        // The regenerated source must be valid azora.
        assertParses(out.source)
        return out.source
    }

    private fun assertParses(source: String) {
        try {
            Parser(Lexer(source).tokenize()).parse()
        } catch (e: Exception) {
            throw AssertionError("Generated source does not parse: ${e.message}\n--- source ---\n$source")
        }
    }

    // -----------------------------------------------------------------
    // Structured conversion
    // -----------------------------------------------------------------

    @Test
    fun helloWorldBecomesStartAndPrint() {
        val result = toGraph(
            """
            func main() {
                println("Hello, world!")
            }
            """.trimIndent()
        )
        val types = result.graph.nodes.values.map { it.type }
        assertTrue(AzoraNodeType.START in types, "expected START node")
        assertTrue(AzoraNodeType.PRINT in types, "expected PRINT node")
        assertTrue(AzoraNodeType.AZ_CODE !in types, "hello world should not need raw-code nodes")
        // The literal is inlined on the PRINT node, not a separate node.
        val print = result.graph.nodes.values.first { it.type == AzoraNodeType.PRINT }
        assertEquals("Hello, world!", print.properties["literal_value"])
    }

    @Test
    fun helloWorldRoundTrip() {
        val out = roundTrip(
            """
            func main() {
                println("Hello, world!")
            }
            """.trimIndent()
        )
        assertTrue("func main()" in out, out)
        assertTrue("println(\"Hello, world!\")" in out, out)
    }

    @Test
    fun variablesAndArithmeticRoundTrip() {
        val out = roundTrip(
            """
            func main() {
                var total = 0
                total = total + 5
                println(total)
            }
            """.trimIndent()
        )
        assertTrue("var total: Int = 0" in out, out)
        assertTrue("total = (total + 5)" in out, out)
        assertTrue("println(total)" in out, out)
    }

    @Test
    fun ifElseRoundTrip() {
        val source = """
            func main() {
                var x = 5
                if x > 3 {
                    println("big")
                } else {
                    println("small")
                }
                println("done")
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.IF })

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("if (x > 3) {" in out, out)
        assertTrue("} else {" in out, out)
        // Both branches merge back — "done" prints once, after the if.
        assertEquals(1, Regex("println\\(\"done\"\\)").findAll(out).count(), out)
        val ifIndex = out.indexOf("if (x > 3)")
        val doneIndex = out.indexOf("println(\"done\")")
        assertTrue(doneIndex > ifIndex, "merge statement must come after the if\n$out")
    }

    @Test
    fun whileLoopRoundTrip() {
        val out = roundTrip(
            """
            func main() {
                var x = 20
                while x > 10 {
                    x = x - 4
                }
                println(x)
            }
            """.trimIndent()
        )
        assertTrue("while (x > 10) {" in out, out)
        assertTrue("x = (x - 4)" in out, out)
    }

    @Test
    fun forRangeRoundTrip() {
        val source = """
            func main() {
                var total = 0
                for i in 1..5 {
                    total = total + i
                }
                println(total)
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        val forNode = graph.nodes.values.first { it.type == AzoraNodeType.FOR_RANGE }
        assertEquals("i", forNode.properties["counter"])
        assertEquals("true", forNode.properties["inclusive"])

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("for i in 1..5 {" in out, out)
        assertTrue("total = (total + i)" in out, out)
    }

    @Test
    fun exclusiveRangeRoundTrip() {
        val out = roundTrip(
            """
            func main() {
                for i in 0..<3 {
                    println(i)
                }
            }
            """.trimIndent()
        )
        assertTrue("for i in 0..<3 {" in out, out)
    }

    @Test
    fun whenBecomesMatchAndBack() {
        val source = """
            func main() {
                var grade = 2
                when grade {
                    1 -> {
                        println("one")
                    }
                    2, 3 -> {
                        println("two or three")
                    }
                    else -> {
                        println("other")
                    }
                }
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        val match = graph.nodes.values.first { it.type == AzoraNodeType.MATCH }
        assertEquals("3", match.properties["caseCount"])
        assertEquals("1", match.properties["case_0"])
        assertEquals("2", match.properties["case_1"])
        assertEquals("3", match.properties["case_2"])

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("when grade {" in out, out)
        assertTrue("2, 3 -> {" in out, out)
        assertTrue("else -> {" in out, out)
    }

    @Test
    fun functionsAndCallsRoundTrip() {
        val source = """
            func add(a: Int, b: Int): Int {
                return a + b
            }
            func main() {
                let sum = add(2, 3)
                println(sum)
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        assertTrue(graph.functions.values.any { it.name == "add" })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.FUNCTION_DEF })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.FUNCTION_CALL })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.PARAM_GET })

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("func add(a: Int, b: Int): Int {" in out, out)
        assertTrue("return (a + b)" in out, out)
        assertTrue("add(2, 3)" in out, out)
    }

    @Test
    fun externalCallsBecomeAzCallNodes() {
        val source = """
            func main() {
                gpuInit()
                drawSprite(1, 2)
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        val calls = graph.nodes.values.filter { it.type == AzoraNodeType.AZ_CALL }
        assertEquals(2, calls.size)
        assertEquals(setOf("gpuInit", "drawSprite"), calls.map { it.properties["name"] }.toSet())

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("gpuInit()" in out, out)
        assertTrue("drawSprite(1, 2)" in out, out)
    }

    @Test
    fun packRoundTrip() {
        val source = """
            pack Point {
                var x: Int
                var y: Int
            }
            func main() {
                let p = Point(3, 4)
                p.x = p.x + 1
                println(p.x)
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        assertTrue(graph.dataClasses.values.any { it.name == "Point" })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.DATA_CLASS_CREATE })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.DATA_CLASS_GET_FIELD })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.DATA_CLASS_SET_FIELD })

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("pack Point {" in out, out)
        assertTrue("var x: Int" in out, out)
        assertTrue("Point(3, 4)" in out, out)
        assertTrue(".x = (" in out, out)
        assertTrue("println(p.x)" in out || "println(" in out, out)
    }

    @Test
    fun enumRoundTrip() {
        val source = """
            enum Color {
                Red
                Green
            }
            func main() {
                let c = Color.Red
                when c {
                    Red -> {
                        println("red")
                    }
                    else -> {
                        println("other")
                    }
                }
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        assertTrue(graph.enums.values.any { it.name == "Color" })
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.ENUM_VALUE })

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("enum Color {" in out, out)
        assertTrue("Color.Red" in out, out)
    }

    // -----------------------------------------------------------------
    // Fallbacks preserve semantics verbatim
    // -----------------------------------------------------------------

    @Test
    fun unsupportedStatementsBecomeAzCodeNodes() {
        val source = """
            func main() {
                let nums = [10, 20, 30]
                nums[1] = 25
                println(nums[1])
            }
        """.trimIndent()
        val graph = toGraph(source).graph
        assertTrue(graph.nodes.values.any { it.type == AzoraNodeType.AZ_CODE })

        val out = NodesToAzConverter().convert(graph).source
        assertParses(out)
        assertTrue("[10, 20, 30]" in out, out)
        assertTrue("nums[1] = 25" in out, out)
    }

    @Test
    fun stringInterpolationFallsBackButRegenerates() {
        val out = roundTrip(
            """
            func main() {
                var n = 5
                println("n = ${'$'}n!")
            }
            """.trimIndent()
        )
        assertTrue("\$n" in out, out)
    }

    @Test
    fun unsupportedTopLevelGoesToPreambleVerbatim() {
        val source = """
            bridge c {
                func az_time(): Real
            }

            func main() {
                println("hi")
            }
        """.trimIndent()
        val result = toGraph(source)
        val preamble = result.graph.meta["preamble"].orEmpty()
        assertTrue("bridge c {" in preamble, preamble)
        assertTrue("func az_time(): Real" in preamble, preamble)

        val out = NodesToAzConverter().convert(result.graph).source
        assertParses(out)
        assertTrue("bridge c {" in out, out)
    }

    @Test
    fun importsAreCarriedThrough() {
        val source = """
            use Math

            func main() {
                println(1)
            }
        """.trimIndent()
        val result = toGraph(source)
        assertEquals("use Math", result.graph.meta["imports"])
        val out = NodesToAzConverter().convert(result.graph).source
        assertTrue(out.startsWith("use Math"), out)
    }

    @Test
    fun parseErrorReportsFailure() {
        val result = AzToNodesConverter().convert("func main( {", "Broken")
        assertIs<AzToNodesResult.Failure>(result)
        assertTrue(result.errors.isNotEmpty())
    }

    // -----------------------------------------------------------------
    // Second-generation stability: az → azn → az → azn → az is a fixpoint
    // -----------------------------------------------------------------

    @Test
    fun secondGenerationIsStable() {
        val source = """
            func add(a: Int, b: Int): Int {
                return a + b
            }
            func main() {
                var total = 0
                for i in 1..5 {
                    total = add(total, i)
                }
                if total > 10 {
                    println("big")
                } else {
                    println(total)
                }
            }
        """.trimIndent()
        val gen1 = NodesToAzConverter().convert(toGraph(source).graph).source
        assertParses(gen1)
        val gen2 = NodesToAzConverter().convert(toGraph(gen1).graph).source
        assertEquals(gen1, gen2, "converting generated source again must be a fixpoint")
    }

    // -----------------------------------------------------------------
    // .azn file format
    // -----------------------------------------------------------------

    @Test
    fun aznEncodeDecodeRoundTrip() {
        val graph = toGraph(
            """
            func main() {
                println("persist me")
            }
            """.trimIndent()
        ).graph
        val encoded = AznFiles.encode(graph)
        val decoded = AznFiles.decode(encoded)
        assertEquals(graph, decoded)
    }

    @Test
    fun generatedHeaderDetection() {
        val wrapped = AznFiles.withGeneratedHeader("func main() {\n}\n", "Game.azn")
        assertTrue(AznFiles.isGenerated(wrapped))
        assertTrue(!AznFiles.isGenerated("func main() {\n}\n"))
        assertEquals("/p/Game.az", AznFiles.siblingAzPath("/p/Game.azn"))
        assertEquals("/p/Game.azn", AznFiles.siblingAznPath("/p/Game.az"))
    }
}
