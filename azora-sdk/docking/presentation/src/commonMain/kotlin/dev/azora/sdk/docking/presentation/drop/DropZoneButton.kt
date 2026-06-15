package dev.azora.sdk.docking.presentation.drop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.dp
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.docking.presentation.theme.*
import org.jetbrains.compose.resources.*

/**
 * Circular button representing a dock drop zone.
 *
 * Displays an icon indicating the zone direction (arrows for edges, layers
 * icon for center). The button reports its bounds for hit detection and
 * changes appearance when hovered.
 *
 * ## Visual States
 *
 * - **Normal**: Semi-transparent background with border, accent-colored icon
 * - **Hovered**: Solid accent background with white icon
 *
 * ## Hit Detection
 *
 * The button uses [onGloballyPositioned] to report its bounds in root
 * coordinates. The parent [DockDropOverlay] uses these bounds to determine
 * which zone the drag cursor is over.
 *
 * @param zone The dock zone this button represents
 * @param iconRes The drawable resource for the zone icon
 * @param isHovered Whether the cursor is currently over this button
 * @param onBoundsChanged Called with the button's bounds in root coordinates
 * @param modifier Modifier for positioning
 *
 * @see DockDropOverlay for the parent overlay
 * @see DockZone for available zones
 */
@Composable
internal fun DropZoneButton(
    zone: DockZone,
    iconRes: DrawableResource,
    isHovered: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    val background = if (isHovered) colors.dropZoneIndicatorBorder else colors.dropZoneIndicator
    val iconColor = if (isHovered) Color.White else colors.dropZoneIndicatorBorder
    val borderColor = if (isHovered) colors.dropZoneIndicatorBorder else colors.dropZoneIndicatorBorder.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .size(dimensions.dropZoneCenterSize)
            .onGloballyPositioned { coords ->
                onBoundsChanged(coords.boundsInRoot())
            }
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = "Drop ${zone.name.lowercase()}",
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}