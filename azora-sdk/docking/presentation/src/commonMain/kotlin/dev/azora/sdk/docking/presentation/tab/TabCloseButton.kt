package dev.azora.sdk.docking.presentation.tab

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.presentation.theme.*
import org.jetbrains.compose.resources.painterResource

/**
 * Small close button displayed within dock tabs.
 *
 * A compact icon button that shows a close (X) icon. The button
 * highlights on hover to indicate interactivity.
 *
 * ## Visual States
 *
 * - **Normal**: Transparent background with muted icon color
 * - **Hovered**: Colored background with highlighted icon
 *
 * ## Usage
 *
 * Used internally by [DockTab] for closeable panels:
 *
 * ```kotlin
 * if (descriptor.closeable) {
 *     TabCloseButton(onClick = { onClose(descriptor.id) })
 * }
 * ```
 *
 * @param onClick Called when the button is clicked
 * @param modifier Modifier for the button container
 *
 * @see DockTab for the parent tab component
 */
@Composable
internal fun TabCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonHovered by buttonInteractionSource.collectIsHoveredAsState()

    val iconColor = if (isButtonHovered) colors.tabCloseButtonHover else colors.tabCloseButton
    val bgColor = if (isButtonHovered) colors.tabCloseButtonBackground else Color.Transparent

    Box(
        modifier = modifier
            .size(dimensions.tabCloseButtonSize)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .hoverable(buttonInteractionSource)
            .clickable(interactionSource = buttonInteractionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_dock_close),
            contentDescription = "Close tab",
            tint = iconColor,
            modifier = Modifier.size(10.dp)
        )
    }
}