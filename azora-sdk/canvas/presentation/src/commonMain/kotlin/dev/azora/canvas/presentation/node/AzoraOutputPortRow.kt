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
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * Labeled output port row: label on the left, triangular [AzoraOutputPort] on the right.
 *
 * Mirror of [AzoraInputPortRow] for the output side. When [enabled] is false, the label is dimmed
 * to match the disabled port. Empty [label] collapses the row to just the port icon.
 *
 * @param label Text shown to the left of the port; pass empty to render the port alone.
 * @param type Forwarded to the underlying [AzoraOutputPort].
 * @param isConnected Forwarded to the underlying [AzoraOutputPort].
 * @param notConnectedCenterColor Forwarded to the underlying [AzoraOutputPort].
 * @param enabled Forwarded to the underlying [AzoraOutputPort]; also dims the label.
 * @param modifier Modifier applied to the row.
 * @param onPositioned Forwarded to the underlying [AzoraOutputPort].
 * @param onClick Forwarded to the underlying [AzoraOutputPort].
 */
@Composable
fun AzoraOutputPortRow(
    label: String,
    type: AzoraPortType,
    isConnected: Boolean = false,
    notConnectedCenterColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onPositioned: ((Offset) -> Unit)? = null,
    onClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val textColor = if (enabled) palette.contentMid else palette.disabled

    Row(
        modifier = modifier.padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(4.dp))
        }

        AzoraOutputPort(
            type = type,
            isConnected = isConnected,
            notConnectedCenterColor = notConnectedCenterColor,
            enabled = enabled,
            onPositioned = onPositioned,
            onClick = onClick
        )
    }
}