package dev.azora.sdk.docking.domain

import kotlinx.serialization.Serializable

/**
 * Orientation of a dock split node.
 *
 * When a [DockNode.Split] divides space between two child nodes, the orientation
 * determines whether the division is side-by-side (horizontal) or stacked (vertical).
 *
 * ## Visual Representation
 *
 * ```
 * HORIZONTAL:          VERTICAL:
 * ┌──────┬──────┐      ┌─────────────┐
 * │      │      │      │    first    │
 * │first │second│      ├─────────────┤
 * │      │      │      │   second    │
 * └──────┴──────┘      └─────────────┘
 * ```
 *
 * ## Usage with DockZone
 *
 * - [HORIZONTAL] is created when dropping on [DockZone.LEFT] or [DockZone.RIGHT]
 * - [VERTICAL] is created when dropping on [DockZone.TOP] or [DockZone.BOTTOM]
 *
 * @see DockNode.Split
 * @see DockZone
 */
@Serializable
enum class DockOrientation {

    /**
     * Children are arranged side-by-side (left to right).
     *
     * The first child appears on the left, the second on the right.
     * The [DockNode.Split.ratio] determines the horizontal split position.
     */
    HORIZONTAL,

    /**
     * Children are stacked vertically (top to bottom).
     *
     * The first child appears on top, the second on the bottom.
     * The [DockNode.Split.ratio] determines the vertical split position.
     */
    VERTICAL
}