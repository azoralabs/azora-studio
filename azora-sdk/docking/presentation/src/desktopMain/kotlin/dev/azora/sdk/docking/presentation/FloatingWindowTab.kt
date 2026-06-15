package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions
import org.jetbrains.compose.resources.painterResource

/**
 * Individual tab component for floating window tab bars.
 *
 * Renders a single tab with the panel title and optional close button.
 * Supports both click-to-select and drag-to-extract interactions.
 *
 * ## Drag Handling
 *
 * When dragged, the tab reports its position in screen coordinates by combining:
 * - The tab's position within the window (`tabPosition`)
 * - The window's position on screen (`awtWindowX`, `awtWindowY`)
 * - The display density factor for proper scaling
 *
 * This allows the dock system to accurately track the drag across windows
 * and determine valid drop targets.
 *
 * ## Visual States
 *
 * - **Active**: Highlighted background, medium font weight
 * - **Inactive**: Normal background, normal font weight
 * - **Hovered**: (Handled by parent for close button visibility)
 *
 * @param panelId The unique identifier of the panel this tab represents
 * @param title The display title for the tab
 * @param isActive Whether this tab is currently selected
 * @param closeable Whether to show a close button
 * @param density Display density for coordinate conversion
 * @param awtWindowX Parent window's X position in screen pixels
 * @param awtWindowY Parent window's Y position in screen pixels
 * @param onClick Called when the tab is clicked
 * @param onClose Called when the close button is clicked
 * @param onDragStart Called when drag begins with screen coordinates
 * @param onDrag Called during drag with screen coordinates
 * @param onDragEnd Called when drag completes or is cancelled
 * @param modifier Modifier for the tab container
 *
 * @see FloatingWindowTabBar for the parent tab bar
 */
@Composable
internal fun FloatingWindowTab(
    panelId: String,
    title: String,
    isActive: Boolean,
    closeable: Boolean,
    density: Float,
    awtWindowX: Int,
    awtWindowY: Int,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onDragStart: (screenX: Float, screenY: Float) -> Unit,
    onDrag: (screenX: Float, screenY: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    val background = if (isActive) colors.tabBackgroundActive else colors.tabBackground
    val textColor = if (isActive) colors.tabTextActive else colors.tabText

    var tabPosition by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = modifier
            .height(dimensions.tabHeight - 4.dp)
            .clip(RoundedCornerShape(dimensions.tabCornerRadius))
            .background(background)
            .onGloballyPositioned { coords ->
                tabPosition = coords.positionInWindow()
            }
            .pointerInput(panelId) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Convert to screen coordinates
                        val screenX = (tabPosition.x + offset.x) / density + awtWindowX
                        val screenY = (tabPosition.y + offset.y) / density + awtWindowY
                        onDragStart(screenX, screenY)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        // Convert to screen coordinates
                        val screenX = (tabPosition.x + change.position.x) / density + awtWindowX
                        val screenY = (tabPosition.y + change.position.y) / density + awtWindowY
                        onDrag(screenX, screenY)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = dimensions.tabPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (closeable) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(Res.drawable.ic_dock_close),
                contentDescription = "Close tab",
                tint = colors.tabCloseButton,
                modifier = Modifier
                    .size(12.dp)
                    .clickableNoRipple(onClick = onClose)
            )
        }
    }
}