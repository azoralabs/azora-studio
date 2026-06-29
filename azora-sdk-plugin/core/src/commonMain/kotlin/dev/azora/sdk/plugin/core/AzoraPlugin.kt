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
     * Settings tabs this plugin contributes, surfaced by the host in its Settings screen alongside the
     * built-in tabs. Return empty (the default) to contribute none. The host stays agnostic of tab
     * content — [settingsTabContent] renders it.
     */
    fun settingsTabs(): List<SettingsTabDescriptor> = emptyList()

    /**
     * Renders the content of the settings tab identified by [tabId] (one of the ids returned by
     * [settingsTabs]). Only invoked for plugins that contribute settings tabs; the default is empty.
     * The plugin receives [context] so it can read/write the project (e.g. via
     * [PluginContext.saveProject]) and access the file system.
     */
    @Composable
    fun settingsTabContent(tabId: String, context: PluginContext) {}

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

    /**
     * The set of `.azscene` document *types* this plugin provides an editor for.
     *
     * `.azscene` is a generic Azora file: the host reads its top-level `type` field and routes the
     * file to whichever plugin lists that type here. The host itself stays agnostic of what the
     * types mean. Defaults to none.
     */
    fun azsceneEditorTypes(): Set<String> = emptySet()

    /**
     * The `.azscene` document types this plugin can *create*, with human labels. The host surfaces
     * these in its "New …" menu (e.g. "New Website Page") so the user picks a type when creating a
     * scene file; the host stays agnostic of what the labels mean. Defaults to none.
     */
    fun azsceneTemplates(): List<AzsceneTemplate> = emptyList()

    /**
     * The initial file content for a new `.azscene` document of [type] (one of [azsceneTemplates]).
     * The host writes this verbatim to the new file. Returns null if the plugin doesn't own [type].
     */
    fun newAzsceneContent(type: String): String? = null

    /**
     * Renders the editor for an `.azscene` file of one of [azsceneEditorTypes]. The host opens the
     * file's dock panel and calls this; the plugin owns loading, saving and any side effects (it has
     * the file system, project and [PluginContext.saveProject] via [context]).
     *
     * @param type The document's `type` (one of [azsceneEditorTypes]).
     * @param filePath Absolute path of the `.azscene` file to edit.
     * @param context Host context for this render.
     */
    @Composable
    fun AzsceneEditor(type: String, filePath: String, context: PluginContext) {}
}
