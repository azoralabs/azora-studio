package dev.azora.canvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Persisted link representing an execution-flow edge between two nodes.
 *
 * Exec links drive the order in which side-effecting nodes run; the interpreter follows them from
 * a source node's exec output to the next node's exec input. Pair with [AzoraDataLinkModel] for
 * value-carrying links.
 *
 * @property id Stable identifier; unique within the containing graph.
 * @property sourceNodeId Id of the node where execution flows from.
 * @property sourcePortIndex Index of the exec output on the source node.
 *   Most nodes expose a single exec output (index 0); branching nodes use multiple
 *   (e.g. `IF` uses 0 for the true branch and 1 for the false branch).
 * @property targetNodeId Id of the node execution flows into.
 * @property reroutePoints Optional waypoints used purely to shape the rendered curve;
 *   they have no effect on execution order.
 */
@Serializable
data class AzoraExecLinkModel(
    val id: String,
    val sourceNodeId: String,
    val sourcePortIndex: Int = 0,
    val targetNodeId: String,
    val reroutePoints: List<AzoraReroutePointModel> = emptyList(),
)