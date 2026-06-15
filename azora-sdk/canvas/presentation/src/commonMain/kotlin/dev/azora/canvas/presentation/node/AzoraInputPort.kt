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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.canvas.presentation.util.createRoundedTrianglePath
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Triangular execution-flow input port pointing right (into the node).
 *
 * Reports its center to [onPositioned] in root coordinates so the canvas can anchor incoming links
 * accurately — that callback fires from `onGloballyPositioned`, which Compose may invoke multiple
 * times per layout pass. The port shape varies by [type]: most types render as a single rounded
 * triangle, while [AzoraPortType.NAV_PUSH_REPLACE] is split horizontally into a green/orange pair
 * (per [AzoraPortType.mustSplitInHalf]).
 *
 * When [isConnected] is false, an inset inner triangle is drawn over the outer shape to give a
 * visible "empty socket" affordance, tinted by [notConnectedCenterColor] so it reads against the
 * node body color.
 *
 * @param type Port type — drives shape and color.
 * @param isConnected Whether at least one inbound link is attached.
 * @param notConnectedCenterColor Tint for the inner "empty socket" indicator.
 * @param modifier Layout modifier applied to the port hit-box.
 * @param onPositioned Reports the port's center in **root** coordinates.
 * @param onClick Fires on left-click; used by the canvas to drop an in-flight link onto this port.
 */
@Composable
fun AzoraInputPort(
    type: AzoraPortType,
    isConnected: Boolean = false,
    notConnectedCenterColor: Color,
    modifier: Modifier = Modifier,
    onPositioned: ((Offset) -> Unit)? = null,
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnPositioned by rememberUpdatedState(onPositioned)
    var isHovered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
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
            val r = 5f // corner radius

            if (type.mustSplitInHalf) {
                val (topColor, bottomColor) = when (type) {
                    AzoraPortType.NAV_PUSH_REPLACE -> Pair(AzoraPalette.AccentGreen, AzoraPalette.AccentOrange)
                    else -> Pair(AzoraPalette.Transparent, AzoraPalette.Transparent)
                }

                val displayTopColor = if (isHovered) topColor.copy(alpha = 0.8f) else topColor
                val displayBottomColor = if (isHovered) bottomColor.copy(alpha = 0.8f) else bottomColor

                // Top half
                val topPath = Path().apply {
                    moveTo(r, 0f)
                    lineTo(width - r, height / 2 - r)
                    quadraticTo(width, height / 2, width - r, height / 2)
                    lineTo(0f, height / 2)
                    lineTo(0f, r)
                    quadraticTo(0f, 0f, r, 0f)
                    close()
                }
                drawPath(topPath, displayTopColor)

                // Bottom half
                val bottomPath = Path().apply {
                    moveTo(0f, height / 2)
                    lineTo(width - r, height / 2)
                    quadraticTo(width, height / 2, width - r, height / 2 + r)
                    lineTo(r, height)
                    quadraticTo(0f, height, 0f, height - r)
                    close()
                }
                drawPath(bottomPath, displayBottomColor)
            } else {
                val color = when (type) {
                    AzoraPortType.NAV_PUSH -> AzoraPalette.AccentGreen
                    AzoraPortType.NAV_REPLACE -> AzoraPalette.AccentOrange
                    AzoraPortType.NAV_DIALOG -> AzoraPalette.White
                    else -> AzoraPalette.Transparent
                }

                val displayColor = if (isHovered) color.copy(alpha = 0.8f) else color

                // Outer triangle
                val outerPath = createRoundedTrianglePath(width, height, r)
                drawPath(outerPath, displayColor)
            }

            // Inner triangle - only when not connected
            if (!isConnected) {
                val scale = 0.6f
                val innerW = width * scale
                val innerH = height * scale
                val innerR = r * scale
                val offsetX = width * 0.15f
                val offsetY = (height - innerH) / 2

                val innerPath = createRoundedTrianglePath(innerW, innerH, innerR, offsetX, offsetY)
                drawPath(innerPath, AzoraPalette.Neutral85)
                drawPath(innerPath, notConnectedCenterColor.copy(alpha = 0.3f))
            }
        }
    }
}
