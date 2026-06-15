package dev.azora.canvas.domain.model.node

import kotlinx.serialization.Serializable

/**
 * User-defined data class - a named record with typed fields.
 *
 * Stored in [dev.azora.canvas.domain.model.AzoraGraphModel.dataClasses] and referenced by the
 * `DATA_CLASS_*` node family via the `classId` property. Drives dynamic input ports on
 * `DATA_CLASS_CREATE` (one per field) and the `fieldName` selector on getter/setter nodes.
 *
 * @property id Stable identifier referenced from node properties.
 * @property name Display name shown in the editor.
 * @property fields Ordered field list; order is preserved for UI layout but lookups are by name.
 */
@Serializable
data class AzoraNodeDataClass(
    val id: String,
    val name: String,
    val fields: List<AzoraNodeDataClassField>
)