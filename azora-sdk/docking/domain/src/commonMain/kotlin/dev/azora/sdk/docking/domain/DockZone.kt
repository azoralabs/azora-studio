package dev.azora.sdk.docking.domain

import kotlinx.serialization.Serializable

/**
 * Drop zones for docking panels relative to a target.
 *
 * When a panel is dragged over another panel or container, the dock zone
 * determines where the dropped panel will be placed. The UI typically shows
 * visual indicators for each zone during drag operations.
 *
 * ## Visual Representation
 *
 * ```
 * ┌─────────────────────────┐
 * │          TOP            │
 * ├─────┬───────────┬───────┤
 * │     │           │       │
 * │LEFT │  CENTER   │ RIGHT │
 * │     │           │       │
 * ├─────┴───────────┴───────┤
 * │        BOTTOM           │
 * └─────────────────────────┘
 * ```
 *
 * ## Zone Behavior
 *
 * - **Edge zones** ([LEFT], [RIGHT], [TOP], [BOTTOM]): Create a new split,
 *   placing the dropped panel on the specified edge. The split ratio defaults
 *   to [DockDefaults.SPLIT_RATIO_PRIMARY] (30%) for the new panel.
 *
 * - **Center zone** ([CENTER]): Adds the panel as a new tab in the target.
 *   If the target is a leaf, it's converted to a tab group.
 *
 * ## Related Operations
 *
 * Zones are used in several actions:
 * - [DockAction.AddPanel] - Add panel at a zone
 * - [DockAction.MovePanel] - Move panel to a zone
 * - [DockAction.DockFloatingWindow] - Dock floating window at a zone
 *
 * @see DockOrientation for split direction
 * @see DockDefaults.SPLIT_RATIO_PRIMARY
 */
@Serializable
enum class DockZone {

    /**
     * Left edge of the target.
     *
     * Creates a [DockOrientation.HORIZONTAL] split with the new panel on the left.
     */
    LEFT,

    /**
     * Right edge of the target.
     *
     * Creates a [DockOrientation.HORIZONTAL] split with the new panel on the right.
     */
    RIGHT,

    /**
     * Top edge of the target.
     *
     * Creates a [DockOrientation.VERTICAL] split with the new panel on top.
     */
    TOP,

    /**
     * Bottom edge of the target.
     *
     * Creates a [DockOrientation.VERTICAL] split with the new panel on the bottom.
     */
    BOTTOM,

    /**
     * Center of the target (tab merge).
     *
     * Adds the panel as a new tab in the target node. If the target is a
     * [DockNode.Leaf], it's converted to a [DockNode.TabGroup].
     */
    CENTER
}