package dev.azora.canvas.presentation.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.azora.canvas.domain.model.AzoraReroutePointModel
import dev.azora.canvas.domain.type.AzoraPortType

/**
 * Render-ready link snapshot consumed by [dev.azora.canvas.presentation.canvas.AzoraLinksLayer].
 *
 * The presentation layer never reads the persisted [dev.azora.canvas.domain.model.AzoraExecLinkModel]
 * directly — the host typically resolves source/target node positions, applies pan, and produces
 * one of these per visible link each frame. Keeping the rendered representation distinct from the
 * persisted one means that node moves, pan, and per-link color overrides don't have to round-trip
 * through the domain model.
 *
 * @property id Identifier copied from the underlying link; used to correlate with selection /
 *   hover state in [dev.azora.canvas.presentation.state.AzoraCanvasState].
 * @property sourceNodeId Id of the node owning the start of the link.
 * @property targetNodeId Id of the node owning the end of the link.
 * @property startPosition Screen-space start point with pan already applied.
 * @property endPosition Screen-space end point with pan already applied.
 * @property portType Drives the link color and shape unless [startColor]/[endColor] override it.
 * @property outputPortIndex Index of the source's exec output, mirroring
 *   [dev.azora.canvas.domain.model.AzoraExecLinkModel.sourcePortIndex]; used so multiple outputs
 *   on one node can be distinguished when computing port positions.
 * @property reroutePoints Optional waypoints in canvas (un-panned) space. The links layer
 *   re-applies pan when drawing them so the reroute coordinates can be persisted as-is.
 * @property startColor Optional override for the gradient start; falls back to the [portType] color.
 * @property endColor Optional override for the gradient end; falls back to the [portType] color.
 */
data class AzoraDrawableLink(
    val id: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val startPosition: Offset,
    val endPosition: Offset,
    val portType: AzoraPortType,
    val outputPortIndex: Int = 0,
    val reroutePoints: List<AzoraReroutePointModel> = emptyList(),
    val startColor: Color? = null,
    val endColor: Color? = null
)