package dev.azora.sdk.docking.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Pure functions for manipulating dock layouts.
 *
 * This object provides all the operations needed to modify a [DockLayout].
 * All functions are pure - they take a layout and return a new layout without
 * modifying the original. This makes state management predictable and enables
 * features like undo/redo.
 *
 * ## Usage
 *
 * Operations are typically called in response to [DockAction]s:
 *
 * ```kotlin
 * val newLayout = when (action) {
 *     is DockAction.AddPanel -> DockLayoutOperations.addPanel(
 *         layout, action.descriptor, action.targetNodeId, action.zone
 *     )
 *     is DockAction.MovePanel -> DockLayoutOperations.movePanel(
 *         layout, action.panelId, action.targetNodeId, action.zone
 *     )
 *     // ...
 * }
 * ```
 *
 * ## ID Generation
 *
 * Many operations create new nodes and require unique IDs. By default, UUIDs
 * are generated, but a custom generator can be provided for testing or
 * deterministic layouts.
 *
 * ## Categories
 *
 * **Panel Operations:**
 * - [addPanel] - Add a new panel to the layout
 * - [movePanel] - Move a panel to a new location
 * - [removePanel] - Remove a panel from the layout
 * - [floatPanel] - Detach a panel into a floating window
 * - [selectPanel] - Select/focus a panel
 *
 * **Floating Window Operations:**
 * - [dockFloatingWindow] - Dock a floating window back to the layout
 * - [moveFloatingWindow] - Move a floating window
 * - [resizeFloatingWindow] - Resize a floating window
 * - [closeFloatingWindow] - Close a floating window
 * - [addPanelToFloatingWindow] - Add a panel to a floating window
 * - [dockToFloatingWindow] - Merge two floating windows
 * - [toggleFloatingWindowMinimized] - Toggle minimized state
 * - [toggleFloatingWindowMaximized] - Toggle maximized state
 * - [toggleFloatingWindowFullscreen] - Toggle fullscreen state
 *
 * **Layout Operations:**
 * - [setSplitRatio] - Adjust split ratio between panels
 * - [selectTab] - Select a tab in a tab group
 * - [reorderTabs] - Reorder tabs in a tab group
 *
 * @see DockLayout
 * @see DockAction
 * @see DockStateManager
 */
object DockLayoutOperations {

    @OptIn(ExperimentalUuidApi::class)
    private fun defaultGenerateId(): String = Uuid.random().toString().take(8)

    /**
     * Creates a split node based on the dock zone.
     *
     * This helper consolidates the repeated pattern of creating splits for
     * LEFT/RIGHT/TOP/BOTTOM zones, determining orientation and child order
     * based on the zone.
     *
     * @param zone The target zone (LEFT, RIGHT, TOP, or BOTTOM)
     * @param newNode The node being added
     * @param existingNode The node already at the target location
     * @param generateId Function to generate unique node IDs
     * @return A new split node containing both nodes
     */
    private fun createZoneSplit(
        zone: DockZone,
        newNode: DockNode,
        existingNode: DockNode,
        generateId: () -> String
    ): DockNode.Split {
        val isHorizontal = zone == DockZone.LEFT || zone == DockZone.RIGHT
        val newNodeFirst = zone == DockZone.LEFT || zone == DockZone.TOP
        return DockNode.Split(
            id = generateId(),
            orientation = if (isHorizontal) DockOrientation.HORIZONTAL else DockOrientation.VERTICAL,
            first = if (newNodeFirst) newNode else existingNode,
            second = if (newNodeFirst) existingNode else newNode,
            ratio = if (newNodeFirst) DockDefaults.SPLIT_RATIO_PRIMARY else DockDefaults.SPLIT_RATIO_SECONDARY
        )
    }

    /**
     * Adds a new panel to the layout.
     *
     * The panel is inserted at the specified target node and zone. If no target
     * is specified, the panel is added at the root level.
     *
     * ## Zone Behavior
     *
     * - **CENTER**: Adds the panel as a new tab in the target node. If the target
     *   is a leaf, it's converted to a tab group.
     * - **LEFT/RIGHT**: Creates a horizontal split with the new panel on the specified side.
     * - **TOP/BOTTOM**: Creates a vertical split with the new panel on the specified side.
     *
     * @param layout The current layout
     * @param descriptor The panel descriptor to add (must have a unique ID)
     * @param targetNodeId The ID of the node to add to, or null/"__root__" for root level
     * @param zone Where to place the panel relative to the target
     * @param generateId Function to generate unique node IDs
     * @return A new layout with the panel added
     * @see DockAction.AddPanel
     */
    fun addPanel(
        layout: DockLayout,
        descriptor: DockPanelDescriptor,
        targetNodeId: String?,
        zone: DockZone,
        generateId: () -> String = ::defaultGenerateId
    ): DockLayout {
        val newPanelId = descriptor.id
        val newDescriptors = layout.panelDescriptors + (newPanelId to descriptor)

        if (layout.rootNode == null) {
            return layout.copy(
                rootNode = DockNode.Leaf(id = generateId(), panelId = newPanelId),
                panelDescriptors = newDescriptors
            )
        }

        val rootNode = layout.rootNode

        // Treat "__root__" as null (add to root level)
        if (targetNodeId == null || targetNodeId == "__root__") {
            val newRoot = addToNode(rootNode, newPanelId, zone, generateId)
            return layout.copy(rootNode = newRoot, panelDescriptors = newDescriptors)
        }

        val newRoot = addToNodeById(rootNode, targetNodeId, newPanelId, zone, generateId)
        return layout.copy(rootNode = newRoot, panelDescriptors = newDescriptors)
    }

    private fun addToNode(
        node: DockNode,
        panelId: String,
        zone: DockZone,
        generateId: () -> String
    ): DockNode {
        return when (zone) {
            DockZone.CENTER -> {
                when (node) {
                    is DockNode.TabGroup -> node.copy(
                        panels = node.panels + panelId,
                        activeTabIndex = node.panels.size
                    )
                    is DockNode.Leaf -> DockNode.TabGroup(
                        id = node.id,
                        panels = listOf(node.panelId, panelId),
                        activeTabIndex = 1
                    )
                    is DockNode.Split -> node.copy(second = addToNode(node.second, panelId, zone, generateId))
                }
            }
            DockZone.LEFT, DockZone.RIGHT, DockZone.TOP, DockZone.BOTTOM -> {
                createZoneSplit(
                    zone = zone,
                    newNode = DockNode.Leaf(id = generateId(), panelId = panelId),
                    existingNode = node,
                    generateId = generateId
                )
            }
        }
    }

    private fun addToNodeById(
        root: DockNode,
        targetNodeId: String,
        panelId: String,
        zone: DockZone,
        generateId: () -> String
    ): DockNode {
        if (root.id == targetNodeId) {
            return addToNode(root, panelId, zone, generateId)
        }
        return when (root) {
            is DockNode.Split -> {
                val firstContains = root.first.findNode(targetNodeId) != null
                if (firstContains) {
                    root.copy(first = addToNodeById(root.first, targetNodeId, panelId, zone, generateId))
                } else {
                    root.copy(second = addToNodeById(root.second, targetNodeId, panelId, zone, generateId))
                }
            }
            else -> root
        }
    }

    /**
     * Moves a panel to a new location in the layout.
     *
     * The panel is removed from its current location and added to the target node
     * at the specified zone. This is typically triggered by drag-and-drop operations.
     *
     * If the panel's removal leaves an empty container, that container is collapsed.
     * The panel's descriptor is preserved during the move.
     *
     * @param layout The current layout
     * @param panelId The ID of the panel to move
     * @param targetNodeId The ID of the target node, or "__root__" for root level
     * @param zone Where to place the panel relative to the target
     * @param generateId Function to generate unique node IDs
     * @return A new layout with the panel moved
     * @see DockAction.MovePanel
     */
    fun movePanel(
        layout: DockLayout,
        panelId: String,
        targetNodeId: String,
        zone: DockZone,
        generateId: () -> String = ::defaultGenerateId
    ): DockLayout {
        val originalRoot = layout.rootNode ?: return layout
        val rootAfterRemove = originalRoot.removePanel(panelId) ?: return layout
        val tempLayout = layout.copy(rootNode = rootAfterRemove)
        val effectiveTargetNodeId = when {
            targetNodeId == "__root__" -> null
            rootAfterRemove.id == targetNodeId -> null
            targetNodeId == originalRoot.id -> null
            else -> targetNodeId
        }
        return addPanel(
            layout = tempLayout,
            descriptor = layout.panelDescriptors[panelId] ?: return layout,
            targetNodeId = effectiveTargetNodeId,
            zone = zone,
            generateId = generateId
        )
    }

    /**
     * Detaches a panel from the docked layout into a floating window.
     *
     * Creates a new [FloatingWindow] containing the panel. The panel is removed
     * from its current docked location. The window size is constrained by the
     * panel's minimum dimensions.
     *
     * @param layout The current layout
     * @param panelId The ID of the panel to float
     * @param x The X position for the floating window (screen coordinates)
     * @param y The Y position for the floating window (screen coordinates)
     * @param width The desired width (will be at least the panel's minimum width)
     * @param height The desired height (will be at least the panel's minimum height)
     * @param generateId Function to generate unique node/window IDs
     * @return A new layout with the panel in a floating window
     * @see DockAction.FloatPanel
     * @see FloatingWindow
     */
    fun floatPanel(
        layout: DockLayout,
        panelId: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        generateId: () -> String = ::defaultGenerateId
    ): DockLayout {
        val descriptor = layout.panelDescriptors[panelId] ?: return layout
        val newRoot = layout.rootNode?.removePanel(panelId)
        val floatingWindow = FloatingWindow(
            id = generateId(),
            content = DockNode.Leaf(id = generateId(), panelId = panelId),
            x = x, y = y,
            width = width.coerceAtLeast(descriptor.minimumWidth),
            height = height.coerceAtLeast(descriptor.minimumHeight)
        )
        return layout.copy(rootNode = newRoot, floatingWindows = layout.floatingWindows + floatingWindow)
    }

    /**
     * Docks a floating window back into the main layout.
     *
     * The floating window is closed and all its panels are inserted into the
     * docked layout at the specified target node and zone. Multiple panels
     * are added sequentially.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window to dock
     * @param targetNodeId The ID of the target node, or null for root level
     * @param zone Where to place the content relative to the target
     * @param generateId Function to generate unique node IDs
     * @return A new layout with the floating window's content docked
     * @see DockAction.DockFloatingWindow
     */
    fun dockFloatingWindow(
        layout: DockLayout,
        windowId: String,
        targetNodeId: String?,
        zone: DockZone,
        generateId: () -> String = ::defaultGenerateId
    ): DockLayout {
        val window = layout.floatingWindows.find { it.id == windowId } ?: return layout
        val panelIds = window.content.collectPanelIds()
        var newLayout = layout.copy(floatingWindows = layout.floatingWindows.filter { it.id != windowId })
        panelIds.forEach { panelId ->
            val descriptor = layout.panelDescriptors[panelId]
            if (descriptor != null) {
                newLayout = addPanel(newLayout, descriptor, targetNodeId, zone, generateId)
            }
        }
        return newLayout
    }

    /**
     * Adjusts the split ratio between two panels in a split node.
     *
     * The ratio determines how space is divided between the first and second
     * children. A ratio of 0.3 gives the first child 30% and the second 70%.
     * The ratio is clamped to [DockDefaults.MIN_SPLIT_RATIO]..[DockDefaults.MAX_SPLIT_RATIO].
     *
     * @param layout The current layout
     * @param nodeId The ID of the split node to adjust
     * @param ratio The new split ratio (0.0 to 1.0)
     * @return A new layout with the adjusted split ratio
     * @see DockAction.SetSplitRatio
     * @see DockNode.Split
     */
    fun setSplitRatio(layout: DockLayout, nodeId: String, ratio: Float): DockLayout {
        val clampedRatio = ratio.coerceIn(DockDefaults.MIN_SPLIT_RATIO, DockDefaults.MAX_SPLIT_RATIO)
        val newRoot = layout.rootNode?.updateNode(nodeId) { node ->
            if (node is DockNode.Split) node.copy(ratio = clampedRatio) else node
        }
        return layout.copy(rootNode = newRoot)
    }

    /**
     * Selects a tab in a tab group by index.
     *
     * Updates the active tab in a tab group node, both in the main docked
     * layout and in any floating windows. The index is clamped to valid bounds.
     *
     * @param layout The current layout
     * @param nodeId The ID of the tab group node
     * @param tabIndex The zero-based index of the tab to select
     * @return A new layout with the tab selected
     * @see DockAction.SelectTab
     * @see DockNode.TabGroup
     */
    fun selectTab(layout: DockLayout, nodeId: String, tabIndex: Int): DockLayout {
        // First try to update in rootNode
        val newRoot = layout.rootNode?.updateNode(nodeId) { node ->
            if (node is DockNode.TabGroup) {
                node.copy(activeTabIndex = tabIndex.coerceIn(0, node.panels.size - 1))
            } else node
        }

        // Also update in floating windows
        val newFloatingWindows = layout.floatingWindows.map { window ->
            val updatedContent = window.content.updateNode(nodeId) { node ->
                if (node is DockNode.TabGroup) {
                    node.copy(activeTabIndex = tabIndex.coerceIn(0, node.panels.size - 1))
                } else node
            }
            if (updatedContent != window.content) {
                window.copy(content = updatedContent)
            } else {
                window
            }
        }

        return layout.copy(rootNode = newRoot, floatingWindows = newFloatingWindows)
    }

    /**
     * Selects a panel by ID, making it visible and focused.
     *
     * If the panel is in a tab group, its tab becomes the active tab.
     * This is a convenience method that finds the panel and calls [selectTab].
     *
     * @param layout The current layout
     * @param panelId The ID of the panel to select
     * @return A new layout with the panel selected
     * @see DockAction.SelectPanel
     */
    fun selectPanel(layout: DockLayout, panelId: String): DockLayout {
        val node = layout.rootNode?.findNodeByPanelId(panelId)
        return if (node is DockNode.TabGroup) {
            val index = node.panels.indexOf(panelId)
            if (index >= 0) selectTab(layout, node.id, index) else layout
        } else layout
    }

    /**
     * Reorders tabs within a tab group.
     *
     * Moves a tab from one position to another within the same tab group.
     * The active tab index is adjusted to follow the tab if it was moved.
     *
     * @param layout The current layout
     * @param nodeId The ID of the tab group node
     * @param fromIndex The current zero-based index of the tab
     * @param toIndex The target zero-based index for the tab
     * @return A new layout with tabs reordered
     * @see DockAction.ReorderTabs
     */
    fun reorderTabs(layout: DockLayout, nodeId: String, fromIndex: Int, toIndex: Int): DockLayout {
        val newRoot = layout.rootNode?.updateNode(nodeId) { node ->
            if (node is DockNode.TabGroup && fromIndex in node.panels.indices && toIndex in node.panels.indices) {
                val mutablePanels = node.panels.toMutableList()
                val panel = mutablePanels.removeAt(fromIndex)
                mutablePanels.add(toIndex, panel)
                val newActiveIndex = when {
                    node.activeTabIndex == fromIndex -> toIndex
                    fromIndex < node.activeTabIndex && toIndex >= node.activeTabIndex -> node.activeTabIndex - 1
                    fromIndex > node.activeTabIndex && toIndex <= node.activeTabIndex -> node.activeTabIndex + 1
                    else -> node.activeTabIndex
                }
                node.copy(panels = mutablePanels, activeTabIndex = newActiveIndex)
            } else node
        }
        return layout.copy(rootNode = newRoot)
    }

    /**
     * Moves a floating window to a new position.
     *
     * Updates the position of an existing floating window without changing its size.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window to move
     * @param x The new X position (screen coordinates)
     * @param y The new Y position (screen coordinates)
     * @return A new layout with the window moved
     * @see DockAction.MoveFloatingWindow
     */
    fun moveFloatingWindow(layout: DockLayout, windowId: String, x: Float, y: Float): DockLayout {
        return layout.copy(floatingWindows = layout.floatingWindows.map { window ->
            if (window.id == windowId) window.copy(x = x, y = y) else window
        })
    }

    /**
     * Resizes a floating window.
     *
     * Updates the dimensions of an existing floating window. The size is
     * constrained by the minimum dimensions of the panels it contains.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window to resize
     * @param width The new width (will be at least the minimum panel width)
     * @param height The new height (will be at least the minimum panel height)
     * @return A new layout with the window resized
     * @see DockAction.ResizeFloatingWindow
     */
    fun resizeFloatingWindow(layout: DockLayout, windowId: String, width: Float, height: Float): DockLayout {
        return layout.copy(floatingWindows = layout.floatingWindows.map { window ->
            if (window.id == windowId) {
                val minWidth = window.content.collectPanelIds()
                    .mapNotNull { layout.panelDescriptors[it]?.minimumWidth }
                    .maxOrNull() ?: DockDefaults.PANEL_MIN_WIDTH
                val minHeight = window.content.collectPanelIds()
                    .mapNotNull { layout.panelDescriptors[it]?.minimumHeight }
                    .maxOrNull() ?: DockDefaults.PANEL_MIN_HEIGHT
                window.copy(width = width.coerceAtLeast(minWidth), height = height.coerceAtLeast(minHeight))
            } else window
        })
    }

    /**
     * Closes a floating window and removes its panels.
     *
     * The floating window is removed from the layout along with all panels
     * it contains. Panel descriptors are also removed.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window to close
     * @return A new layout with the window and its panels removed
     * @see DockAction.CloseFloatingWindow
     */
    fun closeFloatingWindow(layout: DockLayout, windowId: String): DockLayout {
        val window = layout.floatingWindows.find { it.id == windowId } ?: return layout
        val panelIds = window.content.collectPanelIds()
        return layout.copy(
            floatingWindows = layout.floatingWindows.filter { it.id != windowId },
            panelDescriptors = layout.panelDescriptors.filterKeys { it !in panelIds }
        )
    }

    /**
     * Toggles the minimized state of a floating window.
     *
     * When minimized, the window collapses to show only its title bar.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window
     * @return A new layout with the window's minimized state toggled
     * @see DockAction.ToggleFloatingWindowMinimized
     */
    fun toggleFloatingWindowMinimized(layout: DockLayout, windowId: String): DockLayout {
        return layout.copy(floatingWindows = layout.floatingWindows.map { window ->
            if (window.id == windowId) window.copy(isMinimized = !window.isMinimized) else window
        })
    }

    /**
     * Toggles the maximized state of a floating window.
     *
     * When maximized, the window expands to fill the available space.
     * Toggling maximize exits fullscreen mode if active.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window
     * @return A new layout with the window's maximized state toggled
     * @see DockAction.ToggleFloatingWindowMaximized
     */
    fun toggleFloatingWindowMaximized(layout: DockLayout, windowId: String): DockLayout {
        return layout.copy(floatingWindows = layout.floatingWindows.map { window ->
            if (window.id == windowId) window.copy(
                isMaximized = !window.isMaximized,
                isFullscreen = false // Exit fullscreen when toggling maximize
            ) else window
        })
    }

    /**
     * Toggles the fullscreen state of a floating window.
     *
     * When fullscreen, the window covers the entire screen without decorations.
     * Toggling fullscreen exits maximize mode if active.
     *
     * @param layout The current layout
     * @param windowId The ID of the floating window
     * @return A new layout with the window's fullscreen state toggled
     * @see DockAction.ToggleFloatingWindowFullscreen
     */
    fun toggleFloatingWindowFullscreen(layout: DockLayout, windowId: String): DockLayout {
        return layout.copy(floatingWindows = layout.floatingWindows.map { window ->
            if (window.id == windowId) window.copy(
                isFullscreen = !window.isFullscreen,
                isMaximized = false // Exit maximize when toggling fullscreen
            ) else window
        })
    }

    /**
     * Removes a panel from the layout.
     *
     * The panel is removed from both the docked layout and any floating windows.
     * The panel's descriptor is also removed. If removal leaves empty containers,
     * they are collapsed.
     *
     * @param layout The current layout
     * @param panelId The ID of the panel to remove
     * @return A new layout with the panel removed
     * @see DockAction.RemovePanel
     */
    fun removePanel(layout: DockLayout, panelId: String): DockLayout {
        val newRoot = layout.rootNode?.removePanel(panelId)
        val newFloating = layout.floatingWindows.mapNotNull { window ->
            val newContent = window.content.removePanel(panelId)
            if (newContent == null) null else window.copy(content = newContent)
        }
        return layout.copy(
            rootNode = newRoot,
            floatingWindows = newFloating,
            panelDescriptors = layout.panelDescriptors - panelId
        )
    }

    /**
     * Moves a panel from the main docked area to a floating window.
     *
     * The panel is removed from its docked location and added to the target
     * floating window at the specified zone. If the zone is CENTER, the panel
     * is merged into a tab group with the window's existing content.
     *
     * @param layout The current layout
     * @param panelId The ID of the panel to move
     * @param targetWindowId The ID of the target floating window
     * @param zone Where to place the panel in the floating window
     * @param generateId Function to generate unique node IDs
     * @return A new layout with the panel moved to the floating window
     */
    fun addPanelToFloatingWindow(
        layout: DockLayout,
        panelId: String,
        targetWindowId: String,
        zone: DockZone,
        generateId: () -> String = ::defaultGenerateId
    ): DockLayout {
        if (panelId !in layout.panelDescriptors) return layout
        val targetWindow = layout.floatingWindows.find { it.id == targetWindowId } ?: return layout

        // Remove panel from main layout
        val newRoot = layout.rootNode?.removePanel(panelId)

        // Add panel to floating window
        val newContent = when (zone) {
            DockZone.CENTER -> {
                // Merge into tab group
                val targetPanels = targetWindow.content.collectPanelIds().toList()
                DockNode.TabGroup(
                    id = generateId(),
                    panels = targetPanels + panelId,
                    activeTabIndex = targetPanels.size
                )
            }
            DockZone.LEFT, DockZone.RIGHT, DockZone.TOP, DockZone.BOTTOM -> {
                createZoneSplit(
                    zone = zone,
                    newNode = DockNode.Leaf(id = generateId(), panelId = panelId),
                    existingNode = targetWindow.content,
                    generateId = generateId
                )
            }
        }

        val updatedWindow = targetWindow.copy(content = newContent)
        return layout.copy(
            rootNode = newRoot,
            floatingWindows = layout.floatingWindows.map {
                if (it.id == targetWindowId) updatedWindow else it
            }
        )
    }

    /**
     * Merges one floating window into another.
     *
     * The source floating window's content is added to the target window at the
     * specified zone, then the source window is removed. If the zone is CENTER,
     * both windows' panels are merged into a single tab group.
     *
     * @param layout The current layout
     * @param sourceWindowId The ID of the floating window being merged (will be closed)
     * @param targetWindowId The ID of the target floating window
     * @param zone Where to place the source content in the target window
     * @param generateId Function to generate unique node IDs
     * @return A new layout with the windows merged
     * @see DockAction.DockToFloatingWindow
     */
    fun dockToFloatingWindow(
        layout: DockLayout,
        sourceWindowId: String,
        targetWindowId: String,
        zone: DockZone,
        generateId: () -> String = ::defaultGenerateId
    ): DockLayout {
        val sourceWindow = layout.floatingWindows.find { it.id == sourceWindowId } ?: return layout
        val targetWindow = layout.floatingWindows.find { it.id == targetWindowId } ?: return layout

        // Merge source content into target
        val newContent = when (zone) {
            DockZone.CENTER -> {
                // Merge into tab group
                val sourcePanels = sourceWindow.content.collectPanelIds().toList()
                val targetPanels = targetWindow.content.collectPanelIds().toList()
                if (sourcePanels.isNotEmpty() && targetPanels.isNotEmpty()) {
                    DockNode.TabGroup(
                        id = generateId(),
                        panels = targetPanels + sourcePanels,
                        activeTabIndex = targetPanels.size
                    )
                } else targetWindow.content
            }
            DockZone.LEFT, DockZone.RIGHT, DockZone.TOP, DockZone.BOTTOM -> {
                createZoneSplit(
                    zone = zone,
                    newNode = sourceWindow.content,
                    existingNode = targetWindow.content,
                    generateId = generateId
                )
            }
        }

        // Update target window with merged content and remove source window
        val updatedTarget = targetWindow.copy(content = newContent)
        return layout.copy(
            floatingWindows = layout.floatingWindows
                .filter { it.id != sourceWindowId }
                .map { if (it.id == targetWindowId) updatedTarget else it }
        )
    }
}