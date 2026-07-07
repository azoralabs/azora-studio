package dev.azora.canvas.domain.model

import dev.azora.canvas.domain.model.node.*
import kotlinx.serialization.Serializable

/**
 * Top-level serializable description of a single node graph (script, screen flow, etc.).
 *
 * The graph is the unit of persistence and the input to [dev.azora.canvas.domain.interpreter.ScriptInterpreter].
 * Collections are keyed by stable id strings to make patches and lookups O(1) and to keep
 * cross-references (link source/target ids, node `properties` references to variables/functions/
 * enums/data classes) stable across edits.
 *
 * @property id Stable identifier of this graph.
 * @property name Display name shown in editor tabs.
 * @property nodes Every node placed on the canvas, keyed by node id.
 * @property execLinks Execution-flow edges between nodes, keyed by link id.
 * @property dataLinks Data-flow edges between node ports, keyed by link id.
 * @property variables Mutable named slots referenced by `GET_VARIABLE` / `SET_VARIABLE` nodes.
 * @property functions User-defined functions referenced by `FUNCTION_DEF` / `FUNCTION_CALL` nodes.
 * @property enums User-defined enums referenced by `ENUM_DEF` / `ENUM_VALUE` nodes.
 * @property dataClasses User-defined data classes referenced by the `DATA_CLASS_*` nodes.
 * @property meta Free-form metadata carried through save/load. Used by the .az ↔ .azn converters:
 *   `imports` (azora `use` lines) and `preamble` (verbatim top-level azora declarations that have
 *   no node representation, re-emitted as-is when generating .az).
 */
@Serializable
data class AzoraGraphModel(
    val id: String,
    val name: String = "Main",
    val nodes: Map<String, AzoraNodeModel> = emptyMap(),
    val execLinks: Map<String, AzoraExecLinkModel> = emptyMap(),
    val dataLinks: Map<String, AzoraDataLinkModel> = emptyMap(),
    val variables: Map<String, AzoraNodeVar> = emptyMap(),
    val functions: Map<String, AzoraNodeFunction> = emptyMap(),
    val enums: Map<String, AzoraNodeEnum> = emptyMap(),
    val dataClasses: Map<String, AzoraNodeDataClass> = emptyMap(),
    val meta: Map<String, String> = emptyMap(),
)