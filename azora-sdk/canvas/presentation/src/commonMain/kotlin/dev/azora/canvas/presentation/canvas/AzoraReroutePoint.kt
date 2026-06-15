package dev.azora.canvas.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import dev.azora.canvas.domain.model.AzoraReroutePointModel
import kotlin.math.roundToInt

/**
 * Diamond-shaped waypoint marker drawn on top of a link, draggable to bend the link's curve.
 *
 * Positioning uses canvas-space coordinates ([reroutePoint] x/y) plus the current [panOffset], so
 * the persisted [AzoraReroutePointModel] is unchanged by panning. When [isSelected] the diamond
 * grows slightly and gains a soft white glow halo for emphasis.
 *
 * Interaction model:
 * - Left-click selects and starts a drag; the `onSelect`/`onMove`/`onEndDrag` triple lets the
 *   host commit position changes incrementally and checkpoint undo at drag end.
 * - Right-click opens the reroute context menu via [onRightClick] - but if a menu is already open
 *   for this point ([isContextMenuOpen] is true) the click instead dismisses it, mirroring native
 *   menu behavior.
 *
 * @param reroutePoint Domain model providing position and id.
 * @param color Fill color for the diamond - typically derived from the owning link's port type.
 * @param isSelected Drives the size bump and glow.
 * @param panOffset Current canvas pan applied to the model coordinates.
 * @param isContextMenuOpen When true, a right-click dismisses instead of re-opens.
 * @param onSelect Called on left-click before drag tracking starts.
 * @param onMove Called per drag frame with a position delta (not absolute position).
 * @param onEndDrag Called once after a drag - only if the pointer actually moved.
 * @param onRightClick Called on right-click with the world-space position of this reroute point.
 * @param onDismissContextMenus Called whenever this point is interacted with so the canvas can
 *   close any unrelated menus.
 */
@Composable
fun AzoraReroutePoint(
    reroutePoint: AzoraReroutePointModel,
    color: Color,
    isSelected: Boolean,
    panOffset: Offset,
    isContextMenuOpen: Boolean = false,
    onSelect: () -> Unit,
    onMove: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onRightClick: (Offset) -> Unit,
    onDismissContextMenus: () -> Unit
) {
    val size = if (isSelected) 30.dp else 24.dp
    val halfSize = size / 2

    val currentReroutePoint by rememberUpdatedState(reroutePoint)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)
    val currentOnRightClick by rememberUpdatedState(onRightClick)
    val currentIsContextMenuOpen by rememberUpdatedState(isContextMenuOpen)
    val currentOnDismissContextMenus by rememberUpdatedState(onDismissContextMenus)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (reroutePoint.x + panOffset.x - halfSize.toPx()).roundToInt(),
                    (reroutePoint.y + panOffset.y - halfSize.toPx()).roundToInt()
                )
            }
            .size(size)
            // Right-click handler
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.firstOrNull()?.consume()
                            if (currentIsContextMenuOpen) {
                                currentOnDismissContextMenus()
                            } else {
                                currentOnDismissContextMenus()
                                // Pass world-space position
                                currentOnRightClick(Offset(currentReroutePoint.x, currentReroutePoint.y))
                            }
                        }
                    }
                }
            }
            // Left-click handler for selection and drag
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.isConsumed) return@awaitEachGesture

                    currentOnDismissContextMenus()
                    currentOnSelect()
                    down.consume()

                    var hasDragged = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val dragAmount = change.positionChange()
                        if (dragAmount != Offset.Zero) {
                            hasDragged = true
                            change.consume()
                            currentOnMove(dragAmount)
                        }
                    }
                    if (hasDragged) {
                        currentOnEndDrag()
                    }
                }
            }
    ) {
        // Draw diamond shape with glow when selected
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diamondSize = size.toPx() / 2
            val centerX = size.toPx() / 2
            val centerY = size.toPx() / 2

            val path = Path().apply {
                moveTo(centerX, centerY - diamondSize)
                lineTo(centerX + diamondSize, centerY)
                lineTo(centerX, centerY + diamondSize)
                lineTo(centerX - diamondSize, centerY)
                close()
            }

            // Draw glow when selected
            if (isSelected) {
                val outerGlowPath = Path().apply {
                    val glowSize = diamondSize + 9f
                    moveTo(centerX, centerY - glowSize)
                    lineTo(centerX + glowSize, centerY)
                    lineTo(centerX, centerY + glowSize)
                    lineTo(centerX - glowSize, centerY)
                    close()
                }
                drawPath(
                    path = outerGlowPath,
                    color = Color.White.copy(alpha = 0.25f),
                    style = Fill
                )
            }

            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }
    }
}
