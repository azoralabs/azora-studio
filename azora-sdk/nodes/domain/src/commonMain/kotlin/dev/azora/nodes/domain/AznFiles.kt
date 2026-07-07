package dev.azora.nodes.domain

import dev.azora.canvas.domain.model.AzoraGraphModel
import kotlinx.serialization.json.Json

/**
 * File-format helpers shared by everything that reads or writes `.azn` node
 * graphs and their generated `.az` sources.
 *
 * A `.azn` file is a pretty-printed JSON [AzoraGraphModel]. When the Studio
 * compiles/runs a project, every `.azn` is lowered to a sibling `.az` marked
 * with [GENERATED_HEADER]; that marker is how the build integration knows a
 * `.az` file is safe to overwrite on the next run.
 */
object AznFiles {

    /** Extension of node-graph files (no dot). */
    const val AZN_EXTENSION = "azn"

    /** Extension of azora source files (no dot). */
    const val AZ_EXTENSION = "az"

    /** First line written into every `.az` generated from a node graph. */
    const val GENERATED_HEADER = "// GENERATED from a node graph (.azn) — edits will be overwritten on Run."

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Parses `.azn` file content into a graph, or `null` when malformed. */
    fun decode(content: String): AzoraGraphModel? =
        runCatching { json.decodeFromString<AzoraGraphModel>(content) }.getOrNull()

    /** Serializes a graph to `.azn` file content. */
    fun encode(graph: AzoraGraphModel): String =
        json.encodeToString(AzoraGraphModel.serializer(), graph)

    /** Wraps generated azora source with the [GENERATED_HEADER] marker. */
    fun withGeneratedHeader(source: String, aznFileName: String): String =
        "$GENERATED_HEADER\n// Source graph: $aznFileName\n\n$source"

    /** True when [azContent] was generated from a graph (and may be overwritten). */
    fun isGenerated(azContent: String): Boolean =
        azContent.trimStart().startsWith(GENERATED_HEADER)

    /** `Game.azn` → `Game.az` (same directory). */
    fun siblingAzPath(aznPath: String): String =
        aznPath.removeSuffix(".$AZN_EXTENSION") + ".$AZ_EXTENSION"

    /** `Game.az` → `Game.azn` (same directory). */
    fun siblingAznPath(azPath: String): String =
        azPath.removeSuffix(".$AZ_EXTENSION") + ".$AZN_EXTENSION"
}
