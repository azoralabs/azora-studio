package dev.azora.canvas.domain.model.node

import kotlinx.serialization.Serializable

/**
 * Mutable named slot at graph scope, read by `GET_VARIABLE` and written by `SET_VARIABLE` nodes.
 *
 * The interpreter seeds each variable from [defaultValue] (parsed against [type]) at the start of
 * a run, then mutates it as `SET_VARIABLE` nodes execute.
 *
 * @property id Stable identifier referenced from node properties as `variableId`.
 * @property name Human-readable name shown in the editor; safe to rename without breaking links.
 * @property type Declared variable type used for port coloring and value parsing.
 * @property defaultValue Initial value as a string; parsed into the runtime representation
 *   appropriate for [type]. Empty falls back to the type's own [AzoraNodeDataType.defaultValue].
 * @property readonly If true the editor disables `SET_VARIABLE` for this variable.
 * @property nullable Whether `null` is a legal value at runtime.
 */
@Serializable
data class AzoraNodeVar(
    val id: String,
    val name: String,
    val type: AzoraNodeDataType,
    val defaultValue: String = "",
    val readonly: Boolean = false,
    val nullable: Boolean = false
)