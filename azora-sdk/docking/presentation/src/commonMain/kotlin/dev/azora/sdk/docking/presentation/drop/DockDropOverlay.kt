package dev.azora.sdk.docking.presentation.drop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.dp
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.docking.presentation.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview

/**
 * Overlay that displays drop zone indicators when a panel is being dragged.
 *
 * Shows a cross-pattern of circular buttons representing the five dock zones:
 * - **Center**: Tab merge
 * - **Left/Right**: Horizontal split
 * - **Top/Bottom**: Vertical split
 *
 * When the drag position hovers over a zone button, the corresponding area
 * of the panel is highlighted to preview where the dropped panel will appear.
 *
 * ## Layout
 *
 * ```
 *        [↑]
 *    [←] [◻] [→]
 *        [↓]
 * ```
 *
 * ## Zone Detection
 *
 * Each [DropZoneButton] reports its bounds via [onGloballyPositioned]. The overlay
 * checks if [dragPosition] falls within any button's bounds to determine which
 * zone is hovered.
 *
 * ## Visual Feedback
 *
 * - Hovered zone button changes to filled/inverted style
 * - Background highlight shows the region the dropped panel will occupy
 * - Border outlines the drop target area
 *
 * @param dragPosition Current drag position relative to the overlay, or null
 * @param onZoneHover Called when the hovered zone changes (zone or null)
 * @param modifier Modifier for the overlay container
 *
 * @see DropZoneButton for individual zone button rendering
 * @see DockZone for available zones
 */
@Composable
fun DockDropOverlay(
    dragPosition: Offset?,
    onZoneHover: (DockZone?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    // Track bounds of each zone button
    var centerBounds by remember { mutableStateOf(Rect.Zero) }
    var leftBounds by remember { mutableStateOf(Rect.Zero) }
    var rightBounds by remember { mutableStateOf(Rect.Zero) }
    var topBounds by remember { mutableStateOf(Rect.Zero) }
    var bottomBounds by remember { mutableStateOf(Rect.Zero) }

    // Determine which zone the drag position is over
    val hoveredZone = remember(dragPosition, centerBounds, leftBounds, rightBounds, topBounds, bottomBounds) {
        if (dragPosition == null) return@remember null
        when {
            centerBounds.contains(dragPosition) -> DockZone.CENTER
            leftBounds.contains(dragPosition) -> DockZone.LEFT
            rightBounds.contains(dragPosition) -> DockZone.RIGHT
            topBounds.contains(dragPosition) -> DockZone.TOP
            bottomBounds.contains(dragPosition) -> DockZone.BOTTOM
            else -> null
        }
    }

    // Notify parent of hovered zone changes
    LaunchedEffect(hoveredZone) {
        onZoneHover(hoveredZone)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Highlight area based on hovered zone
        hoveredZone?.let { zone ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (zone) {
                            DockZone.LEFT -> Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterStart)
                            DockZone.RIGHT -> Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd)
                            DockZone.TOP -> Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter)
                            DockZone.BOTTOM -> Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                            DockZone.CENTER -> Modifier.fillMaxSize()
                        }
                    )
                    .background(colors.dropZoneHighlight)
                    .border(2.dp, colors.dropZoneIndicatorBorder.copy(alpha = 0.6f), RoundedCornerShape(dimensions.dropZoneCornerRadius))
            )
        }

        // Drop zone indicators - compact circular design
        Box(
            modifier = Modifier.size(dimensions.dropZoneIndicatorSize * 2.5f),
            contentAlignment = Alignment.Center
        ) {
            // Center zone
            DropZoneButton(
                zone = DockZone.CENTER,
                iconRes = Res.drawable.ic_dock_layers,
                isHovered = hoveredZone == DockZone.CENTER,
                onBoundsChanged = { centerBounds = it },
                modifier = Modifier.align(Alignment.Center)
            )

            // Left zone
            DropZoneButton(
                zone = DockZone.LEFT,
                iconRes = Res.drawable.ic_dock_arrow_left,
                isHovered = hoveredZone == DockZone.LEFT,
                onBoundsChanged = { leftBounds = it },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = dimensions.dropZoneSpacing)
            )

            // Right zone
            DropZoneButton(
                zone = DockZone.RIGHT,
                iconRes = Res.drawable.ic_dock_arrow_right,
                isHovered = hoveredZone == DockZone.RIGHT,
                onBoundsChanged = { rightBounds = it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = dimensions.dropZoneSpacing)
            )

            // Top zone
            DropZoneButton(
                zone = DockZone.TOP,
                iconRes = Res.drawable.ic_dock_arrow_up,
                isHovered = hoveredZone == DockZone.TOP,
                onBoundsChanged = { topBounds = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = dimensions.dropZoneSpacing)
            )

            // Bottom zone
            DropZoneButton(
                zone = DockZone.BOTTOM,
                iconRes = Res.drawable.ic_dock_arrow_down,
                isHovered = hoveredZone == DockZone.BOTTOM,
                onBoundsChanged = { bottomBounds = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensions.dropZoneSpacing)
            )
        }
    }
}

@Preview
@Composable
private fun DockDropOverlay_Preview() = AzoraPreview {
    DockDropOverlay(
        dragPosition = null,
        onZoneHover = {}
    )
}