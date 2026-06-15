package dev.azora.sdk.docking.domain

import kotlinx.serialization.Serializable

/**
 * A floating (undocked) window containing dock content.
 *
 * Floating windows are independent windows that exist outside the main dock
 * container. They can be moved, resized, minimized, maximized, and docked
 * back into the main layout.
 *
 * ## Creating Floating Windows
 *
 * Floating windows are typically created by:
 * - Dragging a panel out of the dock area
 * - Using the "float" action on a panel header
 * - Programmatically via [DockAction.FloatPanel]
 *
 * ```kotlin
 * // Float a panel programmatically
 * stateManager.dispatch(DockAction.FloatPanel(
 *     panelId = "explorer",
 *     x = 100f,
 *     y = 100f,
 *     width = 400f,
 *     height = 300f
 * ))
 * ```
 *
 * ## Window States
 *
 * Floating windows support three special states:
 * - **Minimized**: Window collapses to title bar only
 * - **Maximized**: Window fills available space
 * - **Fullscreen**: Window covers entire screen without decorations
 *
 * These states are mutually exclusive with normal windowed mode.
 *
 * ## Native vs Embedded Windows
 *
 * On desktop platforms, floating windows can be rendered as:
 * - **Native OS windows**: Separate windows managed by the OS
 * - **Embedded windows**: Rendered within the main dock container
 *
 * The rendering mode is determined by the `renderFloatingWindows` parameter
 * in [DockContainer].
 *
 * @property id Unique identifier for the floating window
 * @property content The dock node tree contained in this window. Can be a
 *                   single panel ([DockNode.Leaf]), tabs ([DockNode.TabGroup]),
 *                   or splits ([DockNode.Split]).
 * @property x X position in screen coordinates (pixels from left edge)
 * @property y Y position in screen coordinates (pixels from top edge)
 * @property width Window width in pixels (constrained by panel minimums)
 * @property height Window height in pixels (constrained by panel minimums)
 * @property isMinimized Whether the window is minimized (collapsed to title bar)
 * @property isMaximized Whether the window is maximized (fills available space)
 * @property isFullscreen Whether the window is fullscreen (covers entire screen)
 *
 * @see DockLayout.floatingWindows
 * @see DockAction.FloatPanel
 * @see DockAction.DockFloatingWindow
 * @see DockDefaults for default position and size values
 */
@Serializable
data class FloatingWindow(
    val id: String,
    val content: DockNode,
    val x: Float = DockDefaults.FLOATING_WINDOW_X,
    val y: Float = DockDefaults.FLOATING_WINDOW_Y,
    val width: Float = DockDefaults.FLOATING_WINDOW_WIDTH,
    val height: Float = DockDefaults.FLOATING_WINDOW_HEIGHT,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val isFullscreen: Boolean = false
)