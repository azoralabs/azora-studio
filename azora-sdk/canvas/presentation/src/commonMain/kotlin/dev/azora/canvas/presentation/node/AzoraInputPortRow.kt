package dev.azora.canvas.presentation.node

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Labeled input port row: triangular [AzoraInputPort] on the left, followed by a label.
 *
 * Used inside [AzoraNode.headerContent] (typically inside [InputPortsWrapper]) to give the port a
 * left-aligned name. An empty [label] omits the spacer and text entirely so the row collapses to
 * just the port icon.
 *
 * @param label Text shown to the right of the port; pass empty to render the port alone.
 * @param type Forwarded to the underlying [AzoraInputPort].
 * @param isConnected Forwarded to the underlying [AzoraInputPort].
 * @param notConnectedCenterColor Forwarded to the underlying [AzoraInputPort].
 * @param modifier Modifier applied to the row.
 * @param onPositioned Forwarded to the underlying [AzoraInputPort].
 * @param onClick Forwarded to the underlying [AzoraInputPort].
 */
@Composable
fun AzoraInputPortRow(
    label: String,
    type: AzoraPortType,
    isConnected: Boolean = false,
    notConnectedCenterColor: Color,
    modifier: Modifier = Modifier,
    onPositioned: ((Offset) -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzoraInputPort(
            type = type,
            isConnected = isConnected,
            notConnectedCenterColor = notConnectedCenterColor,
            onPositioned = onPositioned,
            onClick = onClick
        )

        if (label.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))

            Text(
                text = label,
                color = AzoraPalette.Neutral30,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}