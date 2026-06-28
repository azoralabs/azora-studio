package dev.azora.canvas.presentation.type

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.type.AzoraPortType

/**
 * Renderer slot that lets the host project draw its own node bodies inside
 * [dev.azora.canvas.presentation.canvas.AzoraEditorCanvas].
 *
 * The canvas owns selection, link creation, dragging, port hit-testing and z-ordering; it doesn't
 * know what your nodes look like. This type alias is the seam: the canvas calls one of these for
 * each visible node, passing live interaction state and callbacks the host wires into its own
 * node composable (typically [dev.azora.canvas.presentation.node.AzoraNode]).
 *
 * Lambda parameters:
 * - `node` - the node model being rendered.
 * - `isSelected` - true when this node is the canvas's selected node.
 * - `isLinkSource` - true while the user is dragging a new link out of this node.
 * - `backLinkTransitionType` - port type of an incoming link, if any; useful for tinting the input edge.
 * - `linkTransitionType` - port type of the in-flight link being created; null when not link-creating.
 * - `panOffset` - current canvas pan, so the node renderer can position itself in screen space.
 * - `isInputConnected` - whether the node's exec input has at least one inbound link.
 * - `connectedOutputPortIndices` - the set of output port indices that already have outgoing links;
 *   the renderer uses this to draw connected vs. unconnected port states.
 * - `onSelect` - call when the user clicks to select this node.
 * - `onStartLink` - call when the user starts dragging a new link out of an output port.
 * - `onEndLink` - call when the user finishes a link drag on this node's input port.
 * - `onMove` - call with the drag delta while the node is being dragged.
 * - `onEndDrag` - call once dragging ends, e.g. to commit the move to persistence.
 * - `onDismissContextMenus` - call to close any open menus (e.g. when starting a node interaction).
 * - `onInputPortPositioned` - when non-null, report the input port's center in root coordinates so
 *   the canvas can draw links to it accurately.
 * - `onOutputPortPositioned` - same as above for output ports, keyed by port index.
 * - `onContextMenu` - call with the click's root-coordinate position when the user right-clicks the node
 *   body; the canvas opens a host-supplied node context menu there. Pass a no-op if unsupported.
 * - `onPortContextMenu` - same as [onContextMenu] but for a right-click on an output port, keyed by
 *   port index.
 */
typealias AzoraNodeContent = @Composable (
    node: AzoraNodeModel,
    isSelected: Boolean,
    isLinkSource: Boolean,
    backLinkTransitionType: AzoraPortType?,
    linkTransitionType: AzoraPortType?,
    panOffset: Offset,
    isInputConnected: Boolean,
    connectedOutputPortIndices: Set<Int>,
    onSelect: () -> Unit,
    onStartLink: (portType: AzoraPortType, outputPortIndex: Int) -> Unit,
    onEndLink: () -> Unit,
    onMove: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onDismissContextMenus: () -> Unit,
    onInputPortPositioned: ((Offset) -> Unit)?,
    onOutputPortPositioned: ((index: Int, position: Offset) -> Unit)?,
    onContextMenu: (rootPosition: Offset) -> Unit,
    onPortContextMenu: (portIndex: Int, rootPosition: Offset) -> Unit
) -> Unit
