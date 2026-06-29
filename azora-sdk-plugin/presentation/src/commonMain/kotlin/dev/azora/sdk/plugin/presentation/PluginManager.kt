package dev.azora.sdk.plugin.presentation

import androidx.compose.runtime.Composable
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectTemplateContribution
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
     * @return A Composable function taking the host [PluginContext], or null if the plugin is not loaded
     */
    fun getPluginContent(pluginId: String): (@Composable (PluginContext) -> Unit)?

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
    ): (@Composable (PluginContext) -> Unit)? = null

    /**
     * Resolve the editor for an `.azscene` document [type] (read from the file's top-level `type`
     * field) to whichever loaded plugin registered for it via `AzoraPlugin.azsceneEditorTypes()`.
     * The returned content edits [filePath]. Returns null when no plugin handles the type.
     */
    fun getAzsceneEditor(type: String, filePath: String): (@Composable (PluginContext) -> Unit)? = null

    /** Creatable `.azscene` types across all loaded plugins, for the host's "New …" menu. */
    fun azsceneTemplates(): List<dev.azora.sdk.plugin.core.AzsceneTemplate> = emptyList()

    /** Initial content for a new `.azscene` document of [type], from the plugin that owns it. */
    fun newAzsceneContent(type: String): String? = null

    /**
     * Project templates contributed by all currently loaded (enabled) plugins. The host surfaces
     * these in the create-project UI and resolves generators through them.
     */
    fun templateContributions(): List<ProjectTemplateContribution> = emptyList()

    /**
     * Settings tabs contributed by all currently loaded (enabled) plugins, for the host's Settings
     * screen. Each entry pairs the contributing plugin's id with its [SettingsTabDescriptor].
     */
    fun getSettingsTabs(): List<Pair<String, SettingsTabDescriptor>> = emptyList()

    /**
     * Renders the content of a plugin-contributed settings tab ([pluginId], [tabId]). Returns null
     * when the plugin/tab is not found.
     */
    fun getSettingsTabContent(
        pluginId: String,
        tabId: String,
        context: PluginContext
    ): (@Composable () -> Unit)? = null

    /**
     * Get a loaded plugin instance by ID.
     *
     * @param pluginId The plugin ID
     * @return The AvePlugin instance, or null if not loaded
     */
    fun getLoadedPlugin(pluginId: String): AzoraPlugin?
}