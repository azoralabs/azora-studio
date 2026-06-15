package dev.azora.sdk.docking.data

import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.domain.DockDefaults.FLOATING_WINDOW_HEIGHT
import dev.azora.sdk.docking.domain.DockDefaults.FLOATING_WINDOW_WIDTH
import dev.azora.sdk.docking.domain.DockDefaults.SPLIT_RATIO_PRIMARY
import dev.azora.sdk.docking.domain.DockDefaults.SPLIT_RATIO_SECONDARY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Default implementation of [DockStateManager].
 *
 * This class manages the complete state of the docking system, including
 * the layout, drag operations, and panel focus. It processes [DockAction]s
 * and updates the state accordingly.
 *
 * ## Usage
 *
 * Create an instance with an optional initial layout:
 *
 * ```kotlin
 * val stateManager = DockStateManagerImpl(initialLayout = myLayout)
 *
 * // Observe state changes
 * stateManager.state.collect { state ->
 *     updateUI(state)
 * }
 *
 * // Dispatch actions
 * stateManager.dispatch(DockAction.AddPanel(descriptor, targetNodeId, zone))
 * ```
 *
 * ## State Management
 *
 * State is managed using Kotlin's [StateFlow]. All state updates are atomic
 * and thread-safe. The [dispatch] method processes actions synchronously,
 * making state changes predictable.
 *
 * ## Action Processing
 *
 * Actions are processed in [processAction], which delegates to
 * [DockLayoutOperations] for layout modifications. The implementation handles:
 *
 * - **Panel operations**: Add, remove, move, float, select panels
 * - **Floating window operations**: Move, resize, dock, close, toggle states
 * - **Layout operations**: Set split ratios, select/reorder tabs
 * - **Drag operations**: Complex drag-and-drop with multi-panel floating windows
 *
 * ## Drag-and-Drop
 *
 * The drag implementation supports sophisticated scenarios:
 * - Dragging docked panels to create floating windows
 * - Dragging floating windows to dock them
 * - Extracting single tabs from multi-panel floating windows
 * - Merging floating windows together
 *
 * ## ID Generation
 *
 * Unique IDs for nodes and windows are generated using UUIDs with a "node_"
 * or "window_" prefix for debugging clarity.
 *
 * @param initialLayout The initial dock layout. Defaults to an empty layout.
 *
 * @see DockStateManager for the interface contract
 * @see DockState for the state structure
 * @see DockAction for available actions
 * @see DockLayoutOperations for pure layout operations
 */
class DockStateManagerImpl(
    initialLayout: DockLayout = DockLayout(rootNode = null)
) : DockStateManager {

    /** Internal mutable state flow. */
    private val _state = MutableStateFlow(DockState(layout = initialLayout))

    /**
     * The current state of the docking system.
     *
     * Collect this flow to observe state changes and update the UI accordingly.
     * State updates are atomic and always reflect a consistent snapshot.
     */
    override val state: StateFlow<DockState> = _state.asStateFlow()

    /**
     * Creates a split node for the given zone.
     *
     * This helper consolidates the logic for creating splits when dropping
     * a panel on LEFT/RIGHT/TOP/BOTTOM zones.
     *
     * @param zone The target drop zone
     * @param panelId The ID of the panel being dropped
     * @param existingNode The node currently at the drop location
     * @return A new split node containing both the new panel and existing content
     */
    private fun createZoneSplit(
        zone: DockZone,
        panelId: String,
        existingNode: DockNode
    ): DockNode.Split {
        val newNode = DockNode.Leaf(id = generateNodeId(), panelId = panelId)
        val isHorizontal = zone == DockZone.LEFT || zone == DockZone.RIGHT
        val newNodeFirst = zone == DockZone.LEFT || zone == DockZone.TOP
        return DockNode.Split(
            id = generateNodeId(),
            orientation = if (isHorizontal) DockOrientation.HORIZONTAL else DockOrientation.VERTICAL,
            first = if (newNodeFirst) newNode else existingNode,
            second = if (newNodeFirst) existingNode else newNode,
            ratio = if (newNodeFirst) SPLIT_RATIO_PRIMARY else SPLIT_RATIO_SECONDARY
        )
    }

    /**
     * Dispatches an action to modify the dock state.
     *
     * This is the primary entry point for all state modifications. Actions are
     * processed synchronously, and the resulting state is immediately available
     * via the [state] flow.
     *
     * @param action The action to process
     * @see DockAction for available actions
     */
    override fun dispatch(action: DockAction) {
        _state.update { currentState -> processAction(currentState, action) }
    }

    /**
     * Processes a single action and returns the resulting state.
     *
     * This method is the core action processor. It handles all action types,
     * delegating to [DockLayoutOperations] for layout modifications and
     * managing transient state (drag, focus, maximize) directly.
     *
     * ## Drag Handling
     *
     * The most complex logic handles [DockAction.EndDrag], which must determine:
     * 1. The drop target type (docked panel, floating window, or empty space)
     * 2. Whether the source is a single-panel or multi-panel floating window
     * 3. Whether to extract a single tab or move the entire window
     *
     * @param state The current state
     * @param action The action to process
     * @return The new state after processing the action
     */
    private fun processAction(state: DockState, action: DockAction): DockState {
        return when (action) {
            is DockAction.AddPanel -> {
                val newLayout = DockLayoutOperations.addPanel(
                    state.layout, action.descriptor, action.targetNodeId, action.zone, ::generateNodeId
                )
                state.copy(layout = newLayout, focusedPanelId = action.descriptor.id)
            }
            is DockAction.RemovePanel -> {
                val newLayout = DockLayoutOperations.removePanel(state.layout, action.panelId)
                state.copy(
                    layout = newLayout,
                    focusedPanelId = if (state.focusedPanelId == action.panelId) null else state.focusedPanelId,
                    maximizedPanelId = if (state.maximizedPanelId == action.panelId) null else state.maximizedPanelId
                )
            }
            is DockAction.MovePanel -> {
                val newLayout = DockLayoutOperations.movePanel(
                    state.layout, action.panelId, action.targetNodeId, action.zone, ::generateNodeId
                )
                state.copy(layout = newLayout)
            }
            is DockAction.FloatPanel -> {
                val newLayout = DockLayoutOperations.floatPanel(
                    state.layout, action.panelId, action.x, action.y, action.width, action.height, ::generateNodeId
                )
                state.copy(layout = newLayout)
            }
            is DockAction.DockFloatingWindow -> {
                val newLayout = DockLayoutOperations.dockFloatingWindow(
                    state.layout, action.windowId, action.targetNodeId, action.zone, ::generateNodeId
                )
                state.copy(layout = newLayout)
            }
            is DockAction.MoveFloatingWindow -> {
                val newLayout = DockLayoutOperations.moveFloatingWindow(state.layout, action.windowId, action.x, action.y)
                state.copy(layout = newLayout)
            }
            is DockAction.ResizeFloatingWindow -> {
                val newLayout = DockLayoutOperations.resizeFloatingWindow(state.layout, action.windowId, action.width, action.height)
                state.copy(layout = newLayout)
            }
            is DockAction.CloseFloatingWindow -> {
                val newLayout = DockLayoutOperations.closeFloatingWindow(state.layout, action.windowId)
                state.copy(layout = newLayout)
            }
            is DockAction.ToggleFloatingWindowMinimized -> {
                val newLayout = DockLayoutOperations.toggleFloatingWindowMinimized(state.layout, action.windowId)
                state.copy(layout = newLayout)
            }
            is DockAction.ToggleFloatingWindowMaximized -> {
                val newLayout = DockLayoutOperations.toggleFloatingWindowMaximized(state.layout, action.windowId)
                state.copy(layout = newLayout)
            }
            is DockAction.ToggleFloatingWindowFullscreen -> {
                val newLayout = DockLayoutOperations.toggleFloatingWindowFullscreen(state.layout, action.windowId)
                state.copy(layout = newLayout)
            }
            is DockAction.SetSplitRatio -> {
                val newLayout = DockLayoutOperations.setSplitRatio(state.layout, action.nodeId, action.ratio)
                state.copy(layout = newLayout)
            }
            is DockAction.SelectTab -> {
                val newLayout = DockLayoutOperations.selectTab(state.layout, action.nodeId, action.tabIndex)
                state.copy(layout = newLayout)
            }
            is DockAction.SelectPanel -> {
                val newLayout = DockLayoutOperations.selectPanel(state.layout, action.panelId)
                state.copy(layout = newLayout, focusedPanelId = action.panelId)
            }
            is DockAction.ReorderTabs -> {
                val newLayout = DockLayoutOperations.reorderTabs(state.layout, action.nodeId, action.fromIndex, action.toIndex)
                state.copy(layout = newLayout)
            }
            is DockAction.ApplyLayout -> state.copy(layout = action.layout, maximizedPanelId = null, dragState = null)
            is DockAction.ResetLayout -> state.copy(layout = getDefaultLayout(), maximizedPanelId = null, dragState = null)
            is DockAction.MaximizePanel -> state.copy(maximizedPanelId = action.panelId)
            is DockAction.StartDrag -> {
                // Don't float immediately - keep panel in place during drag
                // A floating preview will be shown in the UI
                state.copy(
                    dragState = DragState(
                        panelId = action.panelId,
                        sourceNodeId = "",
                        currentX = action.x,
                        currentY = action.y,
                        startX = action.x,
                        startY = action.y,
                        isFromFloating = false,
                        sourceWindowId = null
                    )
                )
            }
            is DockAction.StartDragFromFloating -> state.copy(
                dragState = DragState(
                    panelId = action.panelId,
                    sourceNodeId = "",
                    currentX = action.x,
                    currentY = action.y,
                    startX = action.x,
                    startY = action.y,
                    isFromFloating = true,
                    sourceWindowId = action.windowId
                )
            )
            is DockAction.UpdateDragHover -> state.dragState?.let { drag ->
                state.copy(
                    dragState = drag.copy(
                        hoveredNodeId = action.nodeId,
                        hoveredZone = action.zone
                    )
                )
            } ?: state
            is DockAction.UpdateDragPosition -> state.dragState?.let { drag ->
                // Calculate the delta from previous position
                val deltaX = action.x - drag.currentX
                val deltaY = action.y - drag.currentY

                // Update drag state with both local and screen coordinates
                var newState = state.copy(
                    dragState = drag.copy(
                        currentX = action.x,
                        currentY = action.y,
                        screenX = action.screenX,
                        screenY = action.screenY
                    )
                )

                // If dragging from a floating window, only move it if it's a single-panel window
                if (drag.isFromFloating) {
                    drag.sourceWindowId?.let { windowId ->
                        val window = state.layout.floatingWindows.find { it.id == windowId }
                        if (window != null) {
                            val panelCount = window.content.collectPanelIds().size
                            // Only move the window if it has a single panel
                            if (panelCount == 1) {
                                val newLayout = DockLayoutOperations.moveFloatingWindow(
                                    newState.layout, windowId, window.x + deltaX, window.y + deltaY
                                )
                                newState = newState.copy(layout = newLayout)
                            }
                        }
                    }
                }

                newState
            } ?: state
            is DockAction.UpdateDragHoverFloating -> state.dragState?.let { drag ->
                state.copy(
                    dragState = drag.copy(
                        hoveredFloatingWindowId = action.floatingWindowId
                    )
                )
            } ?: state
            is DockAction.DockToFloatingWindow -> {
                val newLayout = DockLayoutOperations.dockToFloatingWindow(
                    state.layout, action.sourceWindowId, action.targetWindowId, action.zone, ::generateNodeId
                )
                state.copy(layout = newLayout)
            }
            is DockAction.EndDrag -> {
                val dragState = state.dragState ?: return state.copy(dragState = null)
                val hoveredNodeId = dragState.hoveredNodeId
                val hoveredZone = dragState.hoveredZone
                val sourceWindowId = dragState.sourceWindowId
                val hoveredFloatingWindowId = dragState.hoveredFloatingWindowId

                // Helper to check panel count in source floating window
                val sourceWindow = sourceWindowId?.let { id -> state.layout.floatingWindows.find { it.id == id } }
                val sourcePanelCount = sourceWindow?.content?.collectPanelIds()?.size ?: 0

                when {
                    // Drop onto another floating window
                    hoveredFloatingWindowId != null && hoveredZone != null -> {
                        if (dragState.isFromFloating && sourceWindowId != null) {
                            if (sourcePanelCount > 1) {
                                // Extract single panel and add to target floating window
                                val newSourceContent = sourceWindow!!.content.removePanel(dragState.panelId)
                                if (newSourceContent != null) {
                                    val targetWindow = state.layout.floatingWindows.find { it.id == hoveredFloatingWindowId }
                                    if (targetWindow != null) {
                                        // Update source window
                                        var updatedWindows = state.layout.floatingWindows.map { w ->
                                            if (w.id == sourceWindowId) w.copy(content = newSourceContent) else w
                                        }
                                        // Add panel to target window
                                        val newTargetContent = when (hoveredZone) {
                                            DockZone.CENTER -> {
                                                val targetPanels = targetWindow.content.collectPanelIds().toList()
                                                DockNode.TabGroup(
                                                    id = generateNodeId(),
                                                    panels = targetPanels + dragState.panelId,
                                                    activeTabIndex = targetPanels.size
                                                )
                                            }
                                            else -> createZoneSplit(
                                                zone = hoveredZone,
                                                panelId = dragState.panelId,
                                                existingNode = targetWindow.content
                                            )
                                        }
                                        updatedWindows = updatedWindows.map { w ->
                                            if (w.id == hoveredFloatingWindowId) w.copy(content = newTargetContent) else w
                                        }
                                        state.copy(layout = state.layout.copy(floatingWindows = updatedWindows), dragState = null)
                                    } else state.copy(dragState = null)
                                } else {
                                    // Would be empty - merge whole window
                                    val newLayout = DockLayoutOperations.dockToFloatingWindow(
                                        state.layout, sourceWindowId, hoveredFloatingWindowId, hoveredZone, ::generateNodeId
                                    )
                                    state.copy(layout = newLayout, dragState = null)
                                }
                            } else {
                                // Single panel - merge whole window
                                val newLayout = DockLayoutOperations.dockToFloatingWindow(
                                    state.layout, sourceWindowId, hoveredFloatingWindowId, hoveredZone, ::generateNodeId
                                )
                                state.copy(layout = newLayout, dragState = null)
                            }
                        } else {
                            // From docked panel - add to floating window
                            val newLayout = DockLayoutOperations.addPanelToFloatingWindow(
                                state.layout, dragState.panelId, hoveredFloatingWindowId, hoveredZone, ::generateNodeId
                            )
                            state.copy(layout = newLayout, dragState = null)
                        }
                    }
                    // Drop on main dock area
                    hoveredNodeId != null && hoveredZone != null -> {
                        if (dragState.isFromFloating && sourceWindowId != null) {
                            if (sourcePanelCount > 1) {
                                // Extract single panel and dock it
                                val newSourceContent = sourceWindow!!.content.removePanel(dragState.panelId)
                                if (newSourceContent != null) {
                                    val updatedWindows = state.layout.floatingWindows.map { w ->
                                        if (w.id == sourceWindowId) w.copy(content = newSourceContent) else w
                                    }
                                    val descriptor = state.layout.panelDescriptors[dragState.panelId]
                                    if (descriptor != null) {
                                        val layoutWithUpdatedFloating = state.layout.copy(floatingWindows = updatedWindows)
                                        val newLayout = DockLayoutOperations.addPanel(
                                            layoutWithUpdatedFloating, descriptor, hoveredNodeId, hoveredZone, ::generateNodeId
                                        )
                                        state.copy(layout = newLayout, dragState = null)
                                    } else state.copy(dragState = null)
                                } else {
                                    // Would be empty - dock whole window
                                    val newLayout = DockLayoutOperations.dockFloatingWindow(
                                        state.layout, sourceWindowId, hoveredNodeId, hoveredZone, ::generateNodeId
                                    )
                                    state.copy(layout = newLayout, dragState = null)
                                }
                            } else {
                                // Single panel - dock whole window
                                val newLayout = DockLayoutOperations.dockFloatingWindow(
                                    state.layout, sourceWindowId, hoveredNodeId, hoveredZone, ::generateNodeId
                                )
                                state.copy(layout = newLayout, dragState = null)
                            }
                        } else {
                            // Move the panel within docked layout
                            val newLayout = DockLayoutOperations.movePanel(
                                state.layout, dragState.panelId, hoveredNodeId, hoveredZone, ::generateNodeId
                            )
                            state.copy(layout = newLayout, dragState = null)
                        }
                    }
                    // No valid drop target - from docked panel, create floating window
                    !dragState.isFromFloating -> {
                        val newLayout = DockLayoutOperations.floatPanel(
                            state.layout, dragState.panelId, dragState.screenX, dragState.screenY,
                            FLOATING_WINDOW_WIDTH, FLOATING_WINDOW_HEIGHT, ::generateNodeId
                        )
                        state.copy(layout = newLayout, dragState = null)
                    }
                    // No valid drop target - from multi-panel floating window, extract tab to new floating window
                    dragState.isFromFloating && sourceWindowId != null && sourcePanelCount > 1 -> {
                        val newSourceContent = sourceWindow!!.content.removePanel(dragState.panelId)
                        if (newSourceContent != null) {
                            val updatedWindows = state.layout.floatingWindows.map { w ->
                                if (w.id == sourceWindowId) w.copy(content = newSourceContent) else w
                            }
                            val newFloatingWindow = FloatingWindow(
                                id = generateWindowId(),
                                content = DockNode.Leaf(id = generateNodeId(), panelId = dragState.panelId),
                                x = dragState.screenX,
                                y = dragState.screenY,
                                width = FLOATING_WINDOW_WIDTH,
                                height = FLOATING_WINDOW_HEIGHT
                            )
                            state.copy(layout = state.layout.copy(floatingWindows = updatedWindows + newFloatingWindow), dragState = null)
                        } else state.copy(dragState = null)
                    }
                    // Single panel floating window with no target - just clear drag (window already moved)
                    else -> state.copy(dragState = null)
                }
            }
            is DockAction.CancelDrag -> {
                // Cancel drag without any layout modifications - just clear the drag state
                state.copy(dragState = null)
            }
        }
    }

    /**
     * Registers a panel descriptor with the docking system.
     *
     * The descriptor is added to the layout's panel descriptors map, making
     * it available for use in the dock. This does not add the panel to the
     * visible layout - use [DockAction.AddPanel] for that.
     *
     * @param descriptor The panel descriptor to register
     */
    override fun registerPanel(descriptor: DockPanelDescriptor) {
        _state.update { currentState ->
            currentState.copy(
                layout = currentState.layout.copy(
                    panelDescriptors = currentState.layout.panelDescriptors + (descriptor.id to descriptor)
                )
            )
        }
    }

    /**
     * Unregisters a panel from the docking system.
     *
     * This removes the panel from the layout entirely by dispatching a
     * [DockAction.RemovePanel] action.
     *
     * @param panelId The ID of the panel to unregister
     */
    override fun unregisterPanel(panelId: String) {
        dispatch(DockAction.RemovePanel(panelId))
    }

    /**
     * Starts a drag operation for a panel.
     *
     * This method provides direct control over drag state, useful for custom
     * drag implementations. For standard drag-and-drop, prefer dispatching
     * [DockAction.StartDrag] or [DockAction.StartDragFromFloating].
     *
     * @param panelId The ID of the panel being dragged
     * @param sourceNodeId The ID of the source node
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param isFromFloating Whether the drag started from a floating window
     * @param sourceWindowId The source floating window ID (if applicable)
     */
    override fun startDrag(
        panelId: String,
        sourceNodeId: String,
        startX: Float,
        startY: Float,
        isFromFloating: Boolean,
        sourceWindowId: String?
    ) {
        _state.update { currentState ->
            currentState.copy(
                dragState = DragState(
                    panelId = panelId,
                    sourceNodeId = sourceNodeId,
                    currentX = startX, currentY = startY,
                    startX = startX, startY = startY,
                    isFromFloating = isFromFloating,
                    sourceWindowId = sourceWindowId
                )
            )
        }
    }

    /**
     * Updates the current drag position and hover state.
     *
     * Call this method continuously during drag to update the cursor position
     * and the currently hovered drop target. This enables visual feedback
     * for drop zone highlighting.
     *
     * @param currentX Current X coordinate
     * @param currentY Current Y coordinate
     * @param hoveredNodeId The node being hovered, or null
     * @param hoveredZone The zone within the hovered node, or null
     */
    override fun updateDrag(currentX: Float, currentY: Float, hoveredNodeId: String?, hoveredZone: DockZone?) {
        _state.update { currentState ->
            currentState.dragState?.let { drag ->
                currentState.copy(
                    dragState = drag.copy(
                        currentX = currentX, currentY = currentY,
                        hoveredNodeId = hoveredNodeId, hoveredZone = hoveredZone
                    )
                )
            } ?: currentState
        }
    }

    /**
     * Ends the current drag operation.
     *
     * If not cancelled and a valid drop target is hovered, moves the panel
     * to the target location. Otherwise, clears the drag state without
     * modifying the layout.
     *
     * Note: This simplified implementation only handles basic panel moves.
     * For full drag-and-drop support including floating windows, use
     * [DockAction.EndDrag] via [dispatch] instead.
     *
     * @param cancelled If true, cancels the drag without applying changes
     */
    override fun endDrag(cancelled: Boolean) {
        val currentState = _state.value
        val dragState = currentState.dragState ?: return

        val hoveredNodeId = dragState.hoveredNodeId
        val hoveredZone = dragState.hoveredZone

        if (!cancelled && hoveredNodeId != null && hoveredZone != null) {
            dispatch(DockAction.MovePanel(dragState.panelId, hoveredNodeId, hoveredZone))
        }
        _state.update { it.copy(dragState = null) }
    }

    /**
     * Returns the default (empty) layout.
     *
     * @return A new empty [DockLayout] with no root node
     */
    override fun getDefaultLayout(): DockLayout = DockLayout(rootNode = null)

    /**
     * Creates a named copy of the current layout for saving.
     *
     * The returned layout can be serialized and persisted. Load it later
     * using [loadLayout] to restore the dock state.
     *
     * @param name A name to identify this saved layout
     * @return A copy of the current layout with the specified name
     */
    override fun saveLayout(name: String): DockLayout = _state.value.layout.copy(name = name)

    /**
     * Loads a saved layout, replacing the current layout.
     *
     * Dispatches [DockAction.ApplyLayout] to replace the current layout.
     * This also clears any maximized panel and active drag state.
     *
     * @param layout The layout to load
     */
    override fun loadLayout(layout: DockLayout) {
        dispatch(DockAction.ApplyLayout(layout))
    }

    /**
     * Generates a unique ID for a new dock node.
     *
     * IDs are prefixed with "node_" followed by 8 characters from a UUID,
     * providing both uniqueness and debuggability.
     *
     * @return A unique node ID (e.g., "node_a1b2c3d4")
     */
    @OptIn(ExperimentalUuidApi::class)
    override fun generateNodeId(): String = "node_${Uuid.random().toString().take(8)}"

    /**
     * Generates a unique ID for a new floating window.
     *
     * IDs are prefixed with "window_" followed by 8 characters from a UUID,
     * providing both uniqueness and debuggability.
     *
     * @return A unique window ID (e.g., "window_e5f6g7h8")
     */
    @OptIn(ExperimentalUuidApi::class)
    override fun generateWindowId(): String = "window_${Uuid.random().toString().take(8)}"
}