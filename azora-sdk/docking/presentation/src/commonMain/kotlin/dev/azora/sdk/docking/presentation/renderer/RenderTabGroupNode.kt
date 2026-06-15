package dev.azora.sdk.docking.presentation.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.DockCallbacks
import dev.azora.sdk.docking.presentation.panel.DockPanel

/**
 * Renders a tab group node with multiple panels as tabs.
 *
 * Displays a [DockPanel] with a tab bar showing all panels in the group.
 * The active tab's content is shown, and users can switch between tabs,
 * close tabs, reorder tabs via drag, or drag tabs out to dock elsewhere.
 *
 * ## Drag Handling
 *
 * - If this group contains the dragged panel, it's marked as the drag source
 * - If dragging and not the source, a drop overlay is shown
 * - The drag position is passed down for zone hit detection
 *
 * @param node The tab group node to render
 * @param panelDescriptors Map of panel IDs to their descriptors
 * @param dragState Current drag operation state
 * @param callbacks Dock interaction callbacks
 *
 * @see DockPanel for the panel rendering
 * @see DockNode.TabGroup for the data model
 */
@Composable
internal fun RenderTabGroupNode(
    node: DockNode.TabGroup,
    panelDescriptors: Map<String, DockPanelDescriptor>,
    dragState: DragState?,
    callbacks: DockCallbacks
) {
    val panels = node.panels.mapNotNull { panelDescriptors[it] }
    val draggingPanelId = dragState?.panelId

    val isDragging = dragState != null
    val isDragSource = node.panels.contains(draggingPanelId)

    // Pass drag position when:
    // - Dragging and not the source (original behavior), OR
    // - Dragging from this group but other panels remain (allows splitting)
    val dragPosition = if (isDragging && (!isDragSource || panels.size > 1)) {
        Offset(dragState.currentX, dragState.currentY)
    } else null

    DockPanel(
        panels = panels,
        activeIndex = node.activeTabIndex,
        isDragging = isDragging,
        isDragSource = isDragSource,
        dragPosition = dragPosition,
        onTabSelect = { index -> callbacks.onTabSelect(node.id, index) },
        onTabClose = callbacks.onTabClose,
        onTabReorder = { fromIndex, toIndex ->
            callbacks.onTabReorder(
                node.id,
                fromIndex,
                toIndex
            )
        },
        onTabDragStart = callbacks.onTabDragStart,
        onTabDrag = callbacks.onTabDrag,
        onTabDragEnd = callbacks.onTabDragEnd,
        onTabDragCancel = callbacks.onTabDragCancel,
        onPanelClose = callbacks.onPanelClose,
        onPanelEnter = { callbacks.onPanelEnter(node.id) },
        onZoneHover = { zone -> callbacks.onZoneHover(node.id, zone) }
    )
}