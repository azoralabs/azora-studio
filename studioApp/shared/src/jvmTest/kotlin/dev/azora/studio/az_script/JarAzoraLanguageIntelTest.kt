package dev.azora.studio.az_script

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test against the installed language server (~/.azora/azls/azls.jar).
 * Skips silently when the jar is not installed so CI stays green.
 */
class JarAzoraLanguageIntelTest {

    private val intel = JarAzoraLanguageIntel()

    @Test
    fun highlightsThroughTheJar() = runBlocking {
        if (!intel.available) return@runBlocking
        val spans = intel.highlight("func main() {\n    println(\"hi\")\n}")
        assertTrue(spans.any { it.type == "keyword" }, "$spans")
        assertTrue(spans.any { it.type == "string" }, "$spans")
    }

    @Test
    fun diagnosticsThroughTheJar() = runBlocking {
        if (!intel.available) return@runBlocking
        val clean = intel.diagnostics("func main() {\n    println(1)\n}", "", "")
        assertEquals(emptyList(), clean)
        val broken = intel.diagnostics("func main() {\n    var = 1\n}", "", "")
        assertTrue(broken.isNotEmpty())
        assertEquals(2, broken[0].line)
    }

    @Test
    fun completionsThroughTheJar() = runBlocking {
        if (!intel.available) return@runBlocking
        val source = "func main() {\n    pr\n}"
        val items = intel.complete(source, source.indexOf("pr") + 2, "", "")
        assertTrue(items.any { it.label == "println" }, "$items")
    }

    @Test
    fun engineLibrarySymbolsResolveViaPrelude() = runBlocking {
        if (!intel.available) return@runBlocking
        // gpuInit comes from the installed engine library sources (~/.azora/libraries).
        val source = "func main() {\n    gpuIni\n}"
        val items = intel.complete(source, source.indexOf("gpuIni") + 6, "/tmp/none.az", "/tmp")
        // Only assert when the engine library is installed on this machine.
        val hasEngine = java.io.File(System.getProperty("user.home"), ".azora/libraries").exists()
        if (hasEngine) assertTrue(items.any { it.label.startsWith("gpuInit") }, "$items")
    }
}
