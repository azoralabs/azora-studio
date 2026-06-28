package dev.azora.canvas.presentation.state

import androidx.compose.ui.geometry.Offset
import dev.azora.canvas.domain.type.AzoraPortType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reduces [AzoraCanvasAction]s into [AzoraCanvasState] updates and forwards persistence-relevant
 * events to host-supplied callbacks.
 *
 * Intentionally not a `ViewModel` so it can be composed inside a host ViewModel that owns the
 * graph itself. The split is:
 * - **This class owns:** transient editor state ([state]) - pan, selection, in-flight link, open menus.
 * - **The host owns:** the actual graph data (nodes, links, reroute points). Each callback fires
 *   when an action implies a graph mutation; the host validates, persists, and updates the model.
 *
 * Use [updateState] / [setPanOffset] to push state from outside (e.g. when restoring a saved view).
 *
 * @param initialState Starting state - defaults to an empty [AzoraCanvasState].
 * @param onLinkCreated Fires after a successful drop in link creation; host should validate and persist.
 * @param onLinkDeleted Fires when the user deletes a link via the link context menu.
 * @param onNodePositionChanged Fires per drag frame with the new world-space [Offset]; host updates the model.
 * @param onNodeDragEnded Fires once when a node drag ends - typically used to checkpoint undo.
 * @param onReroutePointAdded Fires when the user inserts a reroute point at a specific segment index.
 * @param onReroutePointDeleted Fires when the user deletes a reroute point via its context menu.
 * @param onReroutePointPositionChanged Fires per drag frame with a delta (not an absolute position).
 * @param onReroutePointDragEnded Fires once when a reroute-point drag ends - typically used to checkpoint undo.
 * @param onPanChanged Fires per pan frame with the cumulative pan offset.
 * @param onPanEnded Fires once when panning ends - typically used to persist the new viewport.
 */
class AzoraCanvasStateHolder(
    initialState: AzoraCanvasState = AzoraCanvasState(),
    private val onLinkCreated: (sourceId: String, targetId: String, portType: AzoraPortType, outputPortIndex: Int) -> Unit = { _, _, _, _ -> },
    private val onLinkDeleted: (linkId: String) -> Unit = {},
    private val onNodePositionChanged: (nodeId: String, position: Offset) -> Unit = { _, _ -> },
    private val onNodeDragEnded: () -> Unit = {},
    private val onReroutePointAdded: (linkId: String, position: Offset, insertIndex: Int) -> Unit = { _, _, _ -> },
    private val onReroutePointDeleted: (linkId: String, reroutePointId: String) -> Unit = { _, _ -> },
    private val onReroutePointPositionChanged: (linkId: String, reroutePointId: String, delta: Offset) -> Unit = { _, _, _ -> },
    private val onReroutePointDragEnded: () -> Unit = {},
    private val onPanChanged: (Offset) -> Unit = {},
    private val onPanEnded: () -> Unit = {}
) {
    private val _state = MutableStateFlow(initialState)
    /** Current state, observable as a [StateFlow]. Collect this from your composable. */
    val state: StateFlow<AzoraCanvasState> = _state.asStateFlow()

    /**
     * Single entry point for every canvas mutation. Dispatches [action] to the matching internal
     * handler, which both updates [state] and (where appropriate) invokes the persistence callback.
     */
    fun onAction(action: AzoraCanvasAction) {
        when (action) {
            // Pan
            is AzoraCanvasAction.UpdatePan -> updatePan(action.offset)
            is AzoraCanvasAction.EndPan -> endPan()

            // Selection
            is AzoraCanvasAction.SelectNode -> selectNode(action.nodeId)
            is AzoraCanvasAction.SelectLink -> selectLink(action.linkId)
            is AzoraCanvasAction.SelectReroutePoint -> selectReroutePoint(action.linkId, action.reroutePointId)
            is AzoraCanvasAction.ClearSelection -> clearSelection()

            // Link creation
            is AzoraCanvasAction.StartCreatingLink -> startCreatingLink(action.sourceNodeId, action.portType, action.outputPortIndex)
            is AzoraCanvasAction.FinishCreatingLink -> finishCreatingLink(action.targetNodeId)
            is AzoraCanvasAction.CancelCreatingLink -> cancelCreatingLink()

            // Node position
            is AzoraCanvasAction.UpdateNodePosition -> updateNodePosition(action.nodeId, action.position)
            is AzoraCanvasAction.EndNodeDrag -> endNodeDrag()

            // Link context menu
            is AzoraCanvasAction.ShowLinkContextMenu -> showLinkContextMenu(action.linkId, action.position, action.segmentIndex)
            is AzoraCanvasAction.DismissLinkContextMenu -> dismissLinkContextMenu()
            is AzoraCanvasAction.AddReroutePoint -> addReroutePoint(action.linkId, action.position, action.insertIndex)
            is AzoraCanvasAction.DeleteLink -> deleteLink(action.linkId)

            // Reroute point
            is AzoraCanvasAction.UpdateReroutePointPosition -> updateReroutePointPosition(action.linkId, action.reroutePointId, action.delta)
            is AzoraCanvasAction.EndReroutePointDrag -> endReroutePointDrag()
            is AzoraCanvasAction.ShowReroutePointContextMenu -> showReroutePointContextMenu(action.linkId, action.reroutePointId, action.position)
            is AzoraCanvasAction.DismissReroutePointContextMenu -> dismissReroutePointContextMenu()
            is AzoraCanvasAction.DeleteReroutePoint -> deleteReroutePoint(action.linkId, action.reroutePointId)

            // Canvas context menu
            is AzoraCanvasAction.ShowCanvasContextMenu -> showCanvasContextMenu(action.position)
            is AzoraCanvasAction.DismissCanvasContextMenu -> dismissCanvasContextMenu()
            is AzoraCanvasAction.ShowNodeContextMenu -> showNodeContextMenu(action.nodeId, action.position)
            is AzoraCanvasAction.DismissNodeContextMenu -> dismissNodeContextMenu()
            is AzoraCanvasAction.ShowPortContextMenu -> showPortContextMenu(action.nodeId, action.portIndex, action.position)
            is AzoraCanvasAction.DismissPortContextMenu -> dismissPortContextMenu()
            is AzoraCanvasAction.DismissAllContextMenus -> dismissAllContextMenus()
        }
    }

    // Pan
    private fun updatePan(offset: Offset) {
        _state.update { it.copy(panOffset = offset) }
        onPanChanged(offset)
    }

    private fun endPan() {
        onPanEnded()
    }

    // Selection
    private fun selectNode(nodeId: String?) {
        _state.update {
            it.copy(
                selectedNodeId = nodeId,
                selectedLinkId = null,
                selectedReroutePointId = null,
                selectedRerouteLinkId = null
            )
        }
    }

    private fun selectLink(linkId: String?) {
        _state.update {
            it.copy(
                selectedLinkId = linkId,
                selectedNodeId = null,
                selectedReroutePointId = null,
                selectedRerouteLinkId = null
            )
        }
    }

    private fun selectReroutePoint(linkId: String, reroutePointId: String) {
        _state.update {
            it.copy(
                selectedReroutePointId = reroutePointId,
                selectedRerouteLinkId = linkId,
                selectedNodeId = null,
                selectedLinkId = null
            )
        }
    }

    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedNodeId = null,
                selectedLinkId = null,
                selectedReroutePointId = null,
                selectedRerouteLinkId = null
            )
        }
    }

    // Link creation
    private fun startCreatingLink(sourceNodeId: String, portType: AzoraPortType, outputPortIndex: Int) {
        _state.update {
            it.copy(
                isCreatingLink = true,
                linkSourceNodeId = sourceNodeId,
                linkPortType = portType,
                linkOutputPortIndex = outputPortIndex
            )
        }
    }

    private fun finishCreatingLink(targetNodeId: String) {
        val sourceId = _state.value.linkSourceNodeId ?: return
        val portType = _state.value.linkPortType ?: return
        val outputPortIndex = _state.value.linkOutputPortIndex

        // Let the parent handle validation and actual link creation
        onLinkCreated(sourceId, targetNodeId, portType, outputPortIndex)

        _state.update {
            it.copy(
                isCreatingLink = false,
                linkSourceNodeId = null,
                linkPortType = null,
                linkOutputPortIndex = 0
            )
        }
    }

    private fun cancelCreatingLink() {
        _state.update {
            it.copy(
                isCreatingLink = false,
                linkSourceNodeId = null,
                linkPortType = null,
                linkOutputPortIndex = 0
            )
        }
    }

    // Node position
    private fun updateNodePosition(nodeId: String, position: Offset) {
        onNodePositionChanged(nodeId, position)
    }

    private fun endNodeDrag() {
        onNodeDragEnded()
    }

    // Link context menu
    private fun showLinkContextMenu(linkId: String, position: Offset, segmentIndex: Int) {
        _state.update {
            it.copy(
                contextMenuLinkId = linkId,
                contextMenuPosition = position,
                contextMenuSegmentIndex = segmentIndex
            )
        }
    }

    private fun dismissLinkContextMenu() {
        _state.update {
            it.copy(
                contextMenuLinkId = null,
                contextMenuPosition = null,
                contextMenuSegmentIndex = 0
            )
        }
    }

    private fun addReroutePoint(linkId: String, position: Offset, insertIndex: Int) {
        onReroutePointAdded(linkId, position, insertIndex)
        dismissLinkContextMenu()
    }

    private fun deleteLink(linkId: String) {
        onLinkDeleted(linkId)
        dismissLinkContextMenu()
        // Clear selection if this link was selected
        if (_state.value.selectedLinkId == linkId) {
            _state.update { it.copy(selectedLinkId = null) }
        }
    }

    // Reroute point
    private fun updateReroutePointPosition(linkId: String, reroutePointId: String, delta: Offset) {
        onReroutePointPositionChanged(linkId, reroutePointId, delta)
    }

    private fun endReroutePointDrag() {
        onReroutePointDragEnded()
    }

    private fun showReroutePointContextMenu(linkId: String, reroutePointId: String, position: Offset) {
        _state.update {
            it.copy(
                contextMenuRerouteLinkId = linkId,
                contextMenuReroutePointId = reroutePointId,
                contextMenuReroutePosition = position
            )
        }
    }

    private fun dismissReroutePointContextMenu() {
        _state.update {
            it.copy(
                contextMenuRerouteLinkId = null,
                contextMenuReroutePointId = null,
                contextMenuReroutePosition = null
            )
        }
    }

    private fun deleteReroutePoint(linkId: String, reroutePointId: String) {
        onReroutePointDeleted(linkId, reroutePointId)
        dismissReroutePointContextMenu()
    }

    // Canvas context menu
    private fun showCanvasContextMenu(position: Offset) {
        _state.update { it.copy(canvasContextMenuPosition = position) }
    }

    private fun dismissCanvasContextMenu() {
        _state.update { it.copy(canvasContextMenuPosition = null) }
    }

    // Node / port context menus open exclusively (close any other menu first), matching the canvas
    // and link menus which are always preceded by dismissAllContextMenus on right-click.
    private fun showNodeContextMenu(nodeId: String, position: Offset) {
        _state.update { dismissAllMenusExcept(it).copy(nodeContextMenuNodeId = nodeId, nodeContextMenuPosition = position) }
    }

    private fun dismissNodeContextMenu() {
        _state.update { it.copy(nodeContextMenuNodeId = null, nodeContextMenuPosition = null) }
    }

    private fun showPortContextMenu(nodeId: String, portIndex: Int, position: Offset) {
        _state.update { dismissAllMenusExcept(it).copy(portContextMenuNodeId = nodeId, portContextMenuPortIndex = portIndex, portContextMenuPosition = position) }
    }

    private fun dismissPortContextMenu() {
        _state.update { it.copy(portContextMenuNodeId = null, portContextMenuPosition = null) }
    }

    /** Clears every menu field on [state] in one go (helper for exclusive open + dismiss-all). */
    private fun dismissAllMenusExcept(state: AzoraCanvasState): AzoraCanvasState = state.copy(
        contextMenuLinkId = null,
        contextMenuPosition = null,
        contextMenuSegmentIndex = 0,
        contextMenuRerouteLinkId = null,
        contextMenuReroutePointId = null,
        contextMenuReroutePosition = null,
        canvasContextMenuPosition = null,
        nodeContextMenuNodeId = null,
        nodeContextMenuPosition = null,
        portContextMenuNodeId = null,
        portContextMenuPosition = null
    )

    private fun dismissAllContextMenus() {
        _state.update { dismissAllMenusExcept(it) }
    }

    /**
     * Apply [transform] to the current state. Use for cross-cutting external updates (e.g.
     * restoring a saved viewport, clearing transient menus during a programmatic operation).
     * Does **not** fire any persistence callback.
     */
    fun updateState(transform: (AzoraCanvasState) -> AzoraCanvasState) {
        _state.update(transform)
    }

    /**
     * Set the pan offset directly without going through an [AzoraCanvasAction.UpdatePan]. Useful
     * when restoring a persisted viewport on graph load. Does **not** fire [onPanChanged].
     */
    fun setPanOffset(offset: Offset) {
        _state.update { it.copy(panOffset = offset) }
    }
}
