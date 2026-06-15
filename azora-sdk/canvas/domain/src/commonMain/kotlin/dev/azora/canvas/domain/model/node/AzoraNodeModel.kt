package dev.azora.canvas.domain.model.node

import dev.azora.canvas.domain.type.AzoraNodeType
import kotlinx.serialization.Serializable

/**
 * A node instance placed on the canvas.
 *
 * The same [type] can appear many times in a graph; each placement is its own [AzoraNodeModel]
 * with a unique [id]. Type-specific configuration lives in [properties] as string-keyed/string-
 * valued entries so the model stays trivially serializable.
 *
 * Common [properties] keys by node type:
 * - `SET_VARIABLE` / `GET_VARIABLE` — `variableId` → id of the [AzoraNodeVar].
 * - `GET_CONSTANT` — `constantId` → id of a project-level constant.
 * - `CAST` — `fromType` and `toType` → [AzoraNodeDataType] names.
 * - `PRINT` — `prefix` → optional label printed before the value.
 * - `FUNCTION_DEF` / `FUNCTION_CALL` — `functionId` (and `name` on `FUNCTION_DEF`).
 * - `ENUM_DEF` — `enumId`, `name`, `values` (comma-separated case names).
 * - `ENUM_VALUE` — `enumId`, `value`.
 * - `DATA_CLASS_DEF` — `classId`, `name`, `fields` (JSON-encoded field list).
 * - `DATA_CLASS_CREATE` — `classId`.
 * - `DATA_CLASS_GET_FIELD` / `DATA_CLASS_SET_FIELD` — `classId`, `fieldName`.
 * - `MATCH` — `caseCount` → number of case branches; per-case values stored as `case_0`, `case_1`, ...
 *
 * @property id Stable identifier; unique within the containing graph.
 * @property screenId Optional id linking this node to a screen for screen-flow graphs.
 * @property title Header text shown on the rendered node.
 * @property type Determines port layout via [dev.azora.canvas.domain.AzoraPortDefinition] and
 *   runtime behavior in the interpreter.
 * @property positionX Canvas-space x coordinate in dp.
 * @property positionY Canvas-space y coordinate in dp.
 * @property width Node body width in dp; defaults to 160 and is clamped to 120–240 by [calculateWidth].
 * @property properties Type-specific configuration. See list above for shape per [type].
 */
@Serializable
data class AzoraNodeModel(
    val id: String,
    val screenId: String = "",
    val title: String = "",
    val type: AzoraNodeType,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val width: Float = 160f,
    val properties: Map<String, String> = emptyMap()
) {

    companion object {

        /**
         * Estimate a node body width from its [title] so headers don't truncate.
         *
         * Approximates the intrinsic header size as `icon(16) + spacing(8) + text + padding(24) + buffer(16)`,
         * with text width approximated as `title.length * 8` for a 12sp font. The result is clamped to the
         * 120–240 dp range used elsewhere in the canvas presentation layer.
         */
        fun calculateWidth(title: String): Float {
            val estimatedTextWidth = title.length * 8f
            val totalWidth = 16f + 8f + estimatedTextWidth + 24f + 16f
            return totalWidth.coerceIn(120f, 240f)
        }
    }
}