package dev.azora.sdk.docking.presentation.window

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.panel.*
import dev.azora.sdk.docking.presentation.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview
import kotlin.math.roundToInt

/**
 * Embedded floating window rendered within the dock container.
 *
 * This composable renders a floating panel as an overlay within the main
 * dock container, as opposed to native OS windows. It provides a draggable
 * header for moving and docking, resizable edges, and displays the panel's
 * content.
 *
 * ## Layout
 *
 * ```
 * +----------------------------------+
 * | [Header: Title]          [Close] |  <- Draggable header
 * +----------------------------------+
 * |                                  |
 * |         Panel Content            |
 * |                                  |
 * +----------------------------------+
 *                                  +-+
 *                                  |R|  <- Resize handles
 *                                  +-+
 * ```
 *
 * ## Features
 *
 * - **Draggable header**: Drag to move the window or dock it elsewhere
 * - **Resize handles**: Right edge, bottom edge, and corner for resizing
 * - **Shadow and border**: Visual elevation to distinguish from docked panels
 * - **Panel content**: Renders the associated panel's content from the registry
 *
 * ## Drag Behavior
 *
 * Dragging the header serves two purposes:
 * 1. Moves the floating window position via `onMove`
 * 2. Reports cursor position via `onDrag` for dock zone detection
 *
 * When released over a dock zone, the window can be re-docked into the layout.
 *
 * ## Resize Behavior
 *
 * Resize deltas are converted from pixels to dp before being applied to
 * maintain consistent sizing across different screen densities.
 *
 * @param window The floating window state (position, size, panel ID)
 * @param descriptor The panel's metadata, or null if not found
 * @param onMove Called during header drag with the movement delta
 * @param onResize Called during edge drag with new width and height in dp
 * @param onClose Called when the close button is clicked
 * @param onDragStart Called when header drag starts with cursor position
 * @param onDrag Called during header drag with current cursor position
 * @param onDragEnd Called when header drag ends
 * @param modifier Modifier for the window container
 *
 * @see FloatingWindow for the data model
 * @see ResizeHandle for the resize interaction
 * @see DockPanelHeader for the header component
 */
@Composable
fun DockFloatingWindow(
    window: FloatingWindow,
    descriptor: DockPanelDescriptor?,
    onMove: (Offset) -> Unit,
    onResize: (width: Float, height: Float) -> Unit,
    onClose: () -> Unit,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val registry = LocalDockPanelRegistry.current
    val density = LocalDensity.current

    // Use rememberUpdatedState to ensure callbacks always use the latest window dimensions
    val currentWindow by rememberUpdatedState(window)
    var headerPosition by remember { mutableStateOf(Offset.Zero) }
    // Track the absolute cursor position at drag start and cumulative delta
    var dragStartAbsolutePosition by remember { mutableStateOf(Offset.Zero) }
    var cumulativeDragDelta by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .offset { IntOffset(window.x.roundToInt(), window.y.roundToInt()) }
            .width(window.width.dp)
            .height(window.height.dp)
            .shadow(
                elevation = dimensions.floatingWindowShadowElevation,
                shape = RoundedCornerShape(dimensions.floatingWindowCornerRadius),
                ambientColor = colors.floatingWindowShadow,
                spotColor = colors.floatingWindowShadow
            )
            .clip(RoundedCornerShape(dimensions.floatingWindowCornerRadius))
            .background(colors.floatingWindowBackground)
            .border(
                width = 1.dp,
                color = colors.floatingWindowBorder,
                shape = RoundedCornerShape(dimensions.floatingWindowCornerRadius)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Draggable header - supports both moving the window and docking via drag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.floatingWindowHeaderBackground)
                    .onGloballyPositioned { coords ->
                        headerPosition = coords.positionInRoot()
                    }
                    .pointerInput(window.id) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartAbsolutePosition = headerPosition + offset
                                cumulativeDragDelta = Offset.Zero
                                onDragStart(dragStartAbsolutePosition)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                cumulativeDragDelta += dragAmount
                                // Current absolute cursor position = start position + total movement
                                onDrag(dragStartAbsolutePosition + cumulativeDragDelta)
                                // Also move the window
                                onMove(dragAmount)
                            },
                            onDragEnd = {
                                cumulativeDragDelta = Offset.Zero
                                onDragEnd()
                            },
                            onDragCancel = {
                                cumulativeDragDelta = Offset.Zero
                                onDragEnd()
                            }
                        )
                    }
            ) {
                descriptor?.let { panel ->
                    DockPanelHeader(
                        descriptor = panel,
                        onClose = onClose,
                    )
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                descriptor?.let { panel ->
                    val content = registry.getContent(panel.id)
                    content?.invoke()
                }
            }
        }

        // Resize handles - convert pixel delta to dp before adding to width/height
        ResizeHandle(
            position = ResizePosition.RIGHT,
            onResize = { deltaPx ->
                val deltaDp = with(density) { deltaPx.x.toDp().value }
                onResize(currentWindow.width + deltaDp, currentWindow.height)
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        ResizeHandle(
            position = ResizePosition.BOTTOM,
            onResize = { deltaPx ->
                val deltaDp = with(density) { deltaPx.y.toDp().value }
                onResize(currentWindow.width, currentWindow.height + deltaDp)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        ResizeHandle(
            position = ResizePosition.BOTTOM_RIGHT,
            onResize = { deltaPx ->
                val deltaXDp = with(density) { deltaPx.x.toDp().value }
                val deltaYDp = with(density) { deltaPx.y.toDp().value }
                onResize(currentWindow.width + deltaXDp, currentWindow.height + deltaYDp)
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Preview
@Composable
private fun DockFloatingWindow_Preview() = AzoraPreview {
    DockFloatingWindow(
        window = FloatingWindow(
            id = "window1",
            content = DockNode.Leaf("node1", "panel"),
            x = 0f,
            y = 0f,
            width = 300f,
            height = 200f
        ),
        descriptor = DockPanelDescriptor("panel", "Floating Panel"),
        onMove = {},
        onResize = { _, _ -> },
        onClose = {}
    )
}