package dev.azora.canvas.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.canvas.presentation.data.AzoraDrawableLink
import dev.azora.canvas.presentation.util.*

/**
 * Renders a set of links as cubic Bézier curves on a single canvas, plus an optional
 * link-creation preview.
 *
 * Architecture notes:
 * - **Multiple instances per editor.** [AzoraEditorCanvas] renders this layer up to three times
 *   with different [linkFilter]s and z-indices to stack non-connected, connected, and selected
 *   links cleanly. To avoid one layer's hover-clear racing another's hover-set, the layer only
 *   clears the global hovered link if the current hover belongs to *its* filtered set.
 * - **Bezier shape.** Each segment between consecutive points (start → reroutes → end) is drawn
 *   as a cubic Bézier with horizontal control handles whose distance scales with the x-gap (min
 *   50 px / 2). This produces the familiar Blueprint-style horizontal snake even when reroutes
 *   are present.
 * - **Selection emphasis.** Selected links get three stacked white glow strokes underneath the
 *   colored stroke and a glow halo around the end circle.
 *
 * The link creation preview is built into this layer for convenience, but [AzoraLinkCreationPreview]
 * also exists as a standalone composable when you need it on its own z-layer.
 *
 * @param links Links to draw with positions already in screen space (pan applied).
 * @param panOffset Current pan; used **only** to position [AzoraReroutePointModel] waypoints,
 *   which are stored in canvas space.
 * @param mousePosition Current pointer position for hover detection.
 * @param selectedLinkId Currently selected link, if any.
 * @param hoveredLinkId Currently hovered link, if any — passed in so multiple layers can share it.
 * @param onLinkHovered Reports hover transitions: `(linkId, segmentIndex)` or `(null, 0)` to clear.
 * @param linkFilter Optional predicate to restrict which links this instance draws.
 * @param isCreatingLink Whether the in-flight link preview should be drawn.
 * @param linkCreationStart Start point of the in-flight link in screen space.
 * @param linkCreationPortType Port type of the in-flight link, drives the preview color.
 * @param linkCreationStartColor Optional override for the gradient start of the preview.
 * @param linkCreationEndColor Optional override for the gradient end of the preview.
 */
@Composable
fun AzoraLinksLayer(
    links: List<AzoraDrawableLink>,
    panOffset: Offset,
    mousePosition: Offset,
    selectedLinkId: String?,
    hoveredLinkId: String?,
    onLinkHovered: (linkId: String?, segmentIndex: Int) -> Unit,
    linkFilter: ((AzoraDrawableLink) -> Boolean)? = null,
    isCreatingLink: Boolean = false,
    linkCreationStart: Offset? = null,
    linkCreationPortType: AzoraPortType? = null,
    linkCreationStartColor: Color? = null,
    linkCreationEndColor: Color? = null
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val hoverThreshold = 35f
        var newHoveredLinkId: String? = null
        var newHoveredSegmentIndex = 0

        // Filter links if filter is provided
        val filteredLinks = if (linkFilter != null) links.filter(linkFilter) else links

        // Draw links
        filteredLinks.forEach { link ->
            val isSelected = selectedLinkId == link.id
            val isHovered = hoveredLinkId == link.id

            // Determine colors
            val baseColor = link.portType.toColor()
            val startColor = link.startColor ?: baseColor
            val endColor = link.endColor ?: baseColor

            val adjustedStartColor = when {
                isSelected -> startColor.brighten(0.2f)
                isHovered -> startColor.brighten(0.3f)
                else -> startColor
            }
            val adjustedEndColor = when {
                isSelected -> endColor.brighten(0.2f)
                isHovered -> endColor.brighten(0.3f)
                else -> endColor
            }

            val strokeWidth = when {
                isSelected -> 6f
                isHovered -> 6f
                else -> 4f
            }

            // Build list of points including reroute points
            val allPoints = mutableListOf<Offset>()
            allPoints.add(link.startPosition)
            link.reroutePoints.forEach { reroute ->
                allPoints.add(Offset(reroute.x + panOffset.x, reroute.y + panOffset.y))
            }
            allPoints.add(link.endPosition)

            // Draw bezier segments between consecutive points
            val path = Path()
            for (i in 0 until allPoints.size - 1) {
                val p0 = allPoints[i]
                val p1 = allPoints[i + 1]

                val controlPointOffset = (p1.x - p0.x).coerceAtLeast(50f) / 2
                val cp1x = p0.x + controlPointOffset
                val cp1y = p0.y
                val cp2x = p1.x - controlPointOffset
                val cp2y = p1.y

                // Check if mouse is near this segment
                if (isPointNearBezier(
                        mousePosition,
                        p0,
                        Offset(cp1x, cp1y),
                        Offset(cp2x, cp2y),
                        p1,
                        hoverThreshold
                    )
                ) {
                    newHoveredLinkId = link.id
                    newHoveredSegmentIndex = i
                }

                if (i == 0) {
                    path.moveTo(p0.x, p0.y)
                }
                path.cubicTo(cp1x, cp1y, cp2x, cp2y, p1.x, p1.y)
            }

            // Draw glow effect when selected
            if (isSelected) {
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.1f),
                    style = Stroke(width = 32f)
                )
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.2f),
                    style = Stroke(width = 20f)
                )
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.4f),
                    style = Stroke(width = 12f)
                )
            }

            // Draw main path with gradient
            val pathBrush = Brush.linearGradient(
                colors = listOf(adjustedStartColor, adjustedEndColor),
                start = link.startPosition,
                end = link.endPosition
            )
            drawPath(
                path = path,
                brush = pathBrush,
                style = Stroke(width = strokeWidth)
            )

            // Draw glow for end circle when selected
            if (isSelected) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 16f,
                    center = link.endPosition
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 12f,
                    center = link.endPosition
                )
            }

            // Draw arrow at end
            drawCircle(
                color = adjustedEndColor,
                radius = if (isHovered || isSelected) 8f else 6f,
                center = link.endPosition
            )
        }

        // Update hovered link state
        // Only clear hover if the currently hovered link is in THIS layer's filtered links
        // This prevents one layer from clearing hover state set by another layer
        val currentHoverIsInThisLayer = hoveredLinkId != null && filteredLinks.any { it.id == hoveredLinkId }

        if (newHoveredLinkId != null) {
            // Found a hovered link in this layer - update state
            if (newHoveredLinkId != hoveredLinkId || newHoveredSegmentIndex != 0) {
                onLinkHovered(newHoveredLinkId, newHoveredSegmentIndex)
            }
        } else if (currentHoverIsInThisLayer) {
            // No hover found, but the current hover was from this layer - clear it
            onLinkHovered(null, 0)
        }

        // Draw link creation preview
        if (isCreatingLink && linkCreationStart != null && linkCreationPortType != null) {
            val startX = linkCreationStart.x
            val startY = linkCreationStart.y
            val endX = mousePosition.x
            val endY = mousePosition.y

            val controlPointOffset = (endX - startX).coerceAtLeast(50f) / 2
            val previewPath = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX + controlPointOffset, startY,
                    endX - controlPointOffset, endY,
                    endX, endY
                )
            }

            val previewStartColor = linkCreationStartColor ?: linkCreationPortType.toColor()
            val previewEndColor = linkCreationEndColor ?: linkCreationPortType.toColor()

            val previewBrush = Brush.linearGradient(
                colors = listOf(previewStartColor, previewEndColor),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            )

            drawPath(
                path = previewPath,
                brush = previewBrush,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                )
            )

            // Draw circle at mouse position
            drawCircle(
                color = previewEndColor,
                radius = 6f,
                center = Offset(endX, endY)
            )
        }
    }
}

/**
 * Standalone in-flight link preview, broken out from [AzoraLinksLayer] so it can be hosted on its
 * own z-layer (typically between regular links and selected links).
 *
 * Renders a dashed cubic Bézier with a small disc at the pointer end. The curve uses the same
 * horizontal-handle convention as committed links so the preview's shape matches what the user
 * will see once the link is created.
 *
 * @param startPosition Source port position in screen space.
 * @param mousePosition Current pointer position in screen space.
 * @param portType Port type — drives the default color.
 * @param startColor Optional override for the gradient start.
 * @param endColor Optional override for the gradient end.
 */
@Composable
fun AzoraLinkCreationPreview(
    startPosition: Offset,
    mousePosition: Offset,
    portType: AzoraPortType,
    startColor: Color? = null,
    endColor: Color? = null
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val startX = startPosition.x
        val startY = startPosition.y
        val endX = mousePosition.x
        val endY = mousePosition.y

        val controlPointOffset = (endX - startX).coerceAtLeast(50f) / 2
        val previewPath = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                startX + controlPointOffset, startY,
                endX - controlPointOffset, endY,
                endX, endY
            )
        }

        val previewStartColor = startColor ?: portType.toColor()
        val previewEndColor = endColor ?: portType.toColor()

        val previewBrush = Brush.linearGradient(
            colors = listOf(previewStartColor, previewEndColor),
            start = Offset(startX, startY),
            end = Offset(endX, endY)
        )

        drawPath(
            path = previewPath,
            brush = previewBrush,
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
            )
        )

        // Draw circle at mouse position
        drawCircle(
            color = previewEndColor,
            radius = 8f,
            center = Offset(endX, endY)
        )
    }
}