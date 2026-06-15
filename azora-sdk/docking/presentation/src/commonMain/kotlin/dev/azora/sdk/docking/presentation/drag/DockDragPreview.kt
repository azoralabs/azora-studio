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

@Composable
fun DockDragPreview(
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
            .width(200.dp)
            .height(40.dp)
            .alpha(0.85f)
            .shadow(8.dp, RoundedCornerShape(dimensions.panelCornerRadius))
            .background(colors.panelHeaderBackground, RoundedCornerShape(dimensions.panelCornerRadius))
            .border(1.dp, colors.dropZoneIndicatorBorder, RoundedCornerShape(dimensions.panelCornerRadius))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = descriptor.title,
            color = colors.panelHeaderText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun DockDragPreview_Preview() = AzoraPreview {
    DockDragPreview(
        descriptor = DockPanelDescriptor("panel", "Dragging Panel"),
        x = 0f,
        y = 0f
    )
}