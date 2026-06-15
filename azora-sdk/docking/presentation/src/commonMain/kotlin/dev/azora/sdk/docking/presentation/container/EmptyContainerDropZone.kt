package dev.azora.sdk.docking.presentation.container

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.dp
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.docking.presentation.theme.*
import org.jetbrains.compose.resources.painterResource

/**
 * Drop zone overlay shown when the dock container is empty.
 *
 * When there are no panels in the dock layout and the user is dragging a panel,
 * this overlay provides a visual target for dropping the panel. It displays a
 * centered button that acts as the sole drop zone ([DockZone.CENTER]).
 *
 * ## Visual Feedback
 *
 * - **Normal**: Semi-transparent drop zone indicator in the center
 * - **Hovered**: Full container highlight with border, button color inverts
 *
 * ## Hit Detection
 *
 * The overlay tracks the bounds of the center button and checks if the
 * [dragPosition] falls within those bounds. When hovered, it reports
 * [DockZone.CENTER] via [onZoneHover].
 *
 * @param dragPosition Current drag position relative to the container, or null
 * @param onZoneHover Called when hover state changes ([DockZone.CENTER] or null)
 * @param modifier Modifier for the overlay container
 *
 * @see DockContainer for the parent container
 */
@Composable
internal fun EmptyContainerDropZone(
    dragPosition: Offset?,
    onZoneHover: (DockZone?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    var centerBounds by remember { mutableStateOf(Rect.Zero) }

    val isHovered = remember(dragPosition, centerBounds) {
        dragPosition != null && centerBounds.contains(dragPosition)
    }

    LaunchedEffect(isHovered) {
        onZoneHover(if (isHovered) DockZone.CENTER else null)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Show fullscreen highlight when hovered
        if (isHovered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dropZoneIndicator)
                    .border(2.dp, colors.dropZoneIndicatorBorder, RoundedCornerShape(dimensions.dropZoneCornerRadius))
            )
        }

        // Center drop zone button
        val background = if (isHovered) colors.dropZoneIndicatorBorder else colors.dropZoneIndicator
        val iconColor = if (isHovered) Color.White else colors.dropZoneIndicatorBorder

        Box(
            modifier = Modifier
                .size(dimensions.dropZoneCenterSize)
                .onGloballyPositioned { coords ->
                    centerBounds = coords.boundsInRoot()
                }
                .clip(RoundedCornerShape(dimensions.dropZoneCornerRadius))
                .background(background)
                .border(1.dp, colors.dropZoneIndicatorBorder, RoundedCornerShape(dimensions.dropZoneCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_dock_layers),
                contentDescription = "Drop here",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}