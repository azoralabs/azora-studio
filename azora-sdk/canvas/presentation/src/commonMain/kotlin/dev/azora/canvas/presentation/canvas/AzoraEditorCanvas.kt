package dev.azora.canvas.presentation.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.*
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.canvas.presentation.data.AzoraDrawableLink
import dev.azora.canvas.presentation.state.*
import dev.azora.canvas.presentation.type.AzoraNodeContent
import dev.azora.canvas.presentation.util.*
import dev.azora.sdk.core.theme.palette.AzoraPalette
import kotlin.math.roundToInt

/**
 * Full node-graph editor - the top-level composable a host typically embeds.
 *
 * Composes [AzoraCanvas] (grid + pan + clicks) with three [AzoraLinksLayer]s, an
 * [AzoraLinkCreationPreview], every node via [AzoraNodeContent], reroute points, and three kinds
 * of context menu (link / reroute / canvas). Owns local hover state for links so it can hand it
 * to all three link layers.
 *
 * Z-ordering strategy (so connected links read clearly when something is selected):
 * - **Layer 0** - non-connected, non-selected links (back-most).
 * - **Layer 1 (z=1)** - links connected to a selected node, in front of other links but behind
 *   the nodes themselves so the highlight reads as "behind the nodes".
 * - **Layer 1.5 (z=1.5)** - link creation preview.
 * - **Layer 2.5 (z=2.5)** - selected link, in front of nodes that don't touch it.
 * - Nodes get z 2..5 based on whether they are link source, selected, or attached to the
 *   selected link.
 * - Reroute points inherit their owning link's layer; a *selected* reroute jumps to z=5 so it
 *   stays grabbable above all nodes.
 * - Context menus pin to z=100.
 *
 * Click semantics (forwarded from the underlying [AzoraCanvas]):
 * - Left-click: dismisses any open menu, otherwise cancels link creation if active, otherwise
 *   selects a hovered link (if [isLinkSelectable] allows) or clears selection.
 * - Right-click: dismisses menus, then opens the link context menu if hovering a link, else the
 *   canvas context menu.
 *
 * @param canvasState Authoritative editor state.
 * @param onCanvasAction Sink for [AzoraCanvasAction]s emitted by the editor.
 * @param links Pre-computed drawable links for this frame.
 * @param nodes Nodes to render.
 * @param linkCreationStart Screen-space start of the in-flight link, when one is being created;
 *   the host computes this from the source port's reported position.
 * @param linkCreationStartColor Optional gradient-start override for the preview.
 * @param linkCreationEndColor Optional gradient-end override for the preview.
 * @param isLinkSelectable Filter to gate which links can be selected by left-click.
 * @param nodeContent Host-provided node renderer; see [AzoraNodeContent].
 * @param onInputPortPositioned Forwarded port position callback (root coordinates).
 * @param onOutputPortPositioned Forwarded port position callback (root coordinates).
 * @param onCanvasPositioned Reports the editor's own position in the root window - useful when
 *   the host needs to map screen-space to canvas-space outside this composable.
 * @param extraContent Slot for custom on-canvas content rendered between the back link layer and
 *   the nodes (e.g. fixed root nodes, branch headers).
 * @param canvasContextMenuContent Slot for the canvas (empty-area) context menu.
 * @param modifier Modifier for the outer canvas box.
 */
@Composable
fun AzoraEditorCanvas(
    canvasState: AzoraCanvasState,
    onCanvasAction: (AzoraCanvasAction) -> Unit,
    links: List<AzoraDrawableLink>,
    nodes: List<AzoraNodeModel>,
    linkCreationStart: Offset? = null,
    linkCreationStartColor: Color? = null,
    linkCreationEndColor: Color? = null,
    isLinkSelectable: (linkId: String) -> Boolean = { true },
    nodeContent: AzoraNodeContent,
    // Port position callbacks - called when ports report their positions (in root coordinates)
    onInputPortPositioned: ((nodeId: String, position: Offset) -> Unit)? = null,
    onOutputPortPositioned: ((nodeId: String, index: Int, position: Offset) -> Unit)? = null,
    // Canvas position callback - called with the canvas container's position in root coordinates
    onCanvasPositioned: ((Offset) -> Unit)? = null,
    extraContent: @Composable BoxScope.(
        panOffset: Offset,
        onDismissAllContextMenus: () -> Unit
    ) -> Unit = { _, _ -> },
    canvasContextMenuContent: @Composable (position: Offset, onDismiss: () -> Unit) -> Unit = { _, _ -> },
    // Host-supplied node/port right-click menus. The canvas anchors them at the click (in canvas-local
    // coordinates) and owns open/dismiss state; the host only supplies the menu body.
    nodeContextMenuContent: @Composable (nodeId: String, position: Offset, onDismiss: () -> Unit) -> Unit = { _, _, _ -> },
    portContextMenuContent: @Composable (nodeId: String, portIndex: Int, position: Offset, onDismiss: () -> Unit) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // Local hover state (not persisted)
    var hoveredLinkId by remember { mutableStateOf<String?>(null) }
    var hoveredSegmentIndex by remember { mutableStateOf(0) }
    // The canvas's own position in root coordinates — used to convert node/port menu anchors (reported
    // in root coords by AzoraNode/AzoraOutputPort) back into this overlay's canvas-local space.
    var canvasPositionInRoot by remember { mutableStateOf(Offset.Zero) }

    val dismissAllContextMenus = { onCanvasAction(AzoraCanvasAction.DismissAllContextMenus) }

    val currentOnCanvasPositioned by rememberUpdatedState(onCanvasPositioned)

    AzoraCanvas(
        panOffset = canvasState.panOffset,
        onPanChange = { onCanvasAction(AzoraCanvasAction.UpdatePan(it)) },
        onEndPan = { onCanvasAction(AzoraCanvasAction.EndPan) },
        onLeftClick = {
            // Dismiss any open context menus first
            if (canvasState.hasOpenContextMenu) {
                dismissAllContextMenus()
                return@AzoraCanvas
            }

            if (canvasState.isCreatingLink) {
                onCanvasAction(AzoraCanvasAction.CancelCreatingLink)
            } else {
                // Select link if hovering over one (if selectable), otherwise clear selection
                val linkId = hoveredLinkId
                if (linkId != null && isLinkSelectable(linkId)) {
                    onCanvasAction(AzoraCanvasAction.SelectLink(linkId))
                } else {
                    onCanvasAction(AzoraCanvasAction.ClearSelection)
                }
            }
        },
        onRightClick = { clickPosition ->
            // Dismiss context menus if open
            dismissAllContextMenus()

            // Check if clicking on a hovered link - show context menu
            val linkId = hoveredLinkId
            if (linkId != null) {
                val worldPosition = Offset(
                    clickPosition.x - canvasState.panOffset.x,
                    clickPosition.y - canvasState.panOffset.y
                )
                onCanvasAction(AzoraCanvasAction.ShowLinkContextMenu(linkId, worldPosition, hoveredSegmentIndex))
            } else {
                // Show canvas context menu
                onCanvasAction(AzoraCanvasAction.ShowCanvasContextMenu(clickPosition))
            }
        },
        modifier = modifier.onGloballyPositioned { coordinates ->
            val pos = coordinates.positionInRoot()
            canvasPositionInRoot = pos
            currentOnCanvasPositioned?.invoke(pos)
        }
    ) { mousePosition ->
        val selectedNodeId = canvasState.selectedNodeId
        val selectedLinkId = canvasState.selectedLinkId
        val selectedRerouteLinkId = canvasState.selectedRerouteLinkId

        // Nodes connected to the selected node
        val nodesConnectedToSelectedNode = if (selectedNodeId != null) {
            links.filter { it.sourceNodeId == selectedNodeId || it.targetNodeId == selectedNodeId }
                .flatMap { listOf(it.sourceNodeId, it.targetNodeId) }
                .filter { it != selectedNodeId }
                .toSet()
        } else emptySet()

        // Nodes connected to the selected link (source and target)
        val selectedLink = links.find { it.id == selectedLinkId }
        val nodesConnectedToSelectedLink = if (selectedLink != null) {
            setOf(selectedLink.sourceNodeId, selectedLink.targetNodeId)
        } else emptySet()

        // Check if a link is connected to the selected node
        val isLinkConnectedToSelectedNode: (AzoraDrawableLink) -> Boolean = { link ->
            selectedNodeId != null && (link.sourceNodeId == selectedNodeId || link.targetNodeId == selectedNodeId)
        }

        // Z-ordering strategy:
        // When NODE selected: connected links behind all nodes but in front of other links
        // When LINK selected: selected link behind its connection nodes but in front of other links and nodes

        // Layer 0: Non-connected/non-selected links (always at back)
        AzoraLinksLayer(
            links = links,
            panOffset = canvasState.panOffset,
            mousePosition = mousePosition,
            selectedLinkId = canvasState.selectedLinkId,
            hoveredLinkId = hoveredLinkId,
            onLinkHovered = { linkId, segmentIndex ->
                if (linkId != hoveredLinkId || segmentIndex != hoveredSegmentIndex) {
                    hoveredLinkId = linkId
                    hoveredSegmentIndex = segmentIndex
                }
            },
            linkFilter = { link ->
                // Exclude connected links (when node selected) and selected link (when link selected)
                !isLinkConnectedToSelectedNode(link) && link.id != selectedLinkId && link.id != selectedRerouteLinkId
            },
            isCreatingLink = false,
            linkCreationStart = null,
            linkCreationPortType = null
        )

        // Extra content (root node, control flow nodes, etc.)
        extraContent(canvasState.panOffset, dismissAllContextMenus)

        // Layer 1: Connected links (when node selected) - in front of other links, behind all nodes
        if (selectedNodeId != null) {
            Box(modifier = Modifier.zIndex(1f)) {
                AzoraLinksLayer(
                    links = links,
                    panOffset = canvasState.panOffset,
                    mousePosition = mousePosition,
                    selectedLinkId = canvasState.selectedLinkId,
                    hoveredLinkId = hoveredLinkId,
                    onLinkHovered = { linkId, segmentIndex ->
                        if (linkId != hoveredLinkId || segmentIndex != hoveredSegmentIndex) {
                            hoveredLinkId = linkId
                            hoveredSegmentIndex = segmentIndex
                        }
                    },
                    linkFilter = { link -> isLinkConnectedToSelectedNode(link) },
                    isCreatingLink = false,
                    linkCreationStart = null,
                    linkCreationPortType = null
                )
            }
        }

        // Layer 2: Selected link (when link selected) - in front of other links and other nodes
        if (selectedLinkId != null || selectedRerouteLinkId != null) {
            val linkIdToShow = selectedLinkId ?: selectedRerouteLinkId
            Box(modifier = Modifier.zIndex(2.5f)) {
                AzoraLinksLayer(
                    links = links,
                    panOffset = canvasState.panOffset,
                    mousePosition = mousePosition,
                    selectedLinkId = canvasState.selectedLinkId,
                    hoveredLinkId = hoveredLinkId,
                    onLinkHovered = { linkId, segmentIndex ->
                        if (linkId != hoveredLinkId || segmentIndex != hoveredSegmentIndex) {
                            hoveredLinkId = linkId
                            hoveredSegmentIndex = segmentIndex
                        }
                    },
                    linkFilter = { link -> link.id == linkIdToShow },
                    isCreatingLink = false,
                    linkCreationStart = null,
                    linkCreationPortType = null
                )
            }
        }

        // Link creation preview
        if (canvasState.isCreatingLink && linkCreationStart != null && canvasState.linkPortType != null) {
            Box(modifier = Modifier.zIndex(1.5f)) {
                AzoraLinkCreationPreview(
                    startPosition = linkCreationStart,
                    mousePosition = mousePosition,
                    portType = canvasState.linkPortType,
                    startColor = linkCreationStartColor,
                    endColor = linkCreationEndColor
                )
            }
        }

        // Nodes with z-ordering based on selection state
        val linkSourceNodeId = canvasState.linkSourceNodeId
        nodes.forEach { node ->
            val nodeZIndex = when {
                // Link source node always on top
                node.id == linkSourceNodeId -> 5f
                // When link selected: connection nodes on top of selected link
                selectedLinkId != null && node.id in nodesConnectedToSelectedLink -> 3f
                // Selected node on top
                node.id == selectedNodeId -> 4f
                // When link selected: other nodes behind selected link
                selectedLinkId != null && node.id !in nodesConnectedToSelectedLink -> 2f
                // Regular nodes
                else -> 2f
            }

            key(node.id) {
                Box(modifier = Modifier.zIndex(nodeZIndex)) {
                    NodeRenderer(
                        node = node,
                        links = links,
                        selectedNodeId = selectedNodeId,
                        canvasState = canvasState,
                        onCanvasAction = onCanvasAction,
                        dismissAllContextMenus = dismissAllContextMenus,
                        nodeContent = nodeContent,
                        onInputPortPositioned = onInputPortPositioned,
                        onOutputPortPositioned = onOutputPortPositioned
                    )
                }
            }
        }

        // Reroute points with z-index based on their link's state
        links.forEach { link ->
            val isLinkSelected = canvasState.selectedLinkId == link.id
            val isHovered = hoveredLinkId == link.id
            val hasSelectedReroute = canvasState.selectedRerouteLinkId == link.id
            val baseColor = link.startColor ?: link.portType.toColor()
            val color = when {
                isLinkSelected || hasSelectedReroute -> baseColor.brighten(0.2f)
                isHovered -> baseColor.brighten(0.3f)
                else -> baseColor
            }

            // Reroute z-index matches their link's layer
            val baseRerouteZIndex = when {
                link.id == selectedLinkId || link.id == selectedRerouteLinkId -> 2.6f
                isLinkConnectedToSelectedNode(link) -> 1.1f
                else -> 0.5f
            }

            link.reroutePoints.forEach { reroute ->
                key("${link.id}-${reroute.id}") {
                    val isRerouteSelected = canvasState.selectedReroutePointId == reroute.id &&
                            canvasState.selectedRerouteLinkId == link.id
                    val isContextMenuOpen = canvasState.contextMenuReroutePointId == reroute.id
                    // Selected reroute point gets highest zIndex (above nodes)
                    val rerouteZIndex = if (isRerouteSelected) 5f else baseRerouteZIndex
                    Box(modifier = Modifier.zIndex(rerouteZIndex)) {
                        AzoraReroutePoint(
                            reroutePoint = reroute,
                            color = color,
                            isSelected = isRerouteSelected,
                            panOffset = canvasState.panOffset,
                            isContextMenuOpen = isContextMenuOpen,
                            onSelect = {
                                onCanvasAction(AzoraCanvasAction.SelectReroutePoint(link.id, reroute.id))
                            },
                            onMove = { delta ->
                                onCanvasAction(AzoraCanvasAction.UpdateReroutePointPosition(link.id, reroute.id, delta))
                            },
                            onEndDrag = { onCanvasAction(AzoraCanvasAction.EndReroutePointDrag) },
                            onRightClick = { position ->
                                onCanvasAction(AzoraCanvasAction.ShowReroutePointContextMenu(link.id, reroute.id, position))
                            },
                            onDismissContextMenus = dismissAllContextMenus
                        )
                    }
                }
            }
        }

        // Link context menu (always on top)
        if (canvasState.contextMenuLinkId != null && canvasState.contextMenuPosition != null) {
            Box(modifier = Modifier.zIndex(100f)) {
                val menuScreenPosition = Offset(
                    canvasState.contextMenuPosition.x + canvasState.panOffset.x,
                    canvasState.contextMenuPosition.y + canvasState.panOffset.y
                )
                AzoraLinkContextMenu(
                    position = menuScreenPosition,
                    onAddRerouteNode = {
                        onCanvasAction(
                            AzoraCanvasAction.AddReroutePoint(
                                canvasState.contextMenuLinkId,
                                canvasState.contextMenuPosition,
                                canvasState.contextMenuSegmentIndex
                            )
                        )
                    },
                    onDeleteLink = {
                        onCanvasAction(AzoraCanvasAction.DeleteLink(canvasState.contextMenuLinkId))
                    }
                )
            }
        }

        // Reroute point context menu (always on top)
        if (canvasState.contextMenuRerouteLinkId != null &&
            canvasState.contextMenuReroutePointId != null &&
            canvasState.contextMenuReroutePosition != null
        ) {
            Box(modifier = Modifier.zIndex(100f)) {
                val rerouteMenuScreenPosition = Offset(
                    canvasState.contextMenuReroutePosition.x + canvasState.panOffset.x,
                    canvasState.contextMenuReroutePosition.y + canvasState.panOffset.y
                )
                AzoraReroutePointContextMenu(
                    position = rerouteMenuScreenPosition,
                    onDeleteReroutePoint = {
                        onCanvasAction(
                            AzoraCanvasAction.DeleteReroutePoint(
                                canvasState.contextMenuRerouteLinkId,
                                canvasState.contextMenuReroutePointId
                            )
                        )
                    }
                )
            }
        }

        // Canvas context menu (always on top, provided by caller)
        if (canvasState.canvasContextMenuPosition != null) {
            Box(modifier = Modifier.zIndex(100f)) {
                canvasContextMenuContent(
                    canvasState.canvasContextMenuPosition,
                    { onCanvasAction(AzoraCanvasAction.DismissCanvasContextMenu) }
                )
            }
        }

        // Node-body context menu (host-supplied). Anchor comes in root coords; convert to canvas-local.
        if (canvasState.nodeContextMenuNodeId != null && canvasState.nodeContextMenuPosition != null) {
            val pos = canvasState.nodeContextMenuPosition
            val nodeId = canvasState.nodeContextMenuNodeId
            Box(modifier = Modifier.zIndex(100f)) {
                val local = Offset(pos.x - canvasPositionInRoot.x, pos.y - canvasPositionInRoot.y)
                nodeContextMenuContent(
                    nodeId,
                    local,
                    { onCanvasAction(AzoraCanvasAction.DismissNodeContextMenu) }
                )
            }
        }

        // Output-port context menu (host-supplied).
        if (canvasState.portContextMenuNodeId != null && canvasState.portContextMenuPosition != null) {
            val pos = canvasState.portContextMenuPosition
            val nodeId = canvasState.portContextMenuNodeId
            Box(modifier = Modifier.zIndex(100f)) {
                val local = Offset(pos.x - canvasPositionInRoot.x, pos.y - canvasPositionInRoot.y)
                portContextMenuContent(
                    nodeId,
                    canvasState.portContextMenuPortIndex,
                    local,
                    { onCanvasAction(AzoraCanvasAction.DismissPortContextMenu) }
                )
            }
        }
    }
}

/**
 * Built-in context menu for a link: "Add Reroute Node" and "Delete Link".
 *
 * @param position Screen-space anchor for the menu's top-left corner.
 * @param onAddRerouteNode Fires when the user picks "Add Reroute Node".
 * @param onDeleteLink Fires when the user picks "Delete Link".
 */
@Composable
internal fun AzoraLinkContextMenu(
    position: Offset,
    onAddRerouteNode: () -> Unit,
    onDeleteLink: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .width(IntrinsicSize.Max)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(AzoraPalette.Neutral80)
            .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddRerouteNode() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Add Reroute Node",
                    color = AzoraPalette.Neutral10,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AzoraPalette.Neutral60)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeleteLink() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Delete Link",
                    color = AzoraPalette.AccentRed,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Built-in context menu for a reroute point: a single "Delete Reroute Node" item.
 *
 * @param position Screen-space anchor for the menu's top-left corner.
 * @param onDeleteReroutePoint Fires when the user confirms deletion.
 */
@Composable
internal fun AzoraReroutePointContextMenu(
    position: Offset,
    onDeleteReroutePoint: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .width(IntrinsicSize.Max)
            .shadow(8.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(AzoraPalette.Neutral80)
            .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeleteReroutePoint() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Delete Reroute Node",
                    color = AzoraPalette.AccentRed,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Adapter that bridges [AzoraEditorCanvas]'s per-node state into the [AzoraNodeContent] shape.
 *
 * Computes the input/output connection state for [node] from the link list, then invokes the
 * host-supplied renderer with the appropriate callbacks bound to [AzoraCanvasAction]s. Kept private
 * so the adapter logic can evolve without forcing hosts to reimplement it.
 */
@Composable
private fun NodeRenderer(
    node: AzoraNodeModel,
    links: List<AzoraDrawableLink>,
    selectedNodeId: String?,
    canvasState: AzoraCanvasState,
    onCanvasAction: (AzoraCanvasAction) -> Unit,
    dismissAllContextMenus: () -> Unit,
    nodeContent: AzoraNodeContent,
    onInputPortPositioned: ((nodeId: String, position: Offset) -> Unit)?,
    onOutputPortPositioned: ((nodeId: String, index: Int, position: Offset) -> Unit)?
) {
    val inputLink = links.find { it.targetNodeId == node.id }
    val isInputConnected = inputLink != null
    val backLinkTransitionType = inputLink?.portType
    val connectedOutputPortIndices = links
        .filter { it.sourceNodeId == node.id }
        .map { it.outputPortIndex }
        .toSet()

    nodeContent(
        node,
        selectedNodeId == node.id,
        canvasState.linkSourceNodeId == node.id,
        backLinkTransitionType,
        canvasState.linkPortType,
        canvasState.panOffset,
        isInputConnected,
        connectedOutputPortIndices,
        { onCanvasAction(AzoraCanvasAction.SelectNode(node.id)) },
        { portType, outputPortIndex -> onCanvasAction(AzoraCanvasAction.StartCreatingLink(node.id, portType, outputPortIndex)) },
        { onCanvasAction(AzoraCanvasAction.FinishCreatingLink(node.id)) },
        { delta ->
            onCanvasAction(
                AzoraCanvasAction.UpdateNodePosition(
                    node.id,
                    Offset(node.positionX + delta.x, node.positionY + delta.y)
                )
            )
        },
        { onCanvasAction(AzoraCanvasAction.EndNodeDrag) },
        dismissAllContextMenus,
        onInputPortPositioned?.let { callback -> { position: Offset -> callback(node.id, position) } },
        onOutputPortPositioned?.let { callback -> { index: Int, position: Offset -> callback(node.id, index, position) } },
        // Right-click on the node body / an output port → open the host-supplied menu (root coords;
        // the canvas overlay converts to canvas-local). The host opts in via the menu-content slots.
        { rootPosition -> onCanvasAction(AzoraCanvasAction.ShowNodeContextMenu(node.id, rootPosition)) },
        { portIndex, rootPosition -> onCanvasAction(AzoraCanvasAction.ShowPortContextMenu(node.id, portIndex, rootPosition)) }
    )
}
