package dev.azora.sdk.docking.presentation.container

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.renderer.DockNodeRenderer
import dev.azora.sdk.docking.presentation.drag.DockDragPreviewWindow
import dev.azora.sdk.docking.presentation.panel.DockPanel

/**
 * Main container composable that renders the entire dock layout.
 *
 * DockContainer is the primary UI entry point for rendering docked panels,
 * floating windows, and drag previews. It translates the [DockLayout] tree
 * structure into Compose UI and handles user interactions by dispatching
 * [DockAction]s.
 *
 * ## Features
 *
 * - Renders the dock node tree (splits, tab groups, panels)
 * - Supports maximized panel mode (single panel fills container)
 * - Renders embedded floating windows (optional)
 * - Shows drag preview during panel drag operations
 * - Displays drop zones for empty containers
 *
 * ## Usage
 *
 * Typically used within a [DockTheme] to provide colors and dimensions:
 *
 * ```kotlin
 * DockTheme(colors = defaultDockColors()) {
 *     DockContainer(
 *         layout = state.layout,
 *         dragState = state.dragState,
 *         maximizedPanelId = state.maximizedPanelId,
 *         onAction = { action -> stateManager.dispatch(action) }
 *     )
 * }
 * ```
 *
 * ## Native Floating Windows
 *
 * For desktop apps using native OS windows for floating panels, set
 * `renderFloatingWindows = false` and use [NativeFloatingWindowHost]
 * separately at the application level.
 *
 * ## Coordinate System
 *
 * The container tracks its position and converts between Compose coordinates
 * (density-independent) and screen coordinates (physical pixels) for accurate
 * drag-and-drop across native windows on HiDPI displays.
 *
 * @param layout The dock layout to render
 * @param dragState Current drag operation state, or null if not dragging
 * @param maximizedPanelId ID of the maximized panel, or null for normal view
 * @param onAction Callback to dispatch dock actions
 * @param modifier Modifier for the container
 * @param renderFloatingWindows Whether to render embedded floating windows (default true)
 * @param screenOffsetX Container's X offset in screen pixels (for coordinate conversion)
 * @param screenOffsetY Container's Y offset in screen pixels (for coordinate conversion)
 *
 * @see DockLayout for the layout structure
 * @see DockAction for available actions
 * @see DockTheme for theming
 * @see NativeFloatingWindowHost for native floating windows on desktop
 */
@Composable
fun DockContainer(
    layout: DockLayout,
    dragState: DragState?,
    maximizedPanelId: String?,
    onAction: (DockAction) -> Unit,
    modifier: Modifier = Modifier,
    renderFloatingWindows: Boolean = true,
    /** Screen offset for converting local coordinates to screen coordinates (in screen pixels) */
    screenOffsetX: Float = 0f,
    screenOffsetY: Float = 0f
) {
    var containerPosition by remember { mutableStateOf(Offset.Zero) }

    // Get density for coordinate conversion
    // On macOS Retina: density is typically 2.0
    // Compose coordinates are in density-independent pixels (dp)
    // Screen coordinates from AWT are in physical pixels
    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerPosition = coordinates.positionInRoot()
            }
    ) {
        // Render main dock layout
        layout.rootNode?.let { rootNode ->
            // If a panel is maximized, only show that panel
            if (maximizedPanelId != null) {
                val descriptor = layout.panelDescriptors[maximizedPanelId]
                descriptor?.let {
                    DockPanel(
                        panels = listOf(it),
                        activeIndex = 0,
                        isDragging = false,
                        isDragSource = false,
                        dragPosition = null,
                        onTabSelect = { },
                        onTabClose = { panelId -> onAction(DockAction.RemovePanel(panelId)) },
                        onTabDragStart = { _, _ -> },
                        onTabDrag = { _, _ -> },
                        onTabDragEnd = { },
                        onPanelClose = { panelId -> onAction(DockAction.RemovePanel(panelId)) },
                        onPanelEnter = { },
                        onZoneHover = { _ -> }
                    )
                }
            } else {
                DockNodeRenderer(
                    node = rootNode,
                    panelDescriptors = layout.panelDescriptors,
                    dragState = dragState,
                    maximizedPanelId = maximizedPanelId,
                    onSplitResize = { nodeId, ratio ->
                        onAction(DockAction.SetSplitRatio(nodeId, ratio))
                    },
                    onTabSelect = { nodeId, index ->
                        onAction(DockAction.SelectTab(nodeId, index))
                    },
                    onTabClose = { panelId ->
                        onAction(DockAction.RemovePanel(panelId))
                    },
                    onTabReorder = { nodeId, fromIndex, toIndex ->
                        onAction(DockAction.ReorderTabs(nodeId, fromIndex, toIndex))
                    },
                    onTabDragStart = { panelId, offset ->
                        onAction(DockAction.StartDrag(panelId, offset.x, offset.y))
                    },
                    onTabDrag = { _, offset ->
                        // Convert Compose coordinates (scaled pixels) to screen coordinates (points)
                        // On macOS Retina, Compose uses 2x coordinates internally,
                        // while AWT screen coordinates are in points (logical pixels)
                        val screenX = (offset.x / density) + screenOffsetX
                        val screenY = (offset.y / density) + screenOffsetY
                        onAction(
                            DockAction.UpdateDragPosition(
                                x = offset.x,
                                y = offset.y,
                                screenX = screenX,
                                screenY = screenY
                            )
                        )
                    },
                    onTabDragEnd = { _ ->
                        onAction(DockAction.EndDrag)
                    },
                    onTabDragCancel = { _ ->
                        onAction(DockAction.CancelDrag)
                    },
                    onPanelClose = { panelId ->
                        onAction(DockAction.RemovePanel(panelId))
                    },
                    onPanelFloat = { panelId ->
                        onAction(
                            DockAction.FloatPanel(
                                panelId,
                                containerPosition.x + 50,
                                containerPosition.y + 50
                            )
                        )
                    },
                    onPanelMaximize = { panelId ->
                        onAction(DockAction.MaximizePanel(panelId))
                    },
                    onPanelRestore = {
                        onAction(DockAction.MaximizePanel(null))
                    },
                    onPanelEnter = { nodeId ->
                        // When mouse enters a panel during drag, set it as hover target
                        onAction(DockAction.UpdateDragHover(nodeId, null))
                    },
                    onPanelExit = { _ ->
                        // When mouse exits a panel, clear hover target
                        onAction(DockAction.UpdateDragHover(null, null))
                    },
                    onZoneHover = { nodeId, zone ->
                        onAction(DockAction.UpdateDragHover(nodeId, zone))
                    }
                )
            }
        }

        // Show empty container drop zone when there's no root node and we're dragging
        if (layout.rootNode == null && dragState != null) {
            EmptyContainerDropZone(
                dragPosition = Offset(
                    dragState.currentX - containerPosition.x,
                    dragState.currentY - containerPosition.y
                ),
                onZoneHover = { zone ->
                    onAction(DockAction.UpdateDragHover("__root__", zone))
                }
            )
        }

        // Render floating windows with proper keys (only when not using native windows)
        if (renderFloatingWindows) {
            layout.floatingWindows.forEach { window ->
                key(window.id) {
                    FloatingWindowItem(
                        window = window,
                        layout = layout,
                        onAction = onAction
                    )
                }
            }
        }

        // Show floating preview when dragging from docked panel
        if (dragState != null && !dragState.isFromFloating) {
            val descriptor = layout.panelDescriptors[dragState.panelId]
            DockDragPreviewWindow(
                descriptor = descriptor,
                x = dragState.currentX - containerPosition.x,
                y = dragState.currentY - containerPosition.y
            )
        }
    }
}