package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.dp
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions
import org.jetbrains.compose.resources.painterResource

/**
 * Interactive button representing a dock drop zone.
 *
 * Displays a clickable/hoverable indicator for a specific [DockZone].
 * Used in drop zone overlays to show where panels can be docked.
 *
 * ## Layout
 *
 * Buttons are typically arranged in a cross pattern:
 * ```
 *        [TOP]
 * [LEFT] [CENTER] [RIGHT]
 *       [BOTTOM]
 * ```
 *
 * ## Hit Detection
 *
 * The button reports its bounds via [onBoundsChanged] whenever it's positioned.
 * The parent overlay uses these bounds to determine which zone the cursor is over.
 *
 * ## Visual States
 *
 * - **Normal**: Semi-transparent background with border
 * - **Hovered**: Solid background with inverted icon color
 *
 * @param zone The dock zone this button represents
 * @param isHovered Whether the cursor is currently over this zone
 * @param onBoundsChanged Called with the button's bounds in window coordinates
 * @param modifier Modifier for positioning and spacing
 *
 * @see FloatingWindowDropZoneOverlay for the parent overlay
 * @see DockZone for available zones
 */
@Composable
internal fun DropZoneButton(
    zone: DockZone,
    isHovered: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    val background = if (isHovered) colors.dropZoneIndicatorBorder else colors.dropZoneIndicator.copy(alpha = 0.8f)
    val iconColor = if (isHovered) colors.floatingWindowBackground else colors.dropZoneIndicatorBorder

    Box(
        modifier = modifier
            .size(dimensions.dropZoneCenterSize)
            .onGloballyPositioned { coords ->
                onBoundsChanged(coords.boundsInWindow())
            }
            .clip(RoundedCornerShape(dimensions.dropZoneCornerRadius))
            .background(background)
            .border(1.dp, colors.dropZoneIndicatorBorder, RoundedCornerShape(dimensions.dropZoneCornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_dock_layers),
            contentDescription = zone.name,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}