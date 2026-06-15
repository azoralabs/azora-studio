package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.dp
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions

/**
 * Overlay that displays drop zones when a panel is dragged over a floating window.
 *
 * Shows visual indicators for where a dragged panel can be dropped:
 * - **Center**: Merge as a new tab
 * - **Left/Right**: Split horizontally
 * - **Top/Bottom**: Split vertically
 *
 * ## Coordinate System
 *
 * Uses screen pixel coordinates from [DragState.screenX] and [DragState.screenY]
 * to determine which zone the cursor is over. These are converted to window-local
 * coordinates by subtracting the AWT window position.
 *
 * ## Zone Detection
 *
 * Each [DropZoneButton] reports its bounds via [onGloballyPositioned]. The overlay
 * determines which zone is hovered by checking if the drag position falls within
 * any button's bounds.
 *
 * ## Visual Feedback
 *
 * - Semi-transparent overlay covers the entire window
 * - The hovered zone shows a highlighted region indicating where the panel will land
 * - Drop zone buttons show hover state when active
 *
 * @param dragState Current drag state for position tracking
 * @param awtWindowX Window's X position in screen pixels
 * @param awtWindowY Window's Y position in screen pixels
 * @param awtWindowWidth Window's width in screen pixels
 * @param awtWindowHeight Window's height in screen pixels
 * @param onZoneHover Called when the hovered zone changes
 *
 * @see DropZoneButton for individual zone button rendering
 * @see NativeFloatingWindow for the parent window
 */
@Composable
internal fun FloatingWindowDropZoneOverlay(
    dragState: DragState?,
    awtWindowX: Int,
    awtWindowY: Int,
    awtWindowWidth: Int,
    awtWindowHeight: Int,
    onZoneHover: (DockZone?) -> Unit
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    // Track bounds for each drop zone button (in window-local coordinates)
    var centerBounds by remember { mutableStateOf(Rect.Zero) }
    var leftBounds by remember { mutableStateOf(Rect.Zero) }
    var rightBounds by remember { mutableStateOf(Rect.Zero) }
    var topBounds by remember { mutableStateOf(Rect.Zero) }
    var bottomBounds by remember { mutableStateOf(Rect.Zero) }

    // Calculate drag position relative to this window (convert screen coords to window-local)
    val dragPosition = dragState?.let {
        Offset(it.screenX - awtWindowX, it.screenY - awtWindowY)
    }

    // Determine which zone is hovered based on drop zone button bounds
    val hoveredZone = remember(dragPosition, centerBounds, leftBounds, rightBounds, topBounds, bottomBounds) {
        when {
            dragPosition == null -> null
            centerBounds.contains(dragPosition) -> DockZone.CENTER
            leftBounds.contains(dragPosition) -> DockZone.LEFT
            rightBounds.contains(dragPosition) -> DockZone.RIGHT
            topBounds.contains(dragPosition) -> DockZone.TOP
            bottomBounds.contains(dragPosition) -> DockZone.BOTTOM
            else -> null
        }
    }

    LaunchedEffect(hoveredZone) {
        onZoneHover(hoveredZone)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dropZoneIndicator.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Show highlight for hovered zone
        if (hoveredZone != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let { mod ->
                        when (hoveredZone) {
                            DockZone.CENTER -> mod
                            DockZone.LEFT -> mod.fillMaxHeight().fillMaxWidth(0.3f).align(Alignment.CenterStart)
                            DockZone.RIGHT -> mod.fillMaxHeight().fillMaxWidth(0.3f).align(Alignment.CenterEnd)
                            DockZone.TOP -> mod.fillMaxWidth().fillMaxHeight(0.3f).align(Alignment.TopCenter)
                            DockZone.BOTTOM -> mod.fillMaxWidth().fillMaxHeight(0.3f).align(Alignment.BottomCenter)
                        }
                    }
                    .background(colors.dropZoneIndicator)
                    .border(2.dp, colors.dropZoneIndicatorBorder, RoundedCornerShape(dimensions.dropZoneCornerRadius))
            )
        }

        // Drop zone buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left zone
            DropZoneButton(
                zone = DockZone.LEFT,
                isHovered = hoveredZone == DockZone.LEFT,
                onBoundsChanged = { leftBounds = it },
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top zone
                DropZoneButton(
                    zone = DockZone.TOP,
                    isHovered = hoveredZone == DockZone.TOP,
                    onBoundsChanged = { topBounds = it },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Center zone
                DropZoneButton(
                    zone = DockZone.CENTER,
                    isHovered = hoveredZone == DockZone.CENTER,
                    onBoundsChanged = { centerBounds = it }
                )

                // Bottom zone
                DropZoneButton(
                    zone = DockZone.BOTTOM,
                    isHovered = hoveredZone == DockZone.BOTTOM,
                    onBoundsChanged = { bottomBounds = it },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Right zone
            DropZoneButton(
                zone = DockZone.RIGHT,
                isHovered = hoveredZone == DockZone.RIGHT,
                onBoundsChanged = { rightBounds = it },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}