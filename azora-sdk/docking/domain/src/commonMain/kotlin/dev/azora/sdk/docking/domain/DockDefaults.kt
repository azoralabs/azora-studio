package dev.azora.sdk.docking.domain

/**
 * Default values and constants for the docking system.
 *
 * This object provides consistent default values used throughout the docking SDK.
 * Using these constants ensures consistency and makes it easy to adjust defaults
 * globally if needed.
 *
 * ## Split Ratios
 *
 * When a panel is dropped on a zone, the space is split according to these ratios:
 * - [SPLIT_RATIO_PRIMARY] (0.3): Used for LEFT/TOP zones. The new panel takes 30%.
 * - [SPLIT_RATIO_SECONDARY] (0.7): Used for RIGHT/BOTTOM zones. The existing content keeps 70%.
 *
 * Split ratios are clamped to [MIN_SPLIT_RATIO]..[MAX_SPLIT_RATIO] to prevent
 * panels from becoming too small.
 *
 * ## Floating Windows
 *
 * Default position and size for new floating windows:
 * - Position: ([FLOATING_WINDOW_X], [FLOATING_WINDOW_Y])
 * - Size: [FLOATING_WINDOW_WIDTH] x [FLOATING_WINDOW_HEIGHT]
 *
 * ## Panel Constraints
 *
 * Default minimum dimensions for panels:
 * - [PANEL_MIN_WIDTH]: Minimum width before a panel resists shrinking
 * - [PANEL_MIN_HEIGHT]: Minimum height before a panel resists shrinking
 *
 * @see DockNode.Split for split node structure
 * @see FloatingWindow for floating window configuration
 * @see DockPanelDescriptor for panel constraints
 */
object DockDefaults {

    // Split ratios

    /** Default ratio for LEFT/TOP zones. New panel takes 30% of available space. */
    const val SPLIT_RATIO_PRIMARY = 0.3f

    /** Default ratio for RIGHT/BOTTOM zones. Existing content keeps 70% of space. */
    const val SPLIT_RATIO_SECONDARY = 0.7f

    /** Minimum allowed split ratio. Prevents panels from becoming too small. */
    const val MIN_SPLIT_RATIO = 0.1f

    /** Maximum allowed split ratio. Ensures both split children remain visible. */
    const val MAX_SPLIT_RATIO = 0.9f

    // Floating window defaults

    /** Default X position for new floating windows (in pixels). */
    const val FLOATING_WINDOW_X = 100f

    /** Default Y position for new floating windows (in pixels). */
    const val FLOATING_WINDOW_Y = 100f

    /** Default width for new floating windows (in pixels). */
    const val FLOATING_WINDOW_WIDTH = 400f

    /** Default height for new floating windows (in pixels). */
    const val FLOATING_WINDOW_HEIGHT = 300f

    // Panel defaults

    /** Default minimum width for panels (in pixels). */
    const val PANEL_MIN_WIDTH = 100f

    /** Default minimum height for panels (in pixels). */
    const val PANEL_MIN_HEIGHT = 100f
}