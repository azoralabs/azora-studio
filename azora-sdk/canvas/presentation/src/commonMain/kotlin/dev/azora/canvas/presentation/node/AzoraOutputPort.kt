package dev.azora.canvas.presentation.node

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.canvas.presentation.util.createRoundedTrianglePath
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Triangular execution-flow output port pointing right (out of the node).
 *
 * Mirrors [AzoraInputPort] but on the right-hand side of a node and with an [enabled] flag -
 * disabled ports render greyed-out and ignore clicks, useful for ports that exist structurally
 * but aren't valid in the current node configuration. Unlike the input port, this composable
 * tracks pointer enter/exit to draw a hover state, since output ports are the originator of new
 * link drags.
 *
 * @param type Port type - drives color.
 * @param isConnected Whether at least one outbound link is attached.
 * @param notConnectedCenterColor Tint for the inner "empty socket" indicator.
 * @param enabled When false the port is dimmed and non-interactive.
 * @param onPositioned Reports the port's center in **root** coordinates.
 * @param onClick Fires on left-click; used by the canvas to begin link creation from this port.
 */
@Composable
fun AzoraOutputPort(
    type: AzoraPortType,
    isConnected: Boolean = false,
    notConnectedCenterColor: Color,
    enabled: Boolean = true,
    onPositioned: ((Offset) -> Unit)? = null,
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnPositioned by rememberUpdatedState(onPositioned)
    var isHovered by remember { mutableStateOf(false) }

    val color = when (type) {
        AzoraPortType.NAV_ROOT -> AzoraPalette.AccentRed
        AzoraPortType.NAV_PUSH -> AzoraPalette.AccentGreen
        AzoraPortType.NAV_REPLACE -> AzoraPalette.AccentOrange
        AzoraPortType.NAV_DIALOG -> AzoraPalette.White
        else -> AzoraPalette.Transparent
    }

    val baseColor = if (enabled) color else AzoraPalette.Neutral60
    val displayColor = if (isHovered && enabled) baseColor.copy(alpha = 0.8f) else baseColor

    Box(
        modifier = Modifier
            .size(16.dp)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val center = Offset(
                    position.x + coordinates.size.width / 2f,
                    position.y + coordinates.size.height / 2f
                )
                currentOnPositioned?.invoke(center)
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                            else -> {}
                        }
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                        currentOnClick()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val cornerRadius = 5f

            // Outer triangle
            val outerPath = createRoundedTrianglePath(width, height, cornerRadius)
            drawPath(outerPath, displayColor)

            // Inner triangle - only when not connected
            if (!isConnected) {
                val scale = 0.6f
                val innerW = width * scale
                val innerH = height * scale
                val innerR = cornerRadius * scale
                val offsetX = width * 0.15f
                val offsetY = (height - innerH) / 2

                val innerPath = createRoundedTrianglePath(innerW, innerH, innerR, offsetX, offsetY)
                drawPath(innerPath, AzoraPalette.Neutral85)
                drawPath(innerPath, notConnectedCenterColor.copy(alpha = 0.3f))
            }
        }
    }
}