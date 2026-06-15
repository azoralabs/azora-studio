package dev.azora.sdk.plugin.core

import androidx.compose.runtime.Composable
import dev.azora.sdk.core.project.domain.AzoraProjectModel

/**
 * Interface that all Azora plugins must implement.
 *
 * Plugins are loaded from external JAR files and can appear as:
 * - Tabs in Azora Engine
 * - Standalone applications in Azora Launcher
 *
 * Each plugin provides a Composable content that renders when activated.
 */
interface AzoraPlugin {

    /**
     * Plugin manifest containing metadata.
     */
    val metadata: PluginManifest

    /**
     * The main content displayed when the plugin tab is selected.
     *
     * @param project The currently open project
     */
    @Composable
    fun Content(project: AzoraProjectModel)

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
}