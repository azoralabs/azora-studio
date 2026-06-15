package dev.azora.canvas.presentation.state

import androidx.compose.ui.geometry.Offset
import dev.azora.canvas.domain.type.AzoraPortType

/**
 * Every user-driven mutation the canvas editor can produce.
 *
 * The canvas only emits actions; it never mutates [AzoraCanvasState] directly. A host wires these
 * into [AzoraCanvasStateHolder.onAction] to update the state and dispatch persistence callbacks
 * for graph changes (link creation/deletion, node moves, reroute-point edits).
 *
 * Grouped by concern:
 * - **Pan / viewport** — [UpdatePan], [EndPan].
 * - **Selection** — [SelectNode], [SelectLink], [SelectReroutePoint], [ClearSelection].
 * - **Link creation** — [StartCreatingLink], [FinishCreatingLink], [CancelCreatingLink].
 * - **Node drag** — [UpdateNodePosition], [EndNodeDrag].
 * - **Link context menu** — [ShowLinkContextMenu], [DismissLinkContextMenu], [AddReroutePoint], [DeleteLink].
 * - **Reroute-point edits** — [UpdateReroutePointPosition], [EndReroutePointDrag], [ShowReroutePointContextMenu], [DismissReroutePointContextMenu], [DeleteReroutePoint].
 * - **Canvas context menu** — [ShowCanvasContextMenu], [DismissCanvasContextMenu].
 * - **Cross-cutting** — [DismissAllContextMenus].
 */
sealed interface AzoraCanvasAction {

    /** Pan changed during a drag — emitted continuously until [EndPan]. [offset] is the cumulative pan. */
    data class UpdatePan(val offset: Offset) : AzoraCanvasAction
    /** Pan drag finished — host typically commits the final offset to undo state here. */
    data object EndPan : AzoraCanvasAction

    /** Select a node, or pass `null` to clear node selection without affecting other kinds. */
    data class SelectNode(val nodeId: String?) : AzoraCanvasAction
    /** Select a link, or pass `null` to clear link selection without affecting other kinds. */
    data class SelectLink(val linkId: String?) : AzoraCanvasAction
    /** Select a reroute point on a specific link. Mutually exclusive with node and link selection. */
    data class SelectReroutePoint(val linkId: String, val reroutePointId: String) : AzoraCanvasAction
    /** Clear every kind of selection. */
    data object ClearSelection : AzoraCanvasAction

    /**
     * User started dragging a new link out of an output port.
     * @param sourceNodeId Node owning the source port.
     * @param portType Type of the source port — drives the in-flight preview color.
     * @param outputPortIndex Which output port on the source node.
     */
    data class StartCreatingLink(
        val sourceNodeId: String,
        val portType: AzoraPortType,
        val outputPortIndex: Int = 0
    ) : AzoraCanvasAction
    /** User dropped the in-flight link onto [targetNodeId]; the host validates and persists the new link. */
    data class FinishCreatingLink(val targetNodeId: String) : AzoraCanvasAction
    /** User aborted the in-flight link drag (e.g. clicked empty canvas). */
    data object CancelCreatingLink : AzoraCanvasAction

    /** Node drag in progress; [position] is the new world-space position. Emitted continuously until [EndNodeDrag]. */
    data class UpdateNodePosition(val nodeId: String, val position: Offset) : AzoraCanvasAction
    /** Node drag finished — host typically commits to persistence/undo here. */
    data object EndNodeDrag : AzoraCanvasAction

    /**
     * Open the context menu for [linkId] at canvas-space [position].
     * @param segmentIndex Index of the link segment under the cursor; lets `AddReroutePoint` insert
     *   the new waypoint at the right position along the link.
     */
    data class ShowLinkContextMenu(val linkId: String, val position: Offset, val segmentIndex: Int) : AzoraCanvasAction
    /** Close the link context menu. */
    data object DismissLinkContextMenu : AzoraCanvasAction
    /** Insert a reroute point on [linkId] at [position], spliced into the link at [insertIndex]. */
    data class AddReroutePoint(val linkId: String, val position: Offset, val insertIndex: Int) : AzoraCanvasAction
    /** Delete [linkId]. The host is responsible for persistence. */
    data class DeleteLink(val linkId: String) : AzoraCanvasAction

    /** Reroute-point drag in progress; [delta] is the per-frame movement, not the absolute position. */
    data class UpdateReroutePointPosition(val linkId: String, val reroutePointId: String, val delta: Offset) : AzoraCanvasAction
    /** Reroute-point drag finished. */
    data object EndReroutePointDrag : AzoraCanvasAction
    /** Open the context menu for a reroute point at canvas-space [position]. */
    data class ShowReroutePointContextMenu(val linkId: String, val reroutePointId: String, val position: Offset) : AzoraCanvasAction
    /** Close the reroute-point context menu. */
    data object DismissReroutePointContextMenu : AzoraCanvasAction
    /** Delete a reroute point from [linkId]. */
    data class DeleteReroutePoint(val linkId: String, val reroutePointId: String) : AzoraCanvasAction

    /** Open the empty-area canvas context menu at screen-space [position]. */
    data class ShowCanvasContextMenu(val position: Offset) : AzoraCanvasAction
    /** Close the canvas context menu. */
    data object DismissCanvasContextMenu : AzoraCanvasAction

    /** Close every open context menu in one shot — used when the user starts a new interaction. */
    data object DismissAllContextMenus : AzoraCanvasAction
}
