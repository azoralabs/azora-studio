package dev.azora.studio.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.docking.data.DockStateManagerImpl
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.presentation.container.DockContainer
import dev.azora.sdk.docking.presentation.panel.DockPanelRegistry
import dev.azora.sdk.docking.presentation.theme.DockTheme
import dev.azora.sdk.docking.presentation.theme.LocalAllowExternalDrag
import dev.azora.sdk.plugin.core.PluginPanelDescriptor
import dev.azora.sdk.plugin.presentation.PluginManager

/**
 * Renders a group of plugin panels (sidebar, canvas, properties) inside
 * a single dock panel using a nested DockContainer.
 *
 * The internal layout is a 3-way horizontal split:
 *   Sidebar (0.2) | Canvas (0.6) | Properties (0.2)
 *
 * Panels can be resized and rearranged within this container
 * but remain confined to the parent dock panel.
 */
@Composable
fun PluginGroupPanel(
    pluginId: String,
    panels: List<PluginPanelDescriptor>,
    pluginManager: PluginManager,
    project: AzoraProjectModel
) {
    // Create a local DockStateManager for the nested layout
    val innerStateManager = remember(pluginId) {
        val sidebarId = "inner_${pluginId}_${panels.getOrNull(0)?.id ?: "sidebar"}"
        val canvasId = "inner_${pluginId}_${panels.getOrNull(1)?.id ?: "canvas"}"
        val propertiesId = "inner_${pluginId}_${panels.getOrNull(2)?.id ?: "properties"}"

        val descriptors = mutableMapOf<String, DockPanelDescriptor>()
        val panelIds = mutableListOf<String>()

        panels.forEachIndexed { index, panel ->
            val innerId = "inner_${pluginId}_${panel.id}"
            panelIds.add(innerId)
            descriptors[innerId] = DockPanelDescriptor(
                id = innerId,
                title = panel.title,
                minimumWidth = panel.minimumWidth,
                minimumHeight = panel.minimumHeight,
                closeable = true
            )
        }

        // Build a sidebar | canvas | properties horizontal split
        val rootNode = if (panelIds.size >= 3) {
            DockNode.Split(
                id = "inner_root",
                orientation = DockOrientation.HORIZONTAL,
                first = DockNode.Leaf(id = "inner_left", panelId = panelIds[0]),
                second = DockNode.Split(
                    id = "inner_right_split",
                    orientation = DockOrientation.HORIZONTAL,
                    first = DockNode.Leaf(id = "inner_center", panelId = panelIds[1]),
                    second = DockNode.Leaf(id = "inner_right", panelId = panelIds[2]),
                    ratio = 0.75f
                ),
                ratio = 0.2f
            )
        } else {
            // Fallback: tab group for fewer panels
            DockNode.TabGroup(
                id = "inner_tabs",
                panels = panelIds,
                activeTabIndex = 0
            )
        }

        val layout = DockLayout(
            rootNode = rootNode,
            panelDescriptors = descriptors
        )

        DockStateManagerImpl(initialLayout = layout)
    }

    val innerState by innerStateManager.state.collectAsState()

    // Build registry for the inner panels
    val innerRegistry = remember(pluginId, panels) {
        DockPanelRegistry().apply {
            panels.forEach { panel ->
                val innerId = "inner_${pluginId}_${panel.id}"
                val content = pluginManager.getPluginPanelContent(pluginId, panel.id)
                if (content != null) {
                    register(innerId) { content(project) }
                }
            }
        }
    }

    DockTheme(registry = innerRegistry) {
        CompositionLocalProvider(LocalAllowExternalDrag provides false) {
            DockContainer(
                layout = innerState.layout,
                dragState = innerState.dragState,
                maximizedPanelId = innerState.maximizedPanelId,
                onAction = { action -> innerStateManager.dispatch(action) },
                modifier = Modifier.fillMaxSize(),
                renderFloatingWindows = false
            )
        }
    }
}
