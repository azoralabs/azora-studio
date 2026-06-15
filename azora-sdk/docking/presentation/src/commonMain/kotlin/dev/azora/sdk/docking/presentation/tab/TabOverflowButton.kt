package dev.azora.sdk.docking.presentation.tab

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.presentation.theme.*
import org.jetbrains.compose.resources.painterResource

/**
 * Overflow button and dropdown menu for hidden tabs.
 *
 * When the tab bar has more tabs than can fit in the available width,
 * this button appears showing the count of hidden tabs. Clicking it
 * opens a dropdown menu listing the overflow tabs.
 *
 * ## Layout
 *
 * ```
 * +--------+
 * | +3  v  |  <- Shows count and dropdown arrow
 * +========+  <- Active indicator if overflow contains active tab
 * ```
 *
 * ## Visual States
 *
 * - **Normal**: Default tab background
 * - **Hovered**: Highlighted background
 * - **Has active tab**: Active background with indicator line
 *
 * ## Behavior
 *
 * - Shows count of overflow tabs (e.g., "+3")
 * - Displays dropdown arrow icon
 * - Opens dropdown menu on click
 * - Selecting a tab from the menu moves it to the front of visible tabs
 * - Shows active indicator if the currently active tab is in overflow
 *
 * @param overflowPanels List of panels that don't fit in the tab bar
 * @param activeOverflowIndex Index of active tab within overflow list, or -1 if none
 * @param showMenu Whether the dropdown menu is currently open
 * @param onShowMenu Called to show/hide the dropdown menu
 * @param onTabSelect Called when an overflow tab is selected with its overflow index
 * @param modifier Modifier for the button container
 *
 * @see DockTabBar for the parent tab bar
 */
@Composable
internal fun TabOverflowButton(
    overflowPanels: List<DockPanelDescriptor>,
    activeOverflowIndex: Int,
    showMenu: Boolean,
    onShowMenu: (Boolean) -> Unit,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    if (overflowPanels.isEmpty()) {
        // Empty placeholder when no overflow
        Box(modifier = modifier.size(0.dp))
        return
    }

    val hasActiveTab = activeOverflowIndex >= 0
    val background = when {
        hasActiveTab -> colors.tabBackgroundActive
        isHovered -> colors.tabBackgroundHover
        else -> colors.tabBackground
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.height(dimensions.tabHeight)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(dimensions.tabCornerRadius))
                    .background(background)
                    .hoverable(interactionSource)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        onShowMenu(true)
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+${overflowPanels.size}",
                        color = if (hasActiveTab) colors.tabTextActive else colors.tabText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_dock_arrow_down),
                        contentDescription = "More tabs",
                        tint = if (hasActiveTab) colors.tabTextActive else colors.tabText,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Show indicator if active tab is in overflow
            if (hasActiveTab) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.tabIndicatorHeight)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(colors.tabIndicator)
                )
            } else {
                Spacer(modifier = Modifier.height(dimensions.tabIndicatorHeight))
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onShowMenu(false) },
            modifier = Modifier.background(colors.panelBackground)
        ) {
            overflowPanels.forEachIndexed { index, panel ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = panel.title,
                            color = if (index == activeOverflowIndex) colors.tabTextActive else colors.tabText,
                            fontSize = 12.sp,
                            fontWeight = if (index == activeOverflowIndex) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onTabSelect(index)
                        onShowMenu(false)
                    },
                    modifier = if (index == activeOverflowIndex) {
                        Modifier.background(colors.tabBackgroundActive.copy(alpha = 0.5f))
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}