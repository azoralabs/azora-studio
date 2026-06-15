package dev.azora.canvas.domain.model.node

import kotlinx.serialization.Serializable

/**
 * A single parameter of a user-defined [AzoraNodeFunction].
 *
 * @property id Stable identifier independent of [name] so renames don't break references.
 * @property name Parameter name; surfaces on `FUNCTION_CALL` nodes as a `param_<name>` data input port.
 * @property type Declared parameter type used for type-checking and default-value parsing.
 * @property nullable Whether the parameter accepts `null`. When false, the interpreter substitutes
 *   the type's default value if no input is connected.
 */
@Serializable
data class AzoraNodeFunctionParam(
    val id: String,
    val name: String,
    val type: AzoraNodeDataType,
    val nullable: Boolean = false
)