package dev.azora.sdk.plugin.core

import androidx.compose.runtime.Composable
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectTemplateContribution

/**
 * Interface that all Azora plugins must implement.
 *
 * Plugins are loaded from external JAR files and can appear as:
 * - Tabs in Azora Engine
 * - Standalone applications in Azora Launcher
 *
 * Each plugin provides a Composable content that renders when activated, and may additionally
 * contribute [project templates][projectTemplates] (e.g. a Website builder plugin contributes the
 * Website/Kobweb template).
 */
interface AzoraPlugin {

    /**
     * Plugin manifest containing metadata.
     */
    val metadata: PluginManifest

    /**
     * The main content displayed when the plugin is activated for the open project.
     *
     * @param context Host context exposing the current project, its path, a file system, a logger,
     *   and a [PluginContext.saveProject] hook for persisting plugin edits.
     */
    @Composable
    fun Content(context: PluginContext)

    /**
     * Project templates contributed by this plugin. The host surfaces them in the create-project UI
     * (only when the plugin is installed/enabled) and dispatches generation to [ProjectTemplateContribution.generator].
     * Defaults to none.
     */
    fun projectTemplates(): List<ProjectTemplateContribution> = emptyList()

    /**
     * Called when the plugin is loaded/enabled.
     * Override to perform initialization.
     */
    fun onLoad() {}

    /**
     * Called when the plugin is unloaded/disabled.
     * Override to perform cleanup.
     */
    fun onUnload() {}

    /**
     * Handle a named action (e.g. "build", "run", "clean", "hot_reload") for the
     * given project. Defaults to a no-op; plugins that contribute actions override this.
     *
     * @param action The action identifier.
     * @param project The currently open project.
     */
    fun handleAction(action: String, project: AzoraProjectModel) {}
}
