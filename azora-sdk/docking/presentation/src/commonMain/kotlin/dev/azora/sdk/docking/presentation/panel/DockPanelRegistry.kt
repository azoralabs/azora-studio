package dev.azora.sdk.docking.presentation.panel

import androidx.compose.runtime.Composable

/**
 * Registry that maps panel IDs to their composable content.
 *
 * The panel registry is the bridge between the docking system's data model
 * (which stores panel IDs and metadata) and the actual UI content. When a
 * panel needs to be rendered, the registry is queried for the composable
 * function that provides its content.
 *
 * ## Usage
 *
 * Register panel content at setup time:
 *
 * ```kotlin
 * val registry = DockPanelRegistry()
 *
 * // Register panel content
 * registry.register("explorer") {
 *     FileExplorerPanel()
 * }
 *
 * registry.register("properties") {
 *     PropertiesPanel()
 * }
 *
 * // Use with DockTheme
 * DockTheme(registry = registry) {
 *     DockContainer(...)
 * }
 * ```
 *
 * ## Dynamic Panels
 *
 * For panels that are created/destroyed at runtime:
 *
 * ```kotlin
 * // When opening a new document
 * fun openDocument(doc: Document) {
 *     val panelId = "doc_${doc.id}"
 *     registry.register(panelId) {
 *         DocumentEditor(doc)
 *     }
 *     // Add panel to layout...
 * }
 *
 * // When closing a document
 * fun closeDocument(panelId: String) {
 *     registry.unregister(panelId)
 *     // Remove panel from layout...
 * }
 * ```
 *
 * ## Access
 *
 * The registry is provided through [LocalDockPanelRegistry] and accessed
 * by dock composables to render panel content.
 *
 * @see LocalDockPanelRegistry for composition local access
 * @see DockTheme for providing the registry
 */
class DockPanelRegistry {

    private val panels = mutableMapOf<String, @Composable () -> Unit>()

    /**
     * Registers a composable content provider for a panel.
     *
     * If a panel with the same ID is already registered, it will be replaced.
     *
     * @param panelId Unique identifier for the panel
     * @param content Composable function that renders the panel content
     */
    fun register(panelId: String, content: @Composable () -> Unit) {
        panels[panelId] = content
    }

    /**
     * Removes a panel's content registration.
     *
     * Call this when a panel is permanently closed to prevent memory leaks.
     * If the panel ID is not registered, this is a no-op.
     *
     * @param panelId The panel ID to unregister
     */
    fun unregister(panelId: String) {
        panels.remove(panelId)
    }

    /**
     * Retrieves the content composable for a panel.
     *
     * @param panelId The panel ID to look up
     * @return The composable content function, or null if not registered
     */
    fun getContent(panelId: String): (@Composable () -> Unit)? = panels[panelId]

    /**
     * Checks if a panel ID has registered content.
     *
     * @param panelId The panel ID to check
     * @return True if content is registered for this ID
     */
    fun isRegistered(panelId: String): Boolean = panelId in panels

    /**
     * Returns all registered panel IDs.
     *
     * Useful for debugging or iterating over all available panels.
     *
     * @return Immutable set of all registered panel IDs
     */
    fun getAllPanelIds(): Set<String> = panels.keys.toSet()
}