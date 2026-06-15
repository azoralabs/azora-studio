package dev.azora.sdk.docking.presentation.window

/**
 * Position of a resize handle on a floating window.
 *
 * Defines which edge or corner a [ResizeHandle] controls, determining
 * both the handle's shape and which dimensions change during resize.
 *
 * ## Resize Behavior
 *
 * | Position | Affected Dimension | Handle Shape |
 * |----------|-------------------|--------------|
 * | [RIGHT] | Width only | Vertical strip |
 * | [BOTTOM] | Height only | Horizontal strip |
 * | [BOTTOM_RIGHT] | Width and height | Square corner |
 *
 * @see ResizeHandle
 */
internal enum class ResizePosition {

    /** Right edge - resizes window width. */
    RIGHT,

    /** Bottom edge - resizes window height. */
    BOTTOM,

    /** Bottom-right corner - resizes both width and height. */
    BOTTOM_RIGHT
}