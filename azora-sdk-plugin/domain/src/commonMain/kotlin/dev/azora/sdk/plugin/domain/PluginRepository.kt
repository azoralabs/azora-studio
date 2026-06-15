package dev.azora.sdk.plugin.domain

import dev.azora.sdk.plugin.core.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for plugin management operations.
 */
interface PluginRepository {

    /**
     * StateFlow of all installed plugins.
     */
    val installedPlugins: StateFlow<List<InstalledPlugin>>

    /**
     * Initialize and load all installed plugins from disk.
     */
    suspend fun loadInstalledPlugins()

    /**
     * Discover plugins in the plugin directory without loading them.
     *
     * @return List of discovered plugin manifests
     */
    suspend fun discoverPlugins(): List<PluginManifest>

    /**
     * Install a plugin from a JAR file path.
     *
     * @param jarPath Path to the plugin JAR file
     * @return Result containing the installed plugin or an error
     */
    suspend fun installPlugin(jarPath: String): Result<InstalledPlugin>

    /**
     * Uninstall a plugin by ID.
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
     * Get the plugin directory path.
     */
    fun getPluginDirectory(): String
}
