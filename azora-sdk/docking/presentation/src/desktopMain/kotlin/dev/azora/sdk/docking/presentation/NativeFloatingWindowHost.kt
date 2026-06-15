package dev.azora.sdk.docking.presentation

import androidx.compose.runtime.*
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.panel.LocalDockPanelRegistry

/**
 * Host composable that renders floating windows as native OS windows.
 *
 * This composable creates a separate native window for each [FloatingWindow] in the
 * layout. It should be called at the application level (outside of any window) to
 * properly create independent OS windows.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val stateManager = remember { DockStateManagerImpl(initialLayout) }
 *     val state by stateManager.state.collectAsState()
 *
 *     // Main application window
 *     Window(onCloseRequest = ::exitApplication) {
 *         DockContainer(
 *             layout = state.layout,
 *             dragState = state.dragState,
 *             onAction = { stateManager.dispatch(it) },
 *             renderFloatingWindows = false  // Don't render embedded
 *         )
 *     }
 *
 *     // Native floating windows (outside main window)
 *     NativeFloatingWindowHost(
 *         layout = state.layout,
 *         dragState = state.dragState,
 *         onAction = { stateManager.dispatch(it) },
 *         mainWindowScreenOffsetX = windowX,
 *         mainWindowScreenOffsetY = windowY,
 *         mainWindowDensity = density
 *     )
 * }
 * ```
 *
 * ## Coordinate System
 *
 * Drag-and-drop between the main window and floating windows requires coordinate
 * conversion. The `mainWindowScreenOffset` and `mainWindowDensity` parameters
 * enable accurate coordinate translation for:
 * - Detecting when a dragged panel enters a floating window
 * - Converting screen coordinates to main window local coordinates
 * - Creating new floating windows at the correct screen position
 *
 * ## Retina/HiDPI Support
 *
 * On Retina displays, the density factor is used to convert between logical
 * coordinates (used by Compose) and screen pixels (used by AWT windows).
 *
 * @param layout The current dock layout containing floating windows
 * @param dragState The current drag state for cross-window drag detection
 * @param onAction Callback to dispatch dock actions
 * @param mainWindowScreenOffsetX Main window's X position in screen pixels
 * @param mainWindowScreenOffsetY Main window's Y position in screen pixels
 * @param mainWindowDensity Display density factor (e.g., 2.0 for Retina)
 *
 * @see NativeFloatingWindow for individual floating window implementation
 * @see dev.azora.sdk.docking.presentation.container.DockContainer for the main dock area
 * @see FloatingWindow for floating window state
 */
@Composable
fun NativeFloatingWindowHost(
    layout: DockLayout,
    dragState: DragState?,
    onAction: (DockAction) -> Unit,
    mainWindowScreenOffsetX: Float = 0f,
    mainWindowScreenOffsetY: Float = 0f,
    mainWindowDensity: Float = 1f,
    isDarkMode: Boolean = false
) {
    val registry = LocalDockPanelRegistry.current

    layout.floatingWindows.forEach { floatingWindow ->
        key(floatingWindow.id) {
            NativeFloatingWindow(
                window = floatingWindow,
                layout = layout,
                registry = registry,
                dragState = dragState,
                onAction = onAction,
                mainWindowScreenOffsetX = mainWindowScreenOffsetX,
                mainWindowScreenOffsetY = mainWindowScreenOffsetY,
                mainWindowDensity = mainWindowDensity,
                isDarkMode = isDarkMode
            )
        }
    }
}