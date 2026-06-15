package dev.azora.sdk.docking.presentation.drag

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.presentation.theme.*
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview
import kotlin.math.roundToInt

/**
 * A floating window preview shown when dragging a docked panel.
 * Looks like a floating window to indicate it will become floating.
 */
@Composable
fun DockDragPreviewWindow(
    descriptor: DockPanelDescriptor?,
    x: Float,
    y: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    if (descriptor == null) return

    Box(
        modifier = modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(300.dp)
            .height(200.dp)
            .alpha(0.9f)
            .shadow(12.dp, RoundedCornerShape(dimensions.panelCornerRadius))
            .background(colors.panelBackground, RoundedCornerShape(dimensions.panelCornerRadius))
            .border(2.dp, colors.dropZoneIndicatorBorder, RoundedCornerShape(dimensions.panelCornerRadius))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.panelHeaderBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = descriptor.title,
                    color = colors.panelHeaderText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            // Content area placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Drop to dock or release to float",
                    color = colors.panelHeaderText.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
private fun DockDragPreviewWindow_Preview() = AzoraPreview {
    DockDragPreviewWindow(
        descriptor = DockPanelDescriptor("panel", "Floating Panel"),
        x = 0f,
        y = 0f
    )
}