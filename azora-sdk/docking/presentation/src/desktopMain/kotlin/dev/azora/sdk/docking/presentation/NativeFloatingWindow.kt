package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.panel.DockPanelRegistry
import dev.azora.sdk.docking.presentation.panel.LocalDockPanelRegistry
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions
import dev.azora.sdk.core.theme.palette.AzoraPalette
import dev.azora.sdk.core.theme.palette.azoraLightPalette
import java.awt.event.*

private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

// macOS title bar safe area height (traffic lights area)
private const val MACOS_TITLE_BAR_HEIGHT = 28

/**
 * A native OS window containing dock panel content.
 *
 * This composable creates an undecorated native window with a custom title bar,
 * optional tab bar, and panel content. It handles:
 *
 * - Window state synchronization (position, size, maximize, fullscreen)
 * - Custom title bar with window controls (minimize, maximize, close, dock)
 * - Tab bar for multi-panel windows
 * - Drag-and-drop support for tabs and window docking
 * - Drop zone overlay when a panel is dragged over this window
 *
 * ## Coordinate Tracking
 *
 * The window tracks its AWT position using a [ComponentAdapter] to get accurate
 * screen pixel coordinates. This is necessary for:
 * - Detecting when a dragged panel enters this window
 * - Converting tab drag positions to screen coordinates
 * - Accurate drop zone hit testing
 *
 * ## Tab Dragging
 *
 * When this window contains multiple panels (tab group), individual tabs can be
 * dragged out to:
 * - Create a new floating window
 * - Dock to the main layout
 * - Merge into another floating window
 *
 * @param window The floating window state to render
 * @param layout The complete dock layout (for panel descriptors)
 * @param registry The panel content registry
 * @param dragState Current drag state for drop zone detection
 * @param onAction Callback to dispatch dock actions
 * @param mainWindowScreenOffsetX Main window X position for coordinate conversion
 * @param mainWindowScreenOffsetY Main window Y position for coordinate conversion
 * @param mainWindowDensity Display density for coordinate conversion
 *
 * @see NativeFloatingWindowHost for the host that creates these windows
 * @see NativeWindowTitleBar for the custom title bar
 * @see FloatingWindowTabBar for the tab bar component
 * @see FloatingWindowDropZoneOverlay for drop zone visualization
 */
@Composable
internal fun NativeFloatingWindow(
    window: FloatingWindow,
    layout: DockLayout,
    registry: DockPanelRegistry,
    dragState: DragState?,
    onAction: (DockAction) -> Unit,
    mainWindowScreenOffsetX: Float,
    mainWindowScreenOffsetY: Float,
    mainWindowDensity: Float,
    isDarkMode: Boolean = false
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    // Get all panel IDs from the window content
    val allPanelIds = remember(window.content) { window.content.collectPanelIds().toList() }

    // Determine active panel index for tab groups
    val activeTabIndex = remember(window.content) {
        when (val content = window.content) {
            is DockNode.TabGroup -> content.activeTabIndex
            else -> 0
        }
    }

    // Get the active panel ID
    val activePanelId = allPanelIds.getOrNull(activeTabIndex) ?: allPanelIds.firstOrNull()
    val activeDescriptor = activePanelId?.let { layout.panelDescriptors[it] }
    val title = activeDescriptor?.title ?: "Panel"

    // Check if this is a tab group (multiple panels)
    val isTabGroup = allPanelIds.size > 1

    val windowState = rememberWindowState(
        position = WindowPosition(window.x.dp, window.y.dp),
        size = DpSize(window.width.dp, window.height.dp),
        placement = when {
            window.isFullscreen -> WindowPlacement.Fullscreen
            window.isMaximized -> WindowPlacement.Maximized
            else -> WindowPlacement.Floating
        }
    )

    // Track actual AWT window position and size in SCREEN PIXELS
    var awtWindowX by remember { mutableStateOf(0) }
    var awtWindowY by remember { mutableStateOf(0) }
    var awtWindowWidth by remember { mutableStateOf(0) }
    var awtWindowHeight by remember { mutableStateOf(0) }

    // Check if a panel is being dragged and this window should show drop zones
    val isDropTarget = dragState != null && (
            (dragState.isFromFloating && dragState.sourceWindowId != window.id) ||
                    !dragState.isFromFloating
            )

    // Check if drag is within this window's bounds (using screen pixel coordinates)
    val isWithinWindow = remember(dragState, awtWindowX, awtWindowY, awtWindowWidth, awtWindowHeight) {
        if (dragState == null) false
        else {
            val screenX = dragState.screenX
            val screenY = dragState.screenY
            screenX >= awtWindowX && screenX <= awtWindowX + awtWindowWidth &&
                    screenY >= awtWindowY && screenY <= awtWindowY + awtWindowHeight
        }
    }

    // Track hovered zone
    var hoveredZone by remember { mutableStateOf<DockZone?>(null) }

    // Notify parent when this window is hovered during drag
    LaunchedEffect(isDropTarget, isWithinWindow, hoveredZone) {
        if (isDropTarget && isWithinWindow) {
            onAction(DockAction.UpdateDragHoverFloating(window.id))
            if (hoveredZone != null) {
                onAction(DockAction.UpdateDragHover(null, hoveredZone))
            }
        } else if (dragState?.hoveredFloatingWindowId == window.id && !isWithinWindow) {
            onAction(DockAction.UpdateDragHoverFloating(null))
            onAction(DockAction.UpdateDragHover(null, null))
        }
    }

    Window(
        onCloseRequest = {
            // Close all panels in this window
            allPanelIds.forEach { id -> onAction(DockAction.RemovePanel(id)) }
        },
        state = windowState,
        title = title,
        undecorated = isWindows,
        transparent = false,
        resizable = true
    ) {
        // Track AWT window position using component listener for accurate screen coordinates
        // Also set native title bar color on macOS/Linux
        DisposableEffect(window, isDarkMode) {
            val awtWindow = this@Window.window

            // Set native title bar color and text color on macOS/Linux
            if (!isWindows) {
                val titleBarColor = if (isDarkMode) {
                    java.awt.Color(
                        (AzoraPalette.Neutral90.red * 255).toInt(),
                        (AzoraPalette.Neutral90.green * 255).toInt(),
                        (AzoraPalette.Neutral90.blue * 255).toInt()
                    )
                } else {
                    java.awt.Color(
                        (azoraLightPalette.surfaceMid.red * 255).toInt(),
                        (azoraLightPalette.surfaceMid.green * 255).toInt(),
                        (azoraLightPalette.surfaceMid.blue * 255).toInt()
                    )
                }
                awtWindow.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                awtWindow.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                // Set title bar text color: dark appearance = light text (Neutral10), light appearance = dark text
                awtWindow.rootPane.putClientProperty(
                    "apple.awt.windowAppearance",
                    if (isDarkMode) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                )
                awtWindow.background = titleBarColor
            }
            val listener = object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent) {
                    awtWindowX = awtWindow.x
                    awtWindowY = awtWindow.y
                }
                override fun componentResized(e: ComponentEvent) {
                    awtWindowWidth = awtWindow.width
                    awtWindowHeight = awtWindow.height
                }
            }
            awtWindow.addComponentListener(listener)
            // Initial values
            awtWindowX = awtWindow.x
            awtWindowY = awtWindow.y
            awtWindowWidth = awtWindow.width
            awtWindowHeight = awtWindow.height
            onDispose {
                awtWindow.removeComponentListener(listener)
            }
        }

        // Sync window state changes back to dock state
        LaunchedEffect(windowState.position, windowState.size) {
            if (windowState.placement == WindowPlacement.Floating) {
                val newX = windowState.position.x.value
                val newY = windowState.position.y.value
                if (newX != window.x || newY != window.y) {
                    onAction(DockAction.MoveFloatingWindow(window.id, newX, newY))
                }
                val newWidth = windowState.size.width.value
                val newHeight = windowState.size.height.value
                if (newWidth != window.width || newHeight != window.height) {
                    onAction(DockAction.ResizeFloatingWindow(window.id, newWidth, newHeight))
                }
            }
        }

        // Sync placement changes
        LaunchedEffect(windowState.placement) {
            val isMax = windowState.placement == WindowPlacement.Maximized
            val isFull = windowState.placement == WindowPlacement.Fullscreen
            if (isMax != window.isMaximized) {
                onAction(DockAction.ToggleFloatingWindowMaximized(window.id))
            }
            if (isFull != window.isFullscreen) {
                onAction(DockAction.ToggleFloatingWindowFullscreen(window.id))
            }
        }

        // Provide dock theming
        CompositionLocalProvider(
            LocalDockColors provides colors,
            LocalDockDimensions provides dimensions,
            LocalDockPanelRegistry provides registry
        ) {
            // Safe area padding for macOS transparent title bar
            val safeAreaTop = if (!isWindows) MACOS_TITLE_BAR_HEIGHT.dp else 0.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.floatingWindowBackground)
                        .padding(top = safeAreaTop)
                ) {
                    // Custom title bar only on Windows (macOS/Linux use native title bar)
                    if (isWindows) {
                        WindowDraggableArea {
                            NativeWindowTitleBar(
                                title = title,
                                isMaximized = windowState.placement == WindowPlacement.Maximized,
                                isFullscreen = windowState.placement == WindowPlacement.Fullscreen,
                                onMinimize = {
                                    windowState.isMinimized = true
                                },
                                onMaximize = {
                                    windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                        WindowPlacement.Floating
                                    } else {
                                        WindowPlacement.Maximized
                                    }
                                },
                                onFullscreen = {
                                    windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                                        WindowPlacement.Floating
                                    } else {
                                        WindowPlacement.Fullscreen
                                    }
                                },
                                onClose = {
                                    allPanelIds.forEach { id -> onAction(DockAction.RemovePanel(id)) }
                                },
                                onDock = {
                                    onAction(DockAction.DockFloatingWindow(window.id, null, DockZone.CENTER))
                                }
                            )
                        }
                    }

                    // Tab bar only if multiple panels
                    if (allPanelIds.size > 1) {
                        FloatingWindowTabBar(
                            panelIds = allPanelIds,
                            panelDescriptors = layout.panelDescriptors,
                            activeIndex = activeTabIndex,
                            awtWindowX = awtWindowX,
                            awtWindowY = awtWindowY,
                            onTabSelect = { index ->
                                // Find the node ID for this floating window's content
                                val nodeId = window.content.id
                                onAction(DockAction.SelectTab(nodeId, index))
                            },
                            onTabClose = { panelId ->
                                onAction(DockAction.RemovePanel(panelId))
                            },
                            onTabDragStart = { panelId, screenX, screenY ->
                                // Convert screen coordinates to main window local coordinates
                                val localX = (screenX - mainWindowScreenOffsetX) * mainWindowDensity
                                val localY = (screenY - mainWindowScreenOffsetY) * mainWindowDensity
                                onAction(DockAction.StartDragFromFloating(panelId, window.id, localX, localY))
                            },
                            onTabDrag = { screenX, screenY ->
                                // Convert screen coordinates to main window local coordinates
                                val localX = (screenX - mainWindowScreenOffsetX) * mainWindowDensity
                                val localY = (screenY - mainWindowScreenOffsetY) * mainWindowDensity
                                onAction(DockAction.UpdateDragPosition(
                                    x = localX,
                                    y = localY,
                                    screenX = screenX,
                                    screenY = screenY
                                ))
                            },
                            onTabDragEnd = {
                                onAction(DockAction.EndDrag)
                            }
                        )
                    }

                    // Panel content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Render the active panel content
                        activePanelId?.let { panelId ->
                            val content = registry.getContent(panelId)
                            content?.invoke()
                        }
                    }
                }

                // Drop zone overlay when a panel is being dragged AND cursor is within this window
                if (isDropTarget && isWithinWindow) {
                    FloatingWindowDropZoneOverlay(
                        dragState = dragState,
                        awtWindowX = awtWindowX,
                        awtWindowY = awtWindowY,
                        awtWindowWidth = awtWindowWidth,
                        awtWindowHeight = awtWindowHeight,
                        onZoneHover = { zone -> hoveredZone = zone }
                    )
                }
            }
        }
    }
}