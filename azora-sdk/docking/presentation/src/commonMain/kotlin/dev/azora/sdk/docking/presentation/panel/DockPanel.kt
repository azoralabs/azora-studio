package dev.azora.sdk.docking.presentation.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.tab.DockTabBar
import dev.azora.sdk.docking.presentation.drop.DockDropOverlay
import dev.azora.sdk.docking.presentation.theme.*
import dev.azora.sdk.docking.presentation.theme.LocalAllowExternalDrag
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview

/**
 * Container composable that renders a dock panel with its content.
 *
 * A dock panel is a leaf node in the dock layout tree. It displays one or more
 * [DockPanelDescriptor]s as tabs, showing the active panel's content and providing
 * a drop overlay for drag-and-drop operations.
 *
 * ## Layout Structure
 *
 * The panel layout varies based on the number of tabs:
 *
 * **Single panel:**
 * ```
 * +----------------------------------+
 * | [Header: Title        ] [Close] |
 * +----------------------------------+
 * |                                  |
 * |         Panel Content            |
 * |                                  |
 * +----------------------------------+
 * ```
 *
 * **Multiple panels (tabs):**
 * ```
 * +----------------------------------+
 * | [Tab1] [Tab2*] [Tab3]           |
 * +----------------------------------+
 * |                                  |
 * |      Active Tab Content          |
 * |                                  |
 * +----------------------------------+
 * ```
 *
 * ## Drag and Drop
 *
 * The panel supports both drag sources (tabs being dragged out) and drop
 * targets (receiving dragged panels). A [DockDropOverlay] appears when
 * dragging over the panel, showing available drop zones.
 *
 * @param panels List of panel descriptors to display as tabs
 * @param activeIndex Index of the currently active/visible panel
 * @param isDragging Whether a drag operation is currently in progress (globally)
 * @param isDragSource Whether this panel is the source of the current drag
 * @param dragPosition Current cursor position during drag (in root coordinates)
 * @param onTabSelect Called when a tab is selected with the tab index
 * @param onTabClose Called when a tab's close button is clicked with the panel ID
 * @param onTabReorder Called when tabs are reordered via drag with from/to indices
 * @param onTabDragStart Called when a tab drag begins with panel ID and position
 * @param onTabDrag Called during tab drag with panel ID and current position
 * @param onTabDragEnd Called when a tab drag ends with the panel ID
 * @param onTabDragCancel Called when a tab drag is cancelled with the panel ID
 * @param onPanelClose Called when the panel close button is clicked (single panel mode)
 * @param onPanelEnter Called when drag cursor enters this panel's drop zone
 * @param onZoneHover Called when drag cursor hovers over a drop zone (or null when leaving)
 * @param modifier Modifier for the panel container
 *
 * @see DockPanelHeader for the single-panel header
 * @see DockTabBar for the multi-panel tab bar
 * @see DockDropOverlay for the drop zone overlay
 */
@Composable
fun DockPanel(
    panels: List<DockPanelDescriptor>,
    activeIndex: Int,
    isDragging: Boolean,
    isDragSource: Boolean,
    dragPosition: Offset?,
    onTabSelect: (Int) -> Unit,
    onTabClose: (String) -> Unit,
    onTabReorder: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onTabDragStart: (String, Offset) -> Unit,
    onTabDrag: (String, Offset) -> Unit,
    onTabDragEnd: (String) -> Unit,
    onTabDragCancel: (String) -> Unit = {},
    onPanelClose: (String) -> Unit,
    onPanelEnter: () -> Unit,
    onZoneHover: (DockZone?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current
    val registry = LocalDockPanelRegistry.current
    val allowExternalDrag = LocalAllowExternalDrag.current

    val activePanel = panels.getOrNull(activeIndex)

    // Show drop overlay when:
    // - Dragging and this is not the source panel, OR
    // - Dragging from this panel but there are other panels that would remain (tab group with 2+ tabs)
    // This allows splitting a tab group by dragging one tab to LEFT/RIGHT/TOP/BOTTOM
    val showDropOverlay = isDragging && (!isDragSource || panels.size > 1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(dimensions.panelCornerRadius))
            .background(colors.panelBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab bar for multiple panels
            if (panels.size > 1) {
                DockTabBar(
                    panels = panels,
                    activeIndex = activeIndex,
                    onTabSelect = onTabSelect,
                    onTabClose = onTabClose,
                    onTabReorder = onTabReorder,
                    onTabDragStart = onTabDragStart,
                    onTabDrag = onTabDrag,
                    onTabDragEnd = onTabDragEnd,
                    onTabDragCancel = onTabDragCancel
                )
            }

            // Header for active panel (only show if single panel)
            activePanel?.let { panel ->
                if (panels.size == 1) {
                    DockPanelHeader(
                        descriptor = panel,
                        onClose = { onPanelClose(panel.id) },
                        onDragStart = if (allowExternalDrag) {{ offset -> onTabDragStart(panel.id, offset) }} else null,
                        onDrag = if (allowExternalDrag) {{ offset -> onTabDrag(panel.id, offset) }} else null,
                        onDragEnd = if (allowExternalDrag) {{ onTabDragEnd(panel.id) }} else null
                    )
                }
            }

            // Panel content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Render all panels to preserve composable state across tab switches.
                // Inactive panels are rendered first (invisible), active panel last (on top)
                // so the active panel receives pointer events.
                panels.filter { it.id != activePanel?.id }.forEach { panel ->
                    key(panel.id) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = 0f }
                        ) {
                            val content = registry.getContent(panel.id)
                            content?.invoke()
                        }
                    }
                }
                activePanel?.let { panel ->
                    key(panel.id) {
                        Box(modifier = Modifier.matchParentSize()) {
                            val content = registry.getContent(panel.id)
                            content?.invoke()
                        }
                    }
                }

                // Drop overlay when dragging over this panel
                if (showDropOverlay) {
                    DockDropOverlay(
                        dragPosition = dragPosition,
                        onZoneHover = { zone ->
                            if (zone != null) {
                                onPanelEnter()
                            }
                            onZoneHover(zone)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DockPanel_Preview() = AzoraPreview {
    DockPanel(
        panels = listOf(DockPanelDescriptor("explorer", "Explorer")),
        activeIndex = 0,
        isDragging = false,
        isDragSource = false,
        dragPosition = null,
        onTabSelect = {},
        onTabClose = {},
        onTabDragStart = { _, _ -> },
        onTabDrag = { _, _ -> },
        onTabDragEnd = {},
        onPanelClose = {},
        onPanelEnter = {},
        onZoneHover = {}
    )
}