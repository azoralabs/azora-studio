package dev.azora.sdk.docking.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import dev.azora.sdk.docking.domain.DockZone

/**
 * Bundles all dock-related callbacks into a single immutable object.
 *
 * This data class reduces parameter proliferation in composables like
 * [dev.azora.sdk.docking.presentation.renderer.DockNodeRenderer] by grouping 15+ callback parameters into one.
 * Using a single callbacks object improves code readability and makes
 * it easier to pass callbacks through multiple layers of composition.
 *
 * ## Callback Categories
 *
 * **Split Operations:**
 * - [onSplitResize] - Adjust the ratio between split panes
 *
 * **Tab Operations:**
 * - [onTabSelect] - Select a tab by index
 * - [onTabClose] - Close a specific tab
 * - [onTabReorder] - Reorder tabs via drag
 * - [onTabDragStart], [onTabDrag], [onTabDragEnd], [onTabDragCancel] - Tab drag lifecycle
 *
 * **Panel Operations:**
 * - [onPanelClose] - Close the entire panel
 * - [onPanelFloat] - Detach panel to floating window
 * - [onPanelMaximize] - Maximize the panel
 * - [onPanelRestore] - Restore from maximized state
 *
 * **Hover/Focus:**
 * - [onPanelEnter], [onPanelExit] - Track mouse hover for drop zones
 * - [onZoneHover] - Track which drop zone is hovered
 *
 * ## Usage
 *
 * ```kotlin
 * val callbacks = DockCallbacks(
 *     onSplitResize = { nodeId, ratio -> dispatch(DockAction.SetSplitRatio(nodeId, ratio)) },
 *     onTabSelect = { nodeId, index -> dispatch(DockAction.SelectTab(nodeId, index)) },
 *     // ... other callbacks
 * )
 *
 * DockNodeRenderer(node = rootNode, callbacks = callbacks)
 * ```
 *
 * @property onSplitResize Called when a splitter is dragged (nodeId, newRatio)
 * @property onTabSelect Called when a tab is clicked (nodeId, tabIndex)
 * @property onTabClose Called when a tab's close button is clicked (panelId)
 * @property onTabReorder Called when tabs are reordered (nodeId, fromIndex, toIndex)
 * @property onTabDragStart Called when tab drag begins (panelId, startOffset)
 * @property onTabDrag Called during tab drag (panelId, currentOffset)
 * @property onTabDragEnd Called when tab drag ends (panelId)
 * @property onTabDragCancel Called when tab drag is cancelled (panelId)
 * @property onPanelClose Called to close a panel entirely (panelId)
 * @property onPanelFloat Called to float a panel (panelId)
 * @property onPanelMaximize Called to maximize a panel (panelId)
 * @property onPanelRestore Called to restore a maximized panel (panelId)
 * @property onPanelEnter Called when mouse enters a panel area (nodeId)
 * @property onPanelExit Called when mouse exits a panel area (nodeId)
 * @property onZoneHover Called when hovering over drop zones (nodeId, zone)
 *
 * @see dev.azora.sdk.docking.presentation.renderer.DockNodeRenderer
 * @see dev.azora.sdk.docking.domain.DockAction
 */
@Immutable
data class DockCallbacks(
    val onSplitResize: (nodeId: String, ratio: Float) -> Unit,
    val onTabSelect: (nodeId: String, index: Int) -> Unit,
    val onTabClose: (panelId: String) -> Unit,
    val onTabReorder: (nodeId: String, fromIndex: Int, toIndex: Int) -> Unit,
    val onTabDragStart: (panelId: String, offset: Offset) -> Unit,
    val onTabDrag: (panelId: String, offset: Offset) -> Unit,
    val onTabDragEnd: (panelId: String) -> Unit,
    val onTabDragCancel: (panelId: String) -> Unit,
    val onPanelClose: (panelId: String) -> Unit,
    val onPanelFloat: (panelId: String) -> Unit,
    val onPanelMaximize: (panelId: String) -> Unit,
    val onPanelRestore: (panelId: String) -> Unit,
    val onPanelEnter: (nodeId: String) -> Unit,
    val onPanelExit: (nodeId: String) -> Unit,
    val onZoneHover: (nodeId: String, zone: DockZone?) -> Unit
) {

    companion object {

        /**
         * A no-op callbacks instance for previews, testing, or read-only views.
         *
         * All callbacks are empty lambdas that do nothing when invoked.
         * Useful when rendering dock UI without interaction support.
         */
        val Empty = DockCallbacks(
            onSplitResize = { _, _ -> },
            onTabSelect = { _, _ -> },
            onTabClose = { },
            onTabReorder = { _, _, _ -> },
            onTabDragStart = { _, _ -> },
            onTabDrag = { _, _ -> },
            onTabDragEnd = { },
            onTabDragCancel = { },
            onPanelClose = { },
            onPanelFloat = { },
            onPanelMaximize = { },
            onPanelRestore = { _ -> },
            onPanelEnter = { },
            onPanelExit = { },
            onZoneHover = { _, _ -> }
        )
    }
}
