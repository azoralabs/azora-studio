package dev.azora.canvas.domain.model

import kotlinx.serialization.Serializable

/**
 * A waypoint that a link's rendered curve passes through, allowing users to route links around
 * nodes for clarity. Reroute points are purely visual and never influence evaluation or
 * execution order.
 *
 * @property id Stable identifier; unique within the containing link.
 * @property x Canvas-space x coordinate in dp.
 * @property y Canvas-space y coordinate in dp.
 */
@Serializable
data class AzoraReroutePointModel(
    val id: String,
    val x: Float,
    val y: Float
)