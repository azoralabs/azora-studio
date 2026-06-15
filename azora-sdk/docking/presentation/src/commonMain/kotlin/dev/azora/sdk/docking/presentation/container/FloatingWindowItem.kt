package dev.azora.sdk.docking.presentation.container

import androidx.compose.runtime.*
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.window.DockFloatingWindow

/**
 * Wrapper composable that connects a [FloatingWindow] state to the
 * [DockFloatingWindow] UI component.
 *
 * This internal component handles the translation between the domain
 * [FloatingWindow] model and the presentation layer, wiring up callbacks
 * to dispatch appropriate [DockAction]s.
 *
 * ## Responsibilities
 *
 * - Extracts panel information from the floating window content
 * - Translates UI events (move, resize, close, etc.) to dock actions
 * - Uses [rememberUpdatedState] to ensure callbacks use current window position
 *
 * ## Movement Handling
 *
 * Movement is handled incrementally: the `onMove` callback receives a delta
 * offset, which is added to the current window position to calculate the
 * new position.
 *
 * @param window The floating window state to render
 * @param layout The complete dock layout (for accessing panel descriptors)
 * @param onAction Callback to dispatch dock actions
 *
 * @see DockFloatingWindow for the UI component
 * @see FloatingWindow for the domain model
 */
@Composable
internal fun FloatingWindowItem(
    window: FloatingWindow,
    layout: DockLayout,
    onAction: (DockAction) -> Unit
) {
    val panelId = window.content.collectPanelIds().firstOrNull()
    val descriptor = panelId?.let { layout.panelDescriptors[it] }

    // Use rememberUpdatedState to ensure callbacks always use the latest window position
    val currentWindow by rememberUpdatedState(window)

    DockFloatingWindow(
        window = window,
        descriptor = descriptor,
        onMove = { delta ->
            onAction(
                DockAction.MoveFloatingWindow(
                    currentWindow.id,
                    currentWindow.x + delta.x,
                    currentWindow.y + delta.y
                )
            )
        },
        onResize = { width, height ->
            onAction(DockAction.ResizeFloatingWindow(currentWindow.id, width, height))
        },
        onClose = {
            panelId?.let { onAction(DockAction.RemovePanel(it)) }
        },
        onDragStart = { offset ->
            panelId?.let {
                onAction(DockAction.StartDragFromFloating(it, currentWindow.id, offset.x, offset.y))
            }
        },
        onDrag = { offset ->
            onAction(DockAction.UpdateDragPosition(offset.x, offset.y))
        },
        onDragEnd = {
            onAction(DockAction.EndDrag)
        }
    )
}