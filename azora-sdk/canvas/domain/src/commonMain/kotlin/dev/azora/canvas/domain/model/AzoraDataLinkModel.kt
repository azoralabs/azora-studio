package dev.azora.canvas.domain.model

import kotlinx.serialization.Serializable

/**
 * Persisted link carrying a value from a data-output port to a data-input port.
 *
 * Data links are evaluated lazily by the interpreter: when a node needs an input it walks the link
 * back to its source and computes the producing node's output. Pair with [AzoraExecLinkModel] for
 * execution-flow links.
 *
 * @property id Stable identifier; unique within the containing graph.
 * @property sourceNodeId Id of the node providing the value.
 * @property sourcePortName Name of the output port on the source node.
 * @property targetNodeId Id of the node receiving the value.
 * @property targetPortName Name of the input port on the target node.
 * @property reroutePoints Optional waypoints used purely to shape the rendered curve;
 *   they have no effect on evaluation order.
 */
@Serializable
data class AzoraDataLinkModel(
    val id: String,
    val sourceNodeId: String,
    val sourcePortName: String,
    val targetNodeId: String,
    val targetPortName: String,
    val reroutePoints: List<AzoraReroutePointModel> = emptyList()
)