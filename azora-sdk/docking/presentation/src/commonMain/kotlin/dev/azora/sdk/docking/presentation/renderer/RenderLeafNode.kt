package dev.azora.sdk.docking.presentation.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.DockCallbacks
import dev.azora.sdk.docking.presentation.panel.DockPanel

/**
 * Renders a leaf node containing a single panel.
 *
 * Displays a [DockPanel] with the single panel's content. Unlike tab groups,
 * leaf nodes show a panel header instead of a tab bar since there's only
 * one panel.
 *
 * ## Drag Handling
 *
 * - If this panel is being dragged, it's marked as the drag source
 * - If dragging a different panel, a drop overlay is shown
 * - The drag position is passed down for zone hit detection
 *
 * @param node The leaf node to render
 * @param panelDescriptors Map of panel IDs to their descriptors
 * @param dragState Current drag operation state
 * @param callbacks Dock interaction callbacks
 *
 * @see DockPanel for the panel rendering
 * @see DockNode.Leaf for the data model
 */
@Composable
internal fun RenderLeafNode(
    node: DockNode.Leaf,
    panelDescriptors: Map<String, DockPanelDescriptor>,
    dragState: DragState?,
    callbacks: DockCallbacks
) {
    val panel = panelDescriptors[node.panelId]
    val draggingPanelId = dragState?.panelId

    val isDragging = dragState != null
    val isDragSource = draggingPanelId == node.panelId

    val dragPosition = if (isDragging && !isDragSource) {
        Offset(dragState.currentX, dragState.currentY)
    } else null

    panel?.let {
        DockPanel(
            panels = listOf(it),
            activeIndex = 0,
            isDragging = isDragging,
            isDragSource = isDragSource,
            dragPosition = dragPosition,
            onTabSelect = { },
            onTabClose = callbacks.onTabClose,
            onTabDragStart = callbacks.onTabDragStart,
            onTabDrag = callbacks.onTabDrag,
            onTabDragEnd = callbacks.onTabDragEnd,
            onTabDragCancel = callbacks.onTabDragCancel,
            onPanelClose = callbacks.onPanelClose,
            onPanelEnter = { callbacks.onPanelEnter(node.id) },
            onZoneHover = { zone -> callbacks.onZoneHover(node.id, zone) }
        )
    }
}