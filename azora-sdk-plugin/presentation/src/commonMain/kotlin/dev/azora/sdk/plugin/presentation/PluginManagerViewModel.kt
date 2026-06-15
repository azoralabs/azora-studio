package dev.azora.sdk.plugin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing plugins in the Settings UI.
 */
class PluginManagerViewModel(
    private val pluginManager: PluginManager
) : ViewModel() {

    private val _state = MutableStateFlow(PluginManagerState())
    val state: StateFlow<PluginManagerState> = _state.asStateFlow()

    init {
        // Observe plugin manager state
        viewModelScope.launch {
            pluginManager.installedPlugins.collect { plugins ->
                _state.update { it.copy(installedPlugins = plugins, isLoading = false) }
            }
        }
    }

    /**
     * Load all installed plugins from disk.
     */
    fun loadPlugins() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                pluginManager.loadInstalledPlugins()
                // Explicitly set isLoading = false after completion
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to load plugins: ${e.message}", isLoading = false) }
            }
        }
    }

    /**
     * Install a plugin from a JAR file path.
     */
    fun installPlugin(jarPath: String) {
        viewModelScope.launch {
            try {
                val result = pluginManager.installPlugin(jarPath)
                if (result == null) {
                    _state.update { it.copy(error = "Failed to install plugin") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Installation error: ${e.message}") }
            }
        }
    }

    /**
     * Uninstall a plugin by ID.
     */
    fun uninstallPlugin(pluginId: String) {
        viewModelScope.launch {
            try {
                pluginManager.uninstallPlugin(pluginId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to uninstall: ${e.message}") }
            }
        }
    }

    /**
     * Toggle a plugin's enabled state.
     */
    fun togglePlugin(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    pluginManager.enablePlugin(pluginId)
                } else {
                    pluginManager.disablePlugin(pluginId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to toggle plugin: ${e.message}") }
            }
        }
    }

    /**
     * Clear any displayed error.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
