package dev.azora.sdk.docking.presentation.tab

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.presentation.theme.*
import dev.azora.sdk.docking.presentation.theme.LocalAllowExternalDrag
import kotlin.math.abs

/**
 * Individual tab in a dock tab bar.
 *
 * Displays a panel's title with optional close button, supporting both
 * internal reordering (horizontal drag) and external docking (vertical drag).
 *
 * ## Layout
 *
 * ```
 * +---------------------------+
 * | Panel Title        [X]   |
 * +===========================+  <- Active indicator (if active)
 * ```
 *
 * ## Drag Behavior
 *
 * The tab supports two drag modes:
 *
 * 1. **Internal drag** (horizontal): Reorders tabs within the same tab bar.
 *    Small horizontal movements trigger `onDrag` for reordering.
 *
 * 2. **External drag** (vertical): Initiates dock-wide drag operation.
 *    When dragged more than 30px vertically, switches to external mode
 *    and calls `onExternalDragStart`. This allows the tab to be moved
 *    to a different panel location or floated.
 *
 * ## Visual States
 *
 * - **Normal**: Default tab background
 * - **Hovered**: Highlighted background
 * - **Active**: Active background with bottom indicator line
 * - **Dragging**: Semi-transparent to indicate movement
 * - **Hidden**: 50% alpha when being dragged externally
 *
 * @param descriptor Panel metadata (title, ID, closeable status)
 * @param isActive Whether this tab is the currently selected tab
 * @param isDragging Whether this tab is being dragged for reordering
 * @param isHidden Whether to render at reduced opacity (during external drag)
 * @param onClick Called when the tab is clicked to select it
 * @param onClose Called when the close button is clicked
 * @param onDragStart Called when internal drag begins
 * @param onDrag Called during internal drag with the delta offset
 * @param onDragEnd Called when internal drag ends
 * @param onExternalDragStart Called when drag switches to external mode
 * @param onExternalDrag Called during external drag with cursor position
 * @param onExternalDragEnd Called when external drag completes
 * @param onExternalDragCancel Called when external drag is cancelled
 * @param modifier Modifier for the tab container
 *
 * @see DockTabBar for the parent tab bar
 * @see TabCloseButton for the close button component
 */
@Composable
internal fun DockTab(
    descriptor: DockPanelDescriptor,
    isActive: Boolean,
    isDragging: Boolean,
    isHidden: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onExternalDragStart: (Offset) -> Unit,
    onExternalDrag: (Offset) -> Unit,
    onExternalDragEnd: () -> Unit,
    onExternalDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val allowExternalDrag = LocalAllowExternalDrag.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var tabPosition by remember { mutableStateOf(Offset.Zero) }
    var tabSize by remember { mutableStateOf(IntSize.Zero) }
    var isExternalDrag by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var totalDragDistance by remember { mutableStateOf(Offset.Zero) }

    val background = when {
        isDragging -> colors.tabBackgroundActive.copy(alpha = 0.8f)
        isActive -> colors.tabBackgroundActive
        isHovered -> colors.tabBackgroundHover
        else -> colors.tabBackground
    }

    val textColor = when {
        isActive -> colors.tabTextActive
        isHovered -> colors.tabText.copy(alpha = 0.8f)
        else -> colors.tabText
    }

    // Use alpha 0.5 when hidden to keep gesture handler active
    val alpha = if (isHidden) 0.5f else 1f

    Column(
        modifier = modifier
            .height(dimensions.tabHeight)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(dimensions.tabCornerRadius))
                .background(background)
                .hoverable(interactionSource)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .onGloballyPositioned { coords ->
                    tabPosition = coords.positionInRoot()
                    tabSize = coords.size
                }
                .pointerInput(descriptor.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartOffset = offset
                            totalDragDistance = Offset.Zero
                            isExternalDrag = false
                            onDragStart(offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragDistance += dragAmount

                            // If dragged far enough vertically, switch to external drag mode
                            if (allowExternalDrag && !isExternalDrag && abs(totalDragDistance.y) > 30) {
                                isExternalDrag = true
                                onExternalDragStart(tabPosition + dragStartOffset + totalDragDistance)
                            }

                            if (isExternalDrag) {
                                onExternalDrag(tabPosition + dragStartOffset + totalDragDistance)
                            } else {
                                onDrag(dragAmount)
                            }
                        },
                        onDragEnd = {
                            if (isExternalDrag) {
                                // Check if cursor is back over the tab - cancel the drag
                                val cursorPos = tabPosition + dragStartOffset + totalDragDistance
                                val tabBounds = Rect(
                                    tabPosition.x,
                                    tabPosition.y,
                                    tabPosition.x + tabSize.width,
                                    tabPosition.y + tabSize.height
                                )
                                if (tabBounds.contains(cursorPos)) {
                                    onExternalDragCancel()
                                } else {
                                    onExternalDragEnd()
                                }
                            } else {
                                onDragEnd()
                            }
                            isExternalDrag = false
                            totalDragDistance = Offset.Zero
                        },
                        onDragCancel = {
                            if (isExternalDrag) {
                                onExternalDragCancel()
                            } else {
                                onDragEnd()
                            }
                            isExternalDrag = false
                            totalDragDistance = Offset.Zero
                        }
                    )
                }
                .padding(horizontal = dimensions.tabPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = descriptor.title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (descriptor.closeable) {
                Spacer(modifier = Modifier.width(6.dp))
                TabCloseButton(
                    onClick = onClose
                )
            }
        }

        // Active indicator line
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensions.tabIndicatorHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                    .background(colors.tabIndicator)
            )
        } else {
            Spacer(modifier = Modifier.height(dimensions.tabIndicatorHeight))
        }
    }
}