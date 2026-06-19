package dev.azora.sdk.plugin.presentation

import androidx.compose.runtime.Composable
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.plugin.core.*
import kotlinx.coroutines.flow.*

/**
 * No-op implementation of PluginManager for platforms that don't support dynamic plugin loading.
 * This includes Android, iOS, and WebAssembly targets.
 */
class NoOpPluginManager : PluginManager {

    override val installedPlugins: StateFlow<List<InstalledPlugin>> = MutableStateFlow(emptyList())

    override suspend fun loadInstalledPlugins() {
        // No-op: plugins not supported on this platform
    }

    override suspend fun discoverPlugins(): List<PluginManifest> = emptyList()

    override suspend fun installPlugin(sourcePath: String): InstalledPlugin? = null

    override suspend fun uninstallPlugin(pluginId: String) {
        // No-op
    }

    override suspend fun enablePlugin(pluginId: String) {
        // No-op
    }

    override suspend fun disablePlugin(pluginId: String) {
        // No-op
    }

    override fun getPluginContent(pluginId: String): (@Composable (PluginContext) -> Unit)? =
        null

    override fun getLoadedPlugin(pluginId: String): AzoraPlugin? = null
}