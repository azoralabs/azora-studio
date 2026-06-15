package dev.azora.sdk.plugin.presentation

import androidx.compose.runtime.Composable
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.plugin.core.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic plugin manager interface.
 * Desktop implementation uses URLClassLoader to load plugin JARs.
 */
interface PluginManager {

    /**
     * Flow of all installed plugins with their enabled state.
     */
    val installedPlugins: StateFlow<List<InstalledPlugin>>

    /**
     * Load all installed plugins from the plugin directory.
     */
    suspend fun loadInstalledPlugins()

    /**
     * Discover available plugins (JAR files) in the plugin directory.
     *
     * @return List of discovered plugin manifests
     */
    suspend fun discoverPlugins(): List<PluginManifest>

    /**
     * Install a plugin from a JAR file.
     *
     * @param sourcePath Path to the JAR file to install
     * @return The installed plugin info, or null on failure
     */
    suspend fun installPlugin(sourcePath: String): InstalledPlugin?

    /**
     * Uninstall a plugin.
     *
     * @param pluginId The plugin ID to uninstall
     */
    suspend fun uninstallPlugin(pluginId: String)

    /**
     * Enable a plugin.
     *
     * @param pluginId The plugin ID to enable
     */
    suspend fun enablePlugin(pluginId: String)

    /**
     * Disable a plugin.
     *
     * @param pluginId The plugin ID to disable
     */
    suspend fun disablePlugin(pluginId: String)

    /**
     * Get the Composable content for a loaded plugin.
     *
     * @param pluginId The plugin ID
     * @return A Composable function, or null if the plugin is not loaded
     */
    fun getPluginContent(pluginId: String): (@Composable (AzoraProjectModel) -> Unit)?

    /**
     * Get the dockable panels a plugin contributes. Defaults to none; plugin hosts
     * that support multi-panel layouts can override this.
     */
    fun getPluginPanels(pluginId: String): List<PluginPanelDescriptor> = emptyList()

    /**
     * Get the Composable content for a specific panel contributed by a plugin.
     * Defaults to null (no panel content).
     */
    fun getPluginPanelContent(
        pluginId: String,
        panelId: String
    ): (@Composable (AzoraProjectModel) -> Unit)? = null

    /**
     * Get a loaded plugin instance by ID.
     *
     * @param pluginId The plugin ID
     * @return The AvePlugin instance, or null if not loaded
     */
    fun getLoadedPlugin(pluginId: String): AzoraPlugin?
}