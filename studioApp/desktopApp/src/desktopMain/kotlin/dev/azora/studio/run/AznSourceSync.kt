package dev.azora.studio.run

import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import dev.azora.nodes.domain.AznFiles
import dev.azora.nodes.domain.NodesToAzConverter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

/**
 * Keeps a project's node graphs and azora sources in sync at build time.
 *
 * Before every Run / Hot Reload, [generateAll] walks the project for `.azn`
 * script graphs and lowers each one to a sibling `.az` file, so the engine
 * toolchain (which only understands `.az`) compiles exactly what the node
 * canvas shows. Generated files carry [AznFiles.GENERATED_HEADER]; a
 * hand-written `.az` sitting next to a graph is never overwritten — the graph
 * is skipped with a console warning instead.
 *
 * Plugin-owned `.azn` documents (website pages/components, which carry a
 * top-level `"type"` discriminator) are ignored — their template generators
 * handle them separately.
 */
object AznSourceSync {

    private val SKIPPED_DIRS = setOf(".git", ".gradle", ".idea", ".azora-build", "build", "node_modules")

    /** Lowers every script `.azn` under [projectDir] to its sibling `.az`. */
    fun generateAll(projectDir: File, console: ConsoleOutputManager) {
        val graphs = projectDir.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == AznFiles.AZN_EXTENSION }
            .toList()
        for (azn in graphs) {
            runCatching { generateOne(azn, console) }
                .onFailure { console.error("Nodes ▸ failed to generate from ${azn.name}: ${it.message}") }
        }
    }

    private fun generateOne(azn: File, console: ConsoleOutputManager) {
        val content = azn.readText()

        // Plugin node documents (e.g. website pages) carry a top-level "type".
        val root = runCatching { Json.parseToJsonElement(content) }.getOrNull() as? JsonObject ?: return
        if (root["type"] is JsonPrimitive) return

        val graph = AznFiles.decode(content) ?: return
        if (graph.nodes.isEmpty() && graph.meta.isEmpty()) return

        val azFile = File(azn.parentFile, azn.nameWithoutExtension + ".${AznFiles.AZ_EXTENSION}")
        if (azFile.exists() && !AznFiles.isGenerated(azFile.readText())) {
            console.error("Nodes ▸ skipping ${azn.name}: ${azFile.name} is hand-written (delete it to regenerate from the graph).")
            return
        }

        val result = NodesToAzConverter().convert(graph)
        result.warnings.forEach { console.print("Nodes ▸ [${azn.name}] $it") }
        azFile.writeText(AznFiles.withGeneratedHeader(result.source, azn.name))
        console.info("Nodes ▸ generated ${azFile.name} from ${azn.name}")
    }
}
