package dev.azora.sdk.plugin.presentation

import dev.azora.sdk.plugin.core.InstalledPlugin

/**
 * UI state for the plugin manager.
 */
data class PluginManagerState(
    val isLoading: Boolean = true,
    val installedPlugins: List<InstalledPlugin> = emptyList(),
    val error: String? = null
) {

    val enabledPlugins: List<InstalledPlugin>
        get() = installedPlugins.filter { it.enabled }
}
