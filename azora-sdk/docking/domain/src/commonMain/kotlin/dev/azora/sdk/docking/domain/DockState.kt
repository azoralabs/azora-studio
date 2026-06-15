package dev.azora.sdk.docking.domain

/**
 * Complete state of the docking system.
 *
 * This class represents the entire state needed to render and interact with
 * the dock UI. It combines the persistent layout with transient UI state
 * like drag operations and maximized panels.
 *
 * ## State Flow
 *
 * State is managed by [DockStateManager] and observed via its [state][DockStateManager.state] flow:
 *
 * ```kotlin
 * val stateManager: DockStateManager = DockStateManagerImpl(initialLayout)
 *
 * // Observe state changes
 * stateManager.state.collect { state ->
 *     // Render the dock with current state
 *     renderDock(state.layout, state.dragState, state.maximizedPanelId)
 * }
 * ```
 *
 * ## State Components
 *
 * - **layout**: The persistent dock configuration (panels, splits, floating windows)
 * - **dragState**: Transient state during drag-and-drop operations
 * - **maximizedPanelId**: Currently maximized panel (fills entire dock area)
 * - **focusedPanelId**: Currently focused panel for keyboard navigation
 *
 * @property layout The current dock layout configuration. This is the persistent
 *                  state that should be saved/restored.
 * @property dragState Active drag operation state, or null if not dragging.
 *                     Used to render drag previews and drop zone highlights.
 * @property maximizedPanelId ID of the maximized panel, or null if none.
 *                            When set, only this panel is visible.
 * @property focusedPanelId ID of the focused panel, or null if none.
 *                          Used for keyboard navigation and visual focus indicators.
 *
 * @see DockStateManager
 * @see DockLayout
 * @see DragState
 */
data class DockState(
    val layout: DockLayout = DockLayout(rootNode = null),
    val dragState: DragState? = null,
    val maximizedPanelId: String? = null,
    val focusedPanelId: String? = null
)

/**
 * State of an ongoing drag operation.
 *
 * Tracks all information needed during a panel drag-and-drop operation,
 * including the source, current position, and potential drop target.
 *
 * ## Drag Lifecycle
 *
 * 1. **Start**: Created when user begins dragging a panel tab or header
 * 2. **Update**: Position and hover target updated as user moves cursor
 * 3. **End**: Drag completes with drop action or cancellation
 *
 * ## Coordinate Systems
 *
 * The drag state tracks positions in two coordinate systems:
 * - **Container coordinates** ([currentX], [currentY]): Relative to the dock container
 * - **Screen coordinates** ([screenX], [screenY]): Absolute screen position for native windows
 *
 * ## Drop Targets
 *
 * During drag, the UI updates hover state to indicate valid drop targets:
 * - **Docked panels**: [hoveredNodeId] and [hoveredZone] for main dock area
 * - **Floating windows**: [hoveredFloatingWindowId] for floating window targets
 *
 * @property panelId The ID of the panel being dragged
 * @property sourceNodeId The ID of the node the panel was dragged from
 * @property currentX Current X position in container coordinates
 * @property currentY Current Y position in container coordinates
 * @property startX Starting X position when drag began
 * @property startY Starting Y position when drag began
 * @property hoveredNodeId ID of the docked node being hovered, or null
 * @property hoveredZone The zone within the hovered node (LEFT, RIGHT, etc.)
 * @property isFromFloating True if dragging from a floating window
 * @property sourceWindowId ID of the source floating window (if [isFromFloating])
 * @property screenX Current X position in screen coordinates (for native windows)
 * @property screenY Current Y position in screen coordinates (for native windows)
 * @property hoveredFloatingWindowId ID of floating window being hovered for drop
 *
 * @see DockAction.StartDrag
 * @see DockAction.UpdateDragPosition
 * @see DockAction.EndDrag
 */
data class DragState(
    val panelId: String,
    val sourceNodeId: String,
    val currentX: Float,
    val currentY: Float,
    val startX: Float,
    val startY: Float,
    val hoveredNodeId: String? = null,
    val hoveredZone: DockZone? = null,
    val isFromFloating: Boolean = false,
    val sourceWindowId: String? = null,
    val screenX: Float = currentX,
    val screenY: Float = currentY,
    val hoveredFloatingWindowId: String? = null
)