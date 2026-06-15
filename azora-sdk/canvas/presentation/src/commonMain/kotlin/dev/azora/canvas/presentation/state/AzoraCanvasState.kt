package dev.azora.canvas.presentation.state

import androidx.compose.ui.geometry.Offset
import dev.azora.canvas.domain.type.AzoraPortType

/**
 * Immutable snapshot of all transient canvas-editor state — the value driven by
 * [AzoraCanvasStateHolder] and consumed by [dev.azora.canvas.presentation.canvas.AzoraEditorCanvas].
 *
 * Keep persisted graph data (nodes, links, reroute points) outside of this — that lives in the
 * domain layer and is owned by the host. Everything here is what the editor "remembers" between
 * frames: pan, current selection, in-flight link creation, and which context menu (if any) is open.
 *
 * Selection invariants enforced by [AzoraCanvasStateHolder]:
 * - At most one of [selectedNodeId], [selectedLinkId], or the reroute-point pair
 *   ([selectedReroutePointId] + [selectedRerouteLinkId]) is non-null at a time.
 *
 * Link-creation invariants:
 * - When [isCreatingLink] is true, [linkSourceNodeId] and [linkPortType] are non-null.
 *
 * Context-menu invariants:
 * - At most one of the three menu groups (link, reroute-point, canvas) is shown at a time;
 *   [hasOpenContextMenu] is the cheap "any menu open?" check.
 *
 * @property panOffset Current pan in pixels; applied to nodes, links and reroute points by the renderer.
 * @property selectedNodeId Selected node, if any.
 * @property selectedLinkId Selected link, if any.
 * @property selectedReroutePointId Selected reroute-point id (paired with [selectedRerouteLinkId]).
 * @property selectedRerouteLinkId Id of the link owning [selectedReroutePointId].
 * @property isCreatingLink True while the user is dragging a new link from a source port.
 * @property linkSourceNodeId Source node of the in-flight link, when [isCreatingLink] is true.
 * @property linkPortType Source port type of the in-flight link, when [isCreatingLink] is true.
 * @property linkOutputPortIndex Source output index of the in-flight link.
 * @property contextMenuLinkId Link whose context menu is open, if any.
 * @property contextMenuPosition Canvas-space position of the link context menu.
 * @property contextMenuSegmentIndex Index of the link segment under the cursor when the menu was opened
 *   (used by `AddReroutePoint` so the new waypoint inserts at the right spot).
 * @property contextMenuReroutePointId Reroute-point whose context menu is open, if any.
 * @property contextMenuRerouteLinkId Id of the link owning [contextMenuReroutePointId].
 * @property contextMenuReroutePosition Canvas-space position of the reroute-point context menu.
 * @property canvasContextMenuPosition Screen-space position of the canvas (empty area) context menu, if open.
 */
data class AzoraCanvasState(
    val panOffset: Offset = Offset.Zero,

    val selectedNodeId: String? = null,
    val selectedLinkId: String? = null,
    val selectedReroutePointId: String? = null,
    val selectedRerouteLinkId: String? = null,

    val isCreatingLink: Boolean = false,
    val linkSourceNodeId: String? = null,
    val linkPortType: AzoraPortType? = null,
    val linkOutputPortIndex: Int = 0,

    val contextMenuLinkId: String? = null,
    val contextMenuPosition: Offset? = null,
    val contextMenuSegmentIndex: Int = 0,

    val contextMenuReroutePointId: String? = null,
    val contextMenuRerouteLinkId: String? = null,
    val contextMenuReroutePosition: Offset? = null,

    val canvasContextMenuPosition: Offset? = null
) {
    /** True iff any context menu (link, reroute-point, or canvas) is currently open. */
    val hasOpenContextMenu: Boolean
        get() = contextMenuLinkId != null ||
                contextMenuReroutePointId != null ||
                canvasContextMenuPosition != null
}
