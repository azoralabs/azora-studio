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
     * Used by hosts that surface a plugin as a single tab, and as the fallback when a plugin
     * contributes no [panels]. Multi-tab plugins still implement this (e.g. as a hub or their primary
     * panel) so single-tab hosts remain functional.
     *
     * @param context Host context exposing the current project, its path, a file system, a logger,
     *   and a [PluginContext.saveProject] hook for persisting plugin edits.
     */
    @Composable
    fun Content(context: PluginContext)

    /**
     * The dockable panels this plugin contributes, each surfaced by the host as its own tab. Return
     * an empty list (the default) to appear as a single [Content] tab instead.
     *
     * Panels with the same [PluginPanelDescriptor.group] are laid out together; ungrouped panels
     * each become an independent tab.
     */
    fun panels(): List<PluginPanelDescriptor> = emptyList()

    /**
     * Renders the content of the panel identified by [panelId] (one of the ids returned by [panels]).
     * Only invoked for plugins that contribute [panels]; the default is empty.
     *
     * @param panelId The id of the panel to render.
     * @param context Host context (see [Content]).
     */
    @Composable
    fun PanelContent(panelId: String, context: PluginContext) {}

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
