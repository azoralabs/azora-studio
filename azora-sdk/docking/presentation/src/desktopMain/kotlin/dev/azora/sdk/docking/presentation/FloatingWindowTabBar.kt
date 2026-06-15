package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions

/**
 * Tab bar for floating windows containing multiple panels.
 *
 * Displays a row of tabs for each panel in a multi-panel floating window,
 * allowing users to switch between panels and drag tabs to reorder or
 * extract them.
 *
 * ## Features
 *
 * - Displays panel titles as clickable tabs
 * - Highlights the active tab
 * - Shows close buttons for closeable panels
 * - Supports drag-and-drop for tab extraction
 *
 * ## Drag Behavior
 *
 * When a tab is dragged:
 * 1. `onTabDragStart` is called with screen coordinates
 * 2. `onTabDrag` is called continuously with updated screen coordinates
 * 3. `onTabDragEnd` is called when the drag completes
 *
 * The parent window converts these screen coordinates to main window
 * local coordinates for the dock system to process.
 *
 * @param panelIds List of panel IDs in display order
 * @param panelDescriptors Map of panel descriptors for titles and settings
 * @param activeIndex Index of the currently active tab
 * @param awtWindowX Parent window's X position in screen pixels
 * @param awtWindowY Parent window's Y position in screen pixels
 * @param onTabSelect Called when a tab is clicked to select it
 * @param onTabClose Called when a tab's close button is clicked
 * @param onTabDragStart Called when tab drag begins (panelId, screenX, screenY)
 * @param onTabDrag Called during tab drag (screenX, screenY)
 * @param onTabDragEnd Called when tab drag ends
 * @param modifier Modifier for the tab bar container
 *
 * @see FloatingWindowTab for individual tab rendering
 * @see NativeFloatingWindow for the parent window
 */
@Composable
internal fun FloatingWindowTabBar(
    panelIds: List<String>,
    panelDescriptors: Map<String, DockPanelDescriptor>,
    activeIndex: Int,
    awtWindowX: Int,
    awtWindowY: Int,
    onTabSelect: (Int) -> Unit,
    onTabClose: (String) -> Unit,
    onTabDragStart: (panelId: String, screenX: Float, screenY: Float) -> Unit,
    onTabDrag: (screenX: Float, screenY: Float) -> Unit,
    onTabDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val density = LocalDensity.current.density

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.tabHeight)
            .background(colors.tabBarBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        panelIds.forEachIndexed { index, panelId ->
            val descriptor = panelDescriptors[panelId]
            val isActive = index == activeIndex

            FloatingWindowTab(
                panelId = panelId,
                title = descriptor?.title ?: "Panel",
                isActive = isActive,
                closeable = descriptor?.closeable ?: true,
                density = density,
                awtWindowX = awtWindowX,
                awtWindowY = awtWindowY,
                onClick = { onTabSelect(index) },
                onClose = { onTabClose(panelId) },
                onDragStart = { screenX, screenY -> onTabDragStart(panelId, screenX, screenY) },
                onDrag = onTabDrag,
                onDragEnd = onTabDragEnd
            )

            if (index < panelIds.size - 1) {
                Spacer(modifier = Modifier.width(dimensions.tabSpacing))
            }
        }
    }
}