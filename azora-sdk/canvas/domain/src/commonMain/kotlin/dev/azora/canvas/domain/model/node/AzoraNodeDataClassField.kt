package dev.azora.canvas.domain.model.node

import kotlinx.serialization.Serializable

/**
 * A single field on a user-defined [AzoraNodeDataClass].
 *
 * Field [name] is the lookup key on `DATA_CLASS_GET_FIELD` / `DATA_CLASS_SET_FIELD` nodes and on
 * the dynamic `field_<name>` input ports of `DATA_CLASS_CREATE`.
 *
 * @property name Field name; must be unique within its data class.
 * @property type Declared field type used for port coloring and runtime checks.
 */
@Serializable
data class AzoraNodeDataClassField(
    val name: String,
    val type: AzoraNodeDataType
)
