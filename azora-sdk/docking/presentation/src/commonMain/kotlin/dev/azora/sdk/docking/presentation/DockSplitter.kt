package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import dev.azora.sdk.docking.domain.DockOrientation
import dev.azora.sdk.docking.presentation.theme.*
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview

/**
 * Draggable divider between split panes in the dock layout.
 *
 * Renders a thin visible line with a larger invisible hit area for easier
 * mouse targeting. Supports both horizontal (left/right) and vertical
 * (top/bottom) orientations.
 *
 * ## Layout
 *
 * The splitter has two layers:
 * - **Outer hit area**: Larger invisible area for mouse interaction
 * - **Inner visible line**: Thin colored line showing the split position
 *
 * ```
 * Horizontal:           Vertical:
 * +---+                  +-----------+
 * |   |  <- hit area     |           |
 * | | |  <- visible      |===========|
 * |   |                  |           |
 * +---+                  +-----------+
 * ```
 *
 * ## Visual States
 *
 * - **Normal**: Subtle background color
 * - **Hovered**: Highlighted color to indicate interactivity
 * - **Dragging**: Active color during resize
 *
 * ## Sizing
 *
 * Dimensions are controlled by [DockDimensions]:
 * - `splitterSize`: Width of the visible line
 * - `splitterHitArea`: Width of the interactive area
 *
 * @param orientation Direction of the split (HORIZONTAL = vertical line, VERTICAL = horizontal line)
 * @param onDrag Called during drag with the pixel delta in the drag direction
 * @param modifier Modifier for the splitter container
 *
 * @see DockOrientation for orientation values
 * @see RenderSplitNode for usage in split layouts
 */
@Composable
fun DockSplitter(
    orientation: DockOrientation,
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }

    val color = when {
        isDragging -> colors.splitterBackgroundDragging
        isHovered -> colors.splitterBackgroundHover
        else -> colors.splitterBackground
    }

    // Outer box provides larger hit area for easier mouse targeting
    Box(
        modifier = modifier
            .then(
                when (orientation) {
                    DockOrientation.HORIZONTAL -> Modifier.fillMaxHeight().width(dimensions.splitterHitArea)
                    DockOrientation.VERTICAL -> Modifier.fillMaxWidth().height(dimensions.splitterHitArea)
                }
            )
            .hoverable(interactionSource)
            .pointerInput(orientation) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = when (orientation) {
                            DockOrientation.HORIZONTAL -> dragAmount.x
                            DockOrientation.VERTICAL -> dragAmount.y
                        }
                        onDrag(delta)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner box is the visible splitter line
        Box(
            modifier = when (orientation) {
                DockOrientation.HORIZONTAL -> Modifier.fillMaxHeight().width(dimensions.splitterSize)
                DockOrientation.VERTICAL -> Modifier.fillMaxWidth().height(dimensions.splitterSize)
            }.background(color)
        )
    }
}

@Preview
@Composable
private fun DockSplitter_Preview() = AzoraPreview {
    DockSplitter(
        orientation = DockOrientation.HORIZONTAL,
        onDrag = {}
    )
}