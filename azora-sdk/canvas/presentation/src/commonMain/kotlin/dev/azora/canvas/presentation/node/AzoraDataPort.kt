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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.azora.canvas.domain.type.AzoraPortDataType
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Circular data-flow port used for both inputs and outputs of value-carrying nodes.
 *
 * Where [AzoraInputPort] / [AzoraOutputPort] use a triangular shape for **execution** flow, this
 * port is a circle to make data flow visually distinct. Color is determined by [dataType] (see
 * [dev.azora.canvas.presentation.util.toColor]). When [isConnected] is false an inner circle is
 * drawn over the outer one to give an "empty socket" look.
 *
 * Hovering brightens the fill via alpha, and a click is detected via a `down`/`up`/`consume`
 * sequence so it interoperates with surrounding drag gestures on the node body.
 *
 * @param dataType Drives the port color.
 * @param isConnected Whether the port has at least one link attached.
 * @param modifier Layout modifier applied to the port hit-box.
 * @param onClick Fires on left-click; the host wires this to start/finish a data link.
 */
@Composable
fun AzoraDataPort(
    dataType: AzoraPortDataType,
    isConnected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    var isHovered by remember { mutableStateOf(false) }

    val color = when (dataType) {
        AzoraPortDataType.BOOL -> AzoraPalette.AccentRed
        AzoraPortDataType.INTEGER -> Color(0xFF33CCCC)
        AzoraPortDataType.REAL -> Color(0xFF33CC33)
        AzoraPortDataType.TEXT -> Color(0xFFCC33CC)
        AzoraPortDataType.ENUM -> Color(0xFFCCCC33)
        AzoraPortDataType.DATA_CLASS -> Color(0xFF3366CC)
        AzoraPortDataType.ANY -> Color(0xFF999999)
    }

    val displayColor = if (isHovered) color.copy(alpha = 0.8f) else color

    Box(
        modifier = modifier
            .size(16.dp)
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
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Outer circle
            drawCircle(
                color = displayColor,
                radius = radius,
                center = center
            )

            // Inner circle - only when not connected
            if (!isConnected) {
                drawCircle(
                    color = AzoraPalette.Neutral90,
                    radius = radius * 0.6f,
                    center = center
                )
            }
        }
    }
}
