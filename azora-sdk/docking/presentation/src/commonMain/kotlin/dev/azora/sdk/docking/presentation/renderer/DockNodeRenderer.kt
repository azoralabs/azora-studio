package dev.azora.sdk.docking.presentation.renderer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.DockCallbacks

/**
 * Renders a dock node tree recursively.
 *
 * This is the main rendering entry point for the dock layout. It traverses
 * the [DockNode] tree and renders the appropriate UI for each node type:
 * splits, tab groups, and leaf panels.
 *
 * ## Node Types
 *
 * - **Split**: Renders two children with a draggable splitter between them
 * - **TabGroup**: Renders multiple panels as tabs with a tab bar
 * - **Leaf**: Renders a single panel with its header
 *
 * ## Drag State
 *
 * The renderer passes drag state down the tree to enable:
 * - Drop zone overlays on potential drop targets
 * - Visual feedback on the drag source panel
 * - Cursor position tracking for zone detection
 *
 * @param node The root node of the dock tree to render
 * @param panelDescriptors Map of panel IDs to their descriptors
 * @param dragState Current drag operation state, or null if not dragging
 * @param maximizedPanelId ID of the maximized panel, or null if none
 * @param callbacks Container for all dock interaction callbacks
 * @param modifier Modifier for the root container
 *
 * @see DockNode for the node tree structure
 * @see DockCallbacks for available callbacks
 */
@Composable
fun DockNodeRenderer(
    node: DockNode,
    panelDescriptors: Map<String, DockPanelDescriptor>,
    dragState: DragState?,
    maximizedPanelId: String?,
    callbacks: DockCallbacks,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        when (node) {
            is DockNode.Split -> {
                RenderSplitNode(
                    node = node,
                    panelDescriptors = panelDescriptors,
                    dragState = dragState,
                    maximizedPanelId = maximizedPanelId,
                    containerSize = containerSize,
                    callbacks = callbacks
                )
            }
            is DockNode.TabGroup -> {
                RenderTabGroupNode(
                    node = node,
                    panelDescriptors = panelDescriptors,
                    dragState = dragState,
                    callbacks = callbacks
                )
            }
            is DockNode.Leaf -> {
                RenderLeafNode(
                    node = node,
                    panelDescriptors = panelDescriptors,
                    dragState = dragState,
                    callbacks = callbacks
                )
            }
        }
    }
}

/**
 * Backwards-compatible overload with individual callbacks.
 *
 * Wraps individual callback parameters into a [DockCallbacks] instance
 * and delegates to the primary [DockNodeRenderer] overload.
 *
 * @see DockNodeRenderer for the primary overload with DockCallbacks
 * @see DockCallbacks for callback documentation
 */
@Composable
fun DockNodeRenderer(
    node: DockNode,
    panelDescriptors: Map<String, DockPanelDescriptor>,
    dragState: DragState?,
    maximizedPanelId: String?,
    onSplitResize: (nodeId: String, Float) -> Unit,
    onTabSelect: (nodeId: String, Int) -> Unit,
    onTabClose: (panelId: String) -> Unit,
    onTabReorder: (nodeId: String, fromIndex: Int, toIndex: Int) -> Unit,
    onTabDragStart: (panelId: String, Offset) -> Unit,
    onTabDrag: (panelId: String, Offset) -> Unit,
    onTabDragEnd: (panelId: String) -> Unit,
    onTabDragCancel: (panelId: String) -> Unit,
    onPanelClose: (panelId: String) -> Unit,
    onPanelFloat: (panelId: String) -> Unit,
    onPanelMaximize: (panelId: String) -> Unit,
    onPanelRestore: () -> Unit,
    onPanelEnter: (nodeId: String) -> Unit,
    onPanelExit: (nodeId: String) -> Unit,
    onZoneHover: (nodeId: String, DockZone?) -> Unit,
    modifier: Modifier = Modifier
) {
    DockNodeRenderer(
        node = node,
        panelDescriptors = panelDescriptors,
        dragState = dragState,
        maximizedPanelId = maximizedPanelId,
        callbacks = DockCallbacks(
            onSplitResize = onSplitResize,
            onTabSelect = onTabSelect,
            onTabClose = onTabClose,
            onTabReorder = onTabReorder,
            onTabDragStart = onTabDragStart,
            onTabDrag = onTabDrag,
            onTabDragEnd = onTabDragEnd,
            onTabDragCancel = onTabDragCancel,
            onPanelClose = onPanelClose,
            onPanelFloat = onPanelFloat,
            onPanelMaximize = onPanelMaximize,
            onPanelRestore = { _ -> onPanelRestore() },
            onPanelEnter = onPanelEnter,
            onPanelExit = onPanelExit,
            onZoneHover = onZoneHover
        ),
        modifier = modifier
    )
}