package dev.azora.canvas.domain.type

import kotlinx.serialization.Serializable

/**
 * Classification of an execution-flow port on a navigation node.
 *
 * Drives port shape and color in the presentation layer and determines navigation semantics
 * when the link is followed at runtime:
 * - [NAV_ROOT] - entry point of a navigation graph.
 * - [NAV_PUSH] - push the target onto the navigation stack.
 * - [NAV_REPLACE] - replace the current entry with the target.
 * - [NAV_PUSH_REPLACE] - combined push/replace; rendered as a split port (see [mustSplitInHalf]).
 * - [NAV_DIALOG] - open the target as a dialog/modal.
 *
 * For data-flow port classification see [AzoraPortDataType].
 */
@Serializable
enum class AzoraPortType {
    NAV_ROOT,
    NAV_PUSH,
    NAV_REPLACE,
    NAV_PUSH_REPLACE,
    NAV_DIALOG;

    /**
     * Whether the port should be rendered as a split shape with two colors.
     * Currently only true for [NAV_PUSH_REPLACE] so users can tell the dual semantics at a glance.
     */
    val mustSplitInHalf: Boolean
        get() = this == NAV_PUSH_REPLACE

    companion object {

        /**
         * Convert from a persisted/database string back to an [AzoraPortType].
         * Falls back to [NAV_PUSH] for unknown values so older records remain loadable.
         */
        fun fromDbString(value: String) = entries.find { it.name == value } ?: NAV_PUSH
    }
}
