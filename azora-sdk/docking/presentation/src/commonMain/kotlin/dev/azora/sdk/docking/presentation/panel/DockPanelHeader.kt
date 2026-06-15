package dev.azora.sdk.docking.presentation.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.presentation.theme.*
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview

/**
 * Header bar for a dock panel displaying the title and close button.
 *
 * The header shows the panel's title and provides a close button when the
 * panel is closeable. It also supports drag gestures for initiating panel
 * drag-and-drop operations.
 *
 * ## Layout
 *
 * ```
 * +----------------------------------+
 * | Panel Title              [Close] |
 * +----------------------------------+
 * ```
 *
 * ## Drag Support
 *
 * When drag callbacks are provided, the entire header becomes draggable.
 * Dragging the header initiates a drag operation that can be used to:
 * - Move the panel to a different location in the dock layout
 * - Float the panel as a separate window
 * - Combine with another panel as tabs
 *
 * ## Visual States
 *
 * - **Normal**: Standard header background
 * - **Dragging**: Slightly transparent background to indicate drag in progress
 *
 * @param descriptor The panel's metadata (title, ID, closeable status)
 * @param onClose Called when the close button is clicked
 * @param onDragStart Called when drag starts with the cursor position in root coordinates
 * @param onDrag Called during drag with the current cursor position in root coordinates
 * @param onDragEnd Called when drag ends or is cancelled
 * @param modifier Modifier for the header row
 *
 * @see DockPanel for the parent panel container
 * @see DockPanelDescriptor for panel metadata
 */
@Composable
fun DockPanelHeader(
    descriptor: DockPanelDescriptor,
    onClose: () -> Unit,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    var headerPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var currentDragOffset by remember { mutableStateOf(Offset.Zero) }

    val dragModifier = if (onDragStart != null) {
        Modifier
            .onGloballyPositioned { coords ->
                headerPosition = coords.positionInRoot()
            }
            .pointerInput(descriptor.id) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        currentDragOffset = offset
                        onDragStart(headerPosition + offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentDragOffset += dragAmount
                        onDrag?.invoke(headerPosition + currentDragOffset)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentDragOffset = Offset.Zero
                        onDragEnd?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        currentDragOffset = Offset.Zero
                        onDragEnd?.invoke()
                    }
                )
            }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.panelHeaderHeight)
            .background(
                if (isDragging) colors.panelHeaderBackground.copy(alpha = 0.8f)
                else colors.panelHeaderBackground
            )
            .then(dragModifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = descriptor.title,
            color = colors.panelHeaderText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Close button
        if (descriptor.closeable) {
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(
                iconRes = Res.drawable.ic_dock_close,
                contentDescription = "Close panel",
                onClick = onClose
            )
        }
    }
}

@Preview
@Composable
private fun DockPanelHeader_Preview() = AzoraPreview {
    DockPanelHeader(
        descriptor = DockPanelDescriptor("explorer", "Explorer"),
        onClose = {}
    )
}