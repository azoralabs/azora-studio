package dev.azora.canvas.domain.model.node

import kotlinx.serialization.Serializable

/**
 * User-defined enumeration - a named set of string-valued cases.
 *
 * Stored in [dev.azora.canvas.domain.model.AzoraGraphModel.enums] and referenced by `ENUM_DEF` /
 * `ENUM_VALUE` nodes via the `enumId` property. `ENUM_VALUE` selects one entry from [values] and
 * emits it as an [AzoraNodeDataType.ENUM] on its data output.
 *
 * @property id Stable identifier referenced from node properties.
 * @property name Display name shown in the editor.
 * @property values Ordered list of case names. Order is preserved for the picker UI.
 */
@Serializable
data class AzoraNodeEnum(
    val id: String,
    val name: String,
    val values: List<String>
)