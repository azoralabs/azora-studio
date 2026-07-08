package dev.azora.sdk.docking.domain

/**
 * Actions that can be performed on the docking system.
 *
 * All state modifications in the docking system are represented as actions.
 * Actions are dispatched to a [DockStateManager] which processes them and
 * updates the state accordingly.
 *
 * ## Panel Actions
 * - [AddPanel] - Add a new panel to the layout
 * - [RemovePanel] - Remove a panel from the layout
 * - [MovePanel] - Move a panel to a new location
 * - [FloatPanel] - Detach a panel into a floating window
 * - [SelectPanel] - Select/focus a panel
 * - [MaximizePanel] - Maximize or restore a panel
 *
 * ## Floating Window Actions
 * - [DockFloatingWindow] - Dock a floating window back to the layout
 * - [MoveFloatingWindow] - Move a floating window
 * - [ResizeFloatingWindow] - Resize a floating window
 * - [CloseFloatingWindow] - Close a floating window
 * - [DockToFloatingWindow] - Merge two floating windows
 *
 * ## Layout Actions
 * - [SetSplitRatio] - Adjust the split ratio between panels
 * - [SelectTab] - Select a tab in a tab group
 * - [ReorderTabs] - Reorder tabs in a tab group
 * - [ApplyLayout] - Replace the entire layout
 * - [ResetLayout] - Reset to the default layout
 *
 * ## Drag Actions
 * - [StartDrag] - Begin dragging a panel
 * - [UpdateDragPosition] - Update drag position
 * - [UpdateDragHover] - Update hover target during drag
 * - [EndDrag] - Complete the drag operation
 * - [CancelDrag] - Cancel the drag operation
 *
 * @see DockStateManager.dispatch
 */
sealed interface DockAction {

    /**
     * Adds a new panel to the layout.
     *
     * @property descriptor The panel descriptor to add
     * @property targetNodeId The node to add the panel to (null for root)
     * @property zone Where to place the panel relative to the target
     */
    data class AddPanel(
        val descriptor: DockPanelDescriptor,
        val targetNodeId: String? = null,
        val zone: DockZone = DockZone.CENTER
    ) : DockAction

    /**
     * Removes a panel from the layout.
     *
     * The panel is removed from its current location. If the panel is in a tab group,
     * only that panel is removed. If removal leaves a split with only one child,
     * the split is collapsed.
     *
     * @property panelId The unique identifier of the panel to remove
     * @see DockLayoutOperations.removePanel
     */
    data class RemovePanel(val panelId: String) : DockAction

    /**
     * Changes a panel's tab title, e.g. after the backing file is renamed on disk.
     *
     * @property panelId The unique identifier of the panel to retitle
     * @property title The new tab title
     */
    data class UpdatePanelTitle(val panelId: String, val title: String) : DockAction

    /**
     * Moves a panel to a new location in the layout.
     *
     * The panel is removed from its current location and added to the target node
     * at the specified zone. This is typically triggered by drag-and-drop operations.
     *
     * @property panelId The unique identifier of the panel to move
     * @property targetNodeId The ID of the node to move the panel to
     * @property zone Where to place the panel relative to the target (LEFT, RIGHT, TOP, BOTTOM, CENTER)
     * @see DockLayoutOperations.movePanel
     * @see DockZone
     */
    data class MovePanel(
        val panelId: String,
        val targetNodeId: String,
        val zone: DockZone
    ) : DockAction

    /**
     * Detaches a panel from the docked layout into a floating window.
     *
     * Creates a new floating window containing the panel. The panel is removed
     * from its current docked location and placed in the floating window at
     * the specified position and size.
     *
     * @property panelId The unique identifier of the panel to float
     * @property x The X position for the floating window (in screen coordinates)
     * @property y The Y position for the floating window (in screen coordinates)
     * @property width The width of the floating window
     * @property height The height of the floating window
     * @see DockLayoutOperations.floatPanel
     * @see DockDefaults for default position and size values
     */
    data class FloatPanel(
        val panelId: String,
        val x: Float = DockDefaults.FLOATING_WINDOW_X,
        val y: Float = DockDefaults.FLOATING_WINDOW_Y,
        val width: Float = DockDefaults.FLOATING_WINDOW_WIDTH,
        val height: Float = DockDefaults.FLOATING_WINDOW_HEIGHT
    ) : DockAction

    /**
     * Docks a floating window back into the main layout.
     *
     * The floating window is closed and its content is inserted into the
     * docked layout at the specified target node and zone.
     *
     * @property windowId The unique identifier of the floating window to dock
     * @property targetNodeId The ID of the target node (null to dock at root)
     * @property zone Where to place the content relative to the target
     * @see DockLayoutOperations.dockFloatingWindow
     */
    data class DockFloatingWindow(
        val windowId: String,
        val targetNodeId: String?,
        val zone: DockZone
    ) : DockAction

    /**
     * Moves a floating window to a new position.
     *
     * Updates the position of an existing floating window. This is typically
     * triggered by dragging the window's title bar.
     *
     * @property windowId The unique identifier of the floating window
     * @property x The new X position (in screen coordinates)
     * @property y The new Y position (in screen coordinates)
     * @see DockLayoutOperations.moveFloatingWindow
     */
    data class MoveFloatingWindow(
        val windowId: String,
        val x: Float,
        val y: Float
    ) : DockAction

    /**
     * Resizes a floating window.
     *
     * Updates the dimensions of an existing floating window. This is typically
     * triggered by dragging the window's resize handles.
     *
     * @property windowId The unique identifier of the floating window
     * @property width The new width
     * @property height The new height
     * @see DockLayoutOperations.resizeFloatingWindow
     */
    data class ResizeFloatingWindow(
        val windowId: String,
        val width: Float,
        val height: Float
    ) : DockAction

    /**
     * Closes a floating window.
     *
     * Removes the floating window and all panels it contains from the layout.
     *
     * @property windowId The unique identifier of the floating window to close
     * @see DockLayoutOperations.closeFloatingWindow
     */
    data class CloseFloatingWindow(val windowId: String) : DockAction

    /**
     * Toggles the minimized state of a floating window.
     *
     * When minimized, the window collapses to show only its title bar.
     * Toggling again restores the window to its previous size.
     *
     * @property windowId The unique identifier of the floating window
     * @see DockLayoutOperations.toggleFloatingWindowMinimized
     */
    data class ToggleFloatingWindowMinimized(val windowId: String) : DockAction

    /**
     * Toggles the maximized state of a floating window.
     *
     * When maximized, the window expands to fill the available space.
     * Toggling again restores the window to its previous position and size.
     *
     * @property windowId The unique identifier of the floating window
     * @see DockLayoutOperations.toggleFloatingWindowMaximized
     */
    data class ToggleFloatingWindowMaximized(val windowId: String) : DockAction

    /**
     * Toggles the fullscreen state of a floating window.
     *
     * When fullscreen, the window covers the entire screen without decorations.
     * Toggling again restores the window to its previous state.
     *
     * @property windowId The unique identifier of the floating window
     * @see DockLayoutOperations.toggleFloatingWindowFullscreen
     */
    data class ToggleFloatingWindowFullscreen(val windowId: String) : DockAction

    /**
     * Adjusts the split ratio between two panels in a split node.
     *
     * The ratio determines how space is divided between the first and second
     * children of a split. A ratio of 0.3 means the first child gets 30% of
     * the space and the second gets 70%.
     *
     * @property nodeId The unique identifier of the split node
     * @property ratio The new split ratio (clamped to [DockDefaults.MIN_SPLIT_RATIO]..[DockDefaults.MAX_SPLIT_RATIO])
     * @see DockLayoutOperations.setSplitRatio
     * @see DockNode.Split
     */
    data class SetSplitRatio(
        val nodeId: String,
        val ratio: Float
    ) : DockAction

    /**
     * Selects a tab in a tab group by index.
     *
     * Updates the active tab in a tab group node. The selected tab's content
     * becomes visible while other tabs are hidden.
     *
     * @property nodeId The unique identifier of the tab group node
     * @property tabIndex The zero-based index of the tab to select
     * @see DockLayoutOperations.selectTab
     * @see DockNode.TabGroup
     */
    data class SelectTab(
        val nodeId: String,
        val tabIndex: Int
    ) : DockAction

    /**
     * Selects a panel by ID, making it visible and focused.
     *
     * If the panel is in a tab group, its tab becomes active. This action
     * finds the panel anywhere in the layout and selects it.
     *
     * @property panelId The unique identifier of the panel to select
     * @see DockLayoutOperations.selectPanel
     */
    data class SelectPanel(val panelId: String) : DockAction

    /**
     * Reorders tabs within a tab group.
     *
     * Moves a tab from one position to another within the same tab group.
     * This is typically triggered by dragging tabs within the tab bar.
     *
     * @property nodeId The unique identifier of the tab group node
     * @property fromIndex The current zero-based index of the tab
     * @property toIndex The target zero-based index for the tab
     * @see DockLayoutOperations.reorderTabs
     */
    data class ReorderTabs(
        val nodeId: String,
        val fromIndex: Int,
        val toIndex: Int
    ) : DockAction

    /**
     * Replaces the entire layout with a new one.
     *
     * This is useful for loading saved layouts or restoring from a preset.
     * The current layout is completely replaced.
     *
     * @property layout The new layout to apply
     * @see DockStateManager.loadLayout
     */
    data class ApplyLayout(val layout: DockLayout) : DockAction

    /**
     * Resets the layout to the default configuration.
     *
     * Restores the layout to the initial state provided when the dock
     * system was created. All floating windows are closed and any
     * maximized panel is restored.
     *
     * @see DockStateManager.getDefaultLayout
     */
    data object ResetLayout : DockAction

    /**
     * Maximizes or restores a panel.
     *
     * When a panel is maximized, it fills the entire dock container and
     * other panels are hidden. Pass null to restore the previously
     * maximized panel to normal view.
     *
     * @property panelId The ID of the panel to maximize, or null to restore
     */
    data class MaximizePanel(val panelId: String?) : DockAction

    /**
     * Begins dragging a docked panel.
     *
     * Initiates a drag operation for a panel from the docked layout.
     * During the drag, a preview follows the cursor and drop zones are
     * highlighted on hover.
     *
     * @property panelId The unique identifier of the panel being dragged
     * @property x The starting X coordinate (in container coordinates)
     * @property y The starting Y coordinate (in container coordinates)
     * @see UpdateDragPosition
     * @see UpdateDragHover
     * @see EndDrag
     */
    data class StartDrag(
        val panelId: String,
        val x: Float,
        val y: Float
    ) : DockAction

    /**
     * Begins dragging a panel from a floating window.
     *
     * Similar to [StartDrag] but tracks the source floating window so
     * it can be moved or docked as a whole during the drag operation.
     *
     * @property panelId The unique identifier of the panel being dragged
     * @property windowId The unique identifier of the source floating window
     * @property x The starting X coordinate
     * @property y The starting Y coordinate
     * @see StartDrag
     */
    data class StartDragFromFloating(
        val panelId: String,
        val windowId: String,
        val x: Float,
        val y: Float
    ) : DockAction

    /**
     * Updates which docked node and zone are being hovered during a drag.
     *
     * As the user drags a panel over the dock container, this action updates
     * which drop target is currently highlighted. The UI uses this to show
     * visual feedback for where the panel will be dropped.
     *
     * @property nodeId The ID of the node being hovered, or null if none
     * @property zone The zone within the node being hovered, or null if none
     * @see DockZone
     */
    data class UpdateDragHover(
        val nodeId: String?,
        val zone: DockZone?
    ) : DockAction

    /**
     * Updates the current position during a drag operation.
     *
     * Called continuously as the user moves the cursor while dragging.
     * Updates both local container coordinates and screen coordinates
     * (for native floating window positioning).
     *
     * @property x The current X coordinate (in container coordinates)
     * @property y The current Y coordinate (in container coordinates)
     * @property screenX The current X coordinate in screen space (for native windows)
     * @property screenY The current Y coordinate in screen space (for native windows)
     */
    data class UpdateDragPosition(
        val x: Float,
        val y: Float,
        val screenX: Float = x,
        val screenY: Float = y
    ) : DockAction

    /**
     * Updates which floating window is being hovered during a drag.
     *
     * When dragging over floating windows, this tracks which window is
     * the current drop target. Allows panels to be dropped onto floating
     * windows to merge them.
     *
     * @property floatingWindowId The ID of the floating window being hovered, or null
     */
    data class UpdateDragHoverFloating(
        val floatingWindowId: String?
    ) : DockAction

    /**
     * Docks a floating window into another floating window.
     *
     * Merges two floating windows by moving the content from the source
     * window into the target window at the specified zone. The source
     * window is closed after the merge.
     *
     * @property sourceWindowId The ID of the floating window being docked
     * @property targetWindowId The ID of the target floating window
     * @property zone Where to place the source content in the target window
     * @see DockLayoutOperations.dockToFloatingWindow
     */
    data class DockToFloatingWindow(
        val sourceWindowId: String,
        val targetWindowId: String,
        val zone: DockZone
    ) : DockAction

    /**
     * Completes the current drag operation.
     *
     * Finalizes the drag by applying the drop action based on the current
     * hover state. If hovering over a valid drop zone, the panel is moved
     * there. If not hovering over any zone, a new floating window may be
     * created (depending on the drag source).
     *
     * @see StartDrag
     * @see CancelDrag
     */
    data object EndDrag : DockAction

    /**
     * Cancels the current drag operation.
     *
     * Aborts the drag without applying any changes. The dragged panel
     * remains in its original location.
     *
     * @see EndDrag
     */
    data object CancelDrag : DockAction
}