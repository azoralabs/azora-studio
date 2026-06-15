package dev.azora.sdk.docking.presentation.panel

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
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import org.jetbrains.compose.resources.*

/**
 * Small icon button used in panel headers.
 *
 * Displays an icon that responds to hover with a background highlight.
 * Used for close, float, maximize, and other header actions.
 *
 * @param iconRes The drawable resource for the icon
 * @param contentDescription Accessibility description
 * @param onClick Called when the button is clicked
 * @param modifier Modifier for the button container
 */
@Composable
internal fun HeaderIconButton(
    iconRes: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val iconColor = if (isHovered) colors.panelHeaderTextActive else colors.panelHeaderText.copy(alpha = 0.6f)
    val bgColor = if (isHovered) colors.tabCloseButtonBackground else Color.Transparent

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(12.dp)
        )
    }
}