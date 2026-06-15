package dev.azora.sdk.docking.domain

import kotlinx.serialization.Serializable

/**
 * Complete layout configuration for the docking system.
 *
 * A DockLayout represents the entire state of a docking system, including:
 * - The main docked panels (in [rootNode])
 * - Floating windows (in [floatingWindows])
 * - Panel metadata (in [panelDescriptors])
 *
 * Layouts are serializable and can be saved/restored to persist user preferences.
 *
 * Example:
 * ```kotlin
 * val layout = DockLayout(
 *     rootNode = DockNode.Split(
 *         id = "root",
 *         orientation = DockOrientation.HORIZONTAL,
 *         first = DockNode.Leaf("node1", "explorer"),
 *         second = DockNode.Leaf("node2", "editor"),
 *         ratio = 0.25f
 *     ),
 *     panelDescriptors = mapOf(
 *         "explorer" to DockPanelDescriptor("explorer", "Explorer"),
 *         "editor" to DockPanelDescriptor("editor", "Editor")
 *     )
 * )
 * ```
 *
 * @property rootNode The root of the docked panel tree, or null for an empty layout
 * @property floatingWindows List of floating (undocked) windows
 * @property panelDescriptors Metadata for all panels in the layout
 * @property name Optional name for the layout (useful for saved layouts)
 *
 * @see DockNode for the layout tree structure
 * @see FloatingWindow for floating window configuration
 * @see DockPanelDescriptor for panel metadata
 */
@Serializable
data class DockLayout(
    val rootNode: DockNode?,
    val floatingWindows: List<FloatingWindow> = emptyList(),
    val panelDescriptors: Map<String, DockPanelDescriptor> = emptyMap(),
    val name: String = "Default"
) {

    /**
     * Returns all panel IDs in the layout (both docked and floating).
     */
    fun getAllPanelIds(): Set<String> {
        val dockedPanels = rootNode?.collectPanelIds() ?: emptySet()
        val floatingPanels = floatingWindows.flatMap { it.content.collectPanelIds() }.toSet()
        return dockedPanels + floatingPanels
    }

    /**
     * Checks if a panel exists in the layout.
     *
     * @param panelId The ID of the panel to check
     * @return true if the panel is in the layout (docked or floating)
     */
    fun containsPanel(panelId: String): Boolean = panelId in getAllPanelIds()

    /**
     * Gets the descriptor for a panel.
     *
     * @param panelId The ID of the panel
     * @return The panel descriptor, or null if not found
     */
    fun getDescriptor(panelId: String): DockPanelDescriptor? = panelDescriptors[panelId]
}