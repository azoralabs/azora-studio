package dev.azora.canvas.domain.model.node

import kotlinx.serialization.Serializable

/**
 * User-defined function whose body is itself a sub-graph of nodes.
 *
 * Stored in [dev.azora.canvas.domain.model.AzoraGraphModel.functions] and referenced by
 * `FUNCTION_DEF` and `FUNCTION_CALL` nodes via the `functionId` property.
 *
 * @property id Stable identifier referenced from node properties.
 * @property name Display name used in the UI.
 * @property parameters Ordered list of parameters; their names become `param_<name>` input port keys
 *   on call nodes.
 * @property returnType Result type, or `null` for void functions (no `result` data output).
 */
@Serializable
data class AzoraNodeFunction(
    val id: String,
    val name: String,
    val parameters: List<AzoraNodeFunctionParam> = emptyList(),
    val returnType: AzoraNodeDataType? = null
)