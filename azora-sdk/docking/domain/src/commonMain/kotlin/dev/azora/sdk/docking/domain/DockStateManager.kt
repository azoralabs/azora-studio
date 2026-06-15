package dev.azora.sdk.docking.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing dock state.
 *
 * The DockStateManager is the central controller for the docking system.
 * It manages the current layout, handles drag operations, and processes
 * all actions that modify the dock state.
 *
 * ## Usage
 *
 * The primary way to interact with the dock system is through [dispatch]:
 *
 * ```kotlin
 * // Add a panel
 * stateManager.dispatch(DockAction.AddPanel(descriptor, targetNodeId, zone))
 *
 * // Remove a panel
 * stateManager.dispatch(DockAction.RemovePanel(panelId))
 *
 * // Float a panel
 * stateManager.dispatch(DockAction.FloatPanel(panelId, x, y))
 * ```
 *
 * ## State Observation
 *
 * Observe state changes using the [state] flow:
 *
 * ```kotlin
 * stateManager.state.collect { state ->
 *     // React to layout changes
 *     updateUI(state.layout)
 * }
 * ```
 *
 * ## Implementation
 *
 * Use [DockStateManagerImpl] as the default implementation:
 *
 * ```kotlin
 * val stateManager: DockStateManager = DockStateManagerImpl(initialLayout)
 * ```
 *
 * @see DockState for the complete state structure
 * @see DockAction for available actions
 * @see DockStateManagerImpl for the default implementation
 */
interface DockStateManager {

    /** The current state of the docking system as a flow. */
    val state: StateFlow<DockState>

    /**
     * Dispatches an action to modify the dock state.
     *
     * This is the primary method for interacting with the docking system.
     * All state changes should go through this method.
     *
     * @param action The action to dispatch
     * @see DockAction for available actions
     */
    fun dispatch(action: DockAction)

    /**
     * Registers a panel descriptor with the docking system.
     *
     * This makes the panel available for adding to the layout.
     * Call this before adding a panel to the dock.
     *
     * @param descriptor The panel descriptor to register
     */
    fun registerPanel(descriptor: DockPanelDescriptor)

    /**
     * Unregisters a panel from the docking system.
     *
     * This removes the panel from the layout and its descriptor.
     *
     * @param panelId The ID of the panel to unregister
     */
    fun unregisterPanel(panelId: String)

    /**
     * Starts a drag operation for a panel.
     *
     * This is typically called by the UI when the user starts dragging a tab or header.
     *
     * @param panelId The ID of the panel being dragged
     * @param sourceNodeId The ID of the node the panel is being dragged from
     * @param startX The starting X coordinate
     * @param startY The starting Y coordinate
     * @param isFromFloating Whether the drag started from a floating window
     * @param sourceWindowId The ID of the source floating window (if applicable)
     */
    fun startDrag(
        panelId: String,
        sourceNodeId: String,
        startX: Float,
        startY: Float,
        isFromFloating: Boolean = false,
        sourceWindowId: String? = null
    )

    /**
     * Updates the current drag position and hover state.
     *
     * @param currentX The current X coordinate
     * @param currentY The current Y coordinate
     * @param hoveredNodeId The ID of the node being hovered over
     * @param hoveredZone The zone within the hovered node
     */
    fun updateDrag(
        currentX: Float,
        currentY: Float,
        hoveredNodeId: String? = null,
        hoveredZone: DockZone? = null
    )

    /**
     * Ends the current drag operation.
     *
     * @param cancelled If true, the drag is cancelled without applying changes
     */
    fun endDrag(cancelled: Boolean = false)

    /**
     * Returns the default layout configuration.
     *
     * @return A new default layout
     */
    fun getDefaultLayout(): DockLayout

    /**
     * Creates a named copy of the current layout for saving.
     *
     * @param name The name for the saved layout
     * @return A copy of the current layout with the specified name
     */
    fun saveLayout(name: String): DockLayout

    /**
     * Loads a saved layout, replacing the current layout.
     *
     * @param layout The layout to load
     */
    fun loadLayout(layout: DockLayout)

    /**
     * Generates a unique ID for a new dock node.
     *
     * @return A unique node ID
     */
    fun generateNodeId(): String

    /**
     * Generates a unique ID for a new floating window.
     *
     * @return A unique window ID
     */
    fun generateWindowId(): String
}