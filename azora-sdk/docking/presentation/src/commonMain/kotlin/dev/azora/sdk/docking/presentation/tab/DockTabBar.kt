package dev.azora.sdk.docking.presentation.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.*
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.presentation.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview

/**
 * Horizontal tab bar for displaying multiple panels in a tab group.
 *
 * Renders tabs for each panel with support for selection, closing,
 * reordering via drag, and external drag for docking operations.
 * Automatically shows an overflow button when tabs don't fit.
 *
 * ## Layout
 *
 * ```
 * +-----------------------------------------------+
 * | [Tab1] [Tab2*] [Tab3] [+2 v]                  |
 * +-----------------------------------------------+
 *           ^               ^
 *      active tab     overflow button
 * ```
 *
 * ## Tab Overflow
 *
 * When tabs exceed the available width, excess tabs are hidden and
 * accessible via an overflow dropdown menu. The overflow button shows
 * the count of hidden tabs and highlights when an active tab is hidden.
 *
 * ## Tab Reordering
 *
 * Tabs can be reordered by dragging horizontally. A drop indicator
 * appears at the potential drop position. Selecting a tab from the
 * overflow menu moves it to the front of the visible tabs.
 *
 * ## External Drag
 *
 * Dragging a tab vertically (>30px) initiates an external drag operation,
 * allowing the panel to be moved to a different location in the dock
 * layout or floated as a separate window.
 *
 * @param panels List of panel descriptors to display as tabs
 * @param activeIndex Index of the currently active tab
 * @param onTabSelect Called when a tab is clicked with its index
 * @param onTabClose Called when a tab's close button is clicked with panel ID
 * @param onTabReorder Called when tabs are reordered with from/to indices
 * @param onTabDragStart Called when external drag begins with panel ID and position
 * @param onTabDrag Called during external drag with panel ID and position
 * @param onTabDragEnd Called when external drag ends with panel ID
 * @param onTabDragCancel Called when external drag is cancelled with panel ID
 * @param modifier Modifier for the tab bar container
 *
 * @see DockTab for individual tab rendering
 * @see TabOverflowButton for the overflow menu
 */
@Composable
fun DockTabBar(
    panels: List<DockPanelDescriptor>,
    activeIndex: Int,
    onTabSelect: (Int) -> Unit,
    onTabClose: (String) -> Unit,
    onTabReorder: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onTabDragStart: (String, Offset) -> Unit,
    onTabDrag: (String, Offset) -> Unit = { _, _ -> },
    onTabDragEnd: (String) -> Unit = {},
    onTabDragCancel: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    var showOverflowMenu by remember { mutableStateOf(false) }
    var visibleCount by remember(panels.size) { mutableStateOf(panels.size) }

    // Reset tab bounds when panels change
    var tabBounds by remember(panels) { mutableStateOf<Map<Int, Rect>>(emptyMap()) }

    // Track drag state for reordering
    var draggedTabIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var potentialDropIndex by remember { mutableStateOf<Int?>(null) }

    // Reset drag state when panels change
    LaunchedEffect(panels) {
        draggedTabIndex = null
        dragOffset = Offset.Zero
        potentialDropIndex = null
    }

    Layout(
        content = {
            // Measure all tabs
            panels.forEachIndexed { index, panel ->
                val isBeingDragged = draggedTabIndex == index
                // Inserting before the dragged tab or before its right neighbor keeps the
                // order unchanged, so no indicator for those positions
                val showDropIndicator = potentialDropIndex == index && draggedTabIndex != null &&
                    draggedTabIndex != index && draggedTabIndex != index - 1

                var isExternalDragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            tabBounds = tabBounds + (index to coords.boundsInParent())
                        }
                ) {
                    // Drop indicator before this tab
                    if (showDropIndicator) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(dimensions.tabHeight - 8.dp)
                                .background(colors.tabIndicator, RoundedCornerShape(1.dp))
                                .align(Alignment.CenterStart)
                                .offset(x = (-2).dp)
                        )
                    }

                    DockTab(
                        descriptor = panel,
                        isActive = index == activeIndex,
                        isDragging = isBeingDragged,
                        isHidden = isExternalDragging,
                        onClick = { onTabSelect(index) },
                        onClose = { onTabClose(panel.id) },
                        onDragStart = { offset ->
                            // A drag consumes the pointer events, so clickable never fires;
                            // select here so a click with slight movement still switches tabs
                            onTabSelect(index)
                            draggedTabIndex = index
                            dragOffset = Offset.Zero
                        },
                        onDrag = { delta ->
                            dragOffset += delta
                            val bounds = tabBounds[index]
                            val currentCenterX = (bounds?.left ?: 0f) + (bounds?.width ?: 0f) / 2 + dragOffset.x
                            potentialDropIndex = findDropIndex(currentCenterX, tabBounds, visibleCount, index)
                        },
                        onDragEnd = {
                            val from = draggedTabIndex
                            val to = potentialDropIndex
                            if (from != null && to != null) {
                                // findDropIndex returns an insertion position ("before tab i");
                                // convert to the final index after removal so that dropping a tab
                                // next to itself is a no-op instead of a swap
                                val target = if (to > from) to - 1 else to
                                if (target != from) {
                                    onTabReorder(from, target)
                                }
                            }
                            draggedTabIndex = null
                            dragOffset = Offset.Zero
                            potentialDropIndex = null
                        },
                        onExternalDragStart = { offset ->
                            isExternalDragging = true
                            draggedTabIndex = null
                            dragOffset = Offset.Zero
                            potentialDropIndex = null
                            onTabDragStart(panel.id, offset)
                        },
                        onExternalDrag = { offset -> onTabDrag(panel.id, offset) },
                        onExternalDragEnd = {
                            isExternalDragging = false
                            onTabDragEnd(panel.id)
                        },
                        onExternalDragCancel = {
                            // Cancel the drag - clear hover and end drag without docking
                            isExternalDragging = false
                            // Cancel the drag in the dock system without any layout changes
                            onTabDragCancel(panel.id)
                        },
                        modifier = Modifier.offset {
                            if (isBeingDragged) {
                                IntOffset(dragOffset.x.toInt(), 0)
                            } else {
                                IntOffset.Zero
                            }
                        }
                    )
                }
            }

            // Overflow button (always include in content for measurement)
            TabOverflowButton(
                overflowPanels = panels.drop(visibleCount),
                activeOverflowIndex = if (activeIndex >= visibleCount) activeIndex - visibleCount else -1,
                showMenu = showOverflowMenu,
                onShowMenu = { showOverflowMenu = it },
                onTabSelect = { overflowIndex ->
                    val actualIndex = visibleCount + overflowIndex
                    onTabReorder(actualIndex, 0)
                    onTabSelect(0)
                }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.tabHeight)
            .background(colors.tabBarBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) { measurables, constraints ->
        val spacing = dimensions.tabSpacing.roundToPx()
        val availableWidth = constraints.maxWidth

        // Last measurable is the overflow button
        val tabMeasurables = measurables.dropLast(1)
        val overflowMeasurable = measurables.last()

        // Measure overflow button first to know its width
        val overflowPlaceable = overflowMeasurable.measure(Constraints(maxHeight = constraints.maxHeight))
        val overflowWidth = overflowPlaceable.width

        // Measure all tabs with wrap content width
        val tabPlaceables = tabMeasurables.map { it.measure(Constraints(maxHeight = constraints.maxHeight)) }

        // Calculate which tabs fit
        var usedWidth = 0
        var count = 0
        for (i in tabPlaceables.indices) {
            val tabWidth = tabPlaceables[i].width
            val spacingNeeded = if (i > 0) spacing else 0
            val remainingTabs = tabPlaceables.size - i - 1
            val overflowReserve = if (remainingTabs > 0) overflowWidth + spacing else 0

            if (usedWidth + spacingNeeded + tabWidth + overflowReserve > availableWidth && i > 0) {
                break
            }
            usedWidth += spacingNeeded + tabWidth
            count++
        }

        visibleCount = count.coerceAtLeast(1)
        val showOverflow = visibleCount < tabPlaceables.size

        layout(constraints.maxWidth, constraints.maxHeight) {
            var xPosition = 0

            // Place visible tabs
            for (i in 0 until visibleCount) {
                tabPlaceables[i].placeRelative(xPosition, 0)
                xPosition += tabPlaceables[i].width + spacing
            }

            // Place overflow button if needed
            if (showOverflow) {
                overflowPlaceable.placeRelative(xPosition, 0)
            }
        }
    }
}

/**
 * Calculates the target drop index for a tab being dragged.
 *
 * Compares the drag position against tab midpoints to determine
 * where the dragged tab should be inserted. The returned value is an
 * insertion position ("before tab i"), not a final index.
 *
 * @param dragX Current X position of the dragged tab's center
 * @param tabBounds Map of tab indices to their bounding rectangles
 * @param visibleCount Number of visible (non-overflow) tabs
 * @param draggedIndex Index of the tab being dragged
 * @return The index where the tab should be dropped
 */
private fun findDropIndex(
    dragX: Float,
    tabBounds: Map<Int, Rect>,
    visibleCount: Int,
    draggedIndex: Int
): Int {
    for (i in 0 until visibleCount) {
        if (i == draggedIndex) continue
        val bounds = tabBounds[i] ?: continue
        val midpoint = bounds.left + bounds.width / 2
        if (dragX < midpoint) {
            return i
        }
    }
    return visibleCount
}

@Preview
@Composable
private fun DockTabBar_Preview() = AzoraPreview {
    DockTabBar(
        panels = listOf(
            DockPanelDescriptor("tab1", "Explorer"),
            DockPanelDescriptor("tab2", "Editor"),
            DockPanelDescriptor("tab3", "Console")
        ),
        activeIndex = 1,
        onTabSelect = {},
        onTabClose = {},
        onTabDragStart = { _, _ -> }
    )
}