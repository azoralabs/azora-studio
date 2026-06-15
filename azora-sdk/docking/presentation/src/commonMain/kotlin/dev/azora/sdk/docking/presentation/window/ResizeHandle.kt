package dev.azora.sdk.docking.presentation.window

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions

/**
 * Invisible draggable handle for resizing floating windows.
 *
 * Provides a hit area along window edges or corners that responds to drag
 * gestures and reports the drag delta to resize the window.
 *
 * ## Handle Types
 *
 * - **RIGHT**: Vertical strip along the right edge (resizes width)
 * - **BOTTOM**: Horizontal strip along the bottom edge (resizes height)
 * - **BOTTOM_RIGHT**: Square area at the corner (resizes both dimensions)
 *
 * ## Sizing
 *
 * Handle dimensions are determined by [DockDimensions.floatingWindowResizeHandleSize]:
 * - Edge handles: One dimension is the handle size, the other fills the edge
 * - Corner handle: Both dimensions are 2x the handle size for easier targeting
 *
 * ## Visual Design
 *
 * Handles are invisible (no background) but change the cursor on hover
 * (via platform-specific implementations). The larger hit area improves
 * usability without cluttering the visual design.
 *
 * @param position Which edge or corner this handle controls
 * @param onResize Called with the drag delta during resize
 * @param modifier Modifier for positioning (typically [Modifier.align])
 *
 * @see ResizePosition for available handle positions
 * @see DockFloatingWindow for the parent window
 */
@Composable
internal fun ResizeHandle(
    position: ResizePosition,
    onResize: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalDockDimensions.current

    val handleModifier = when (position) {
        ResizePosition.RIGHT -> Modifier
            .width(dimensions.floatingWindowResizeHandleSize)
            .fillMaxHeight()
        ResizePosition.BOTTOM -> Modifier
            .height(dimensions.floatingWindowResizeHandleSize)
            .fillMaxWidth()
        ResizePosition.BOTTOM_RIGHT -> Modifier
            .size(dimensions.floatingWindowResizeHandleSize * 2)
    }

    Box(
        modifier = modifier
            .then(handleModifier)
            .pointerInput(position) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onResize(dragAmount)
                    }
                )
            }
    )
}