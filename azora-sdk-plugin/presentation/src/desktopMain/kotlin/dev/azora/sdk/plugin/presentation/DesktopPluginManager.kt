package dev.azora.sdk.plugin.presentation

import androidx.compose.runtime.Composable
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectTemplateContribution
import dev.azora.sdk.plugin.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Desktop implementation of PluginManager using URLClassLoader for dynamic JAR loading.
 */
class DesktopPluginManager : PluginManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val pluginDir: File by lazy {
        val homeDir = System.getProperty("user.home")
        File(homeDir, ".azora/plugins").also { it.mkdirs() }
    }

    private val installedJsonFile: File by lazy {
        File(pluginDir, "installed.json")
    }

    private val _installedPlugins = MutableStateFlow<List<InstalledPlugin>>(emptyList())
    override val installedPlugins: StateFlow<List<InstalledPlugin>> = _installedPlugins.asStateFlow()

    private val loadedPlugins = mutableMapOf<String, LoadedPlugin>()

    override suspend fun loadInstalledPlugins() = withContext(Dispatchers.IO) {
        try {
            var installed = readInstalledJson()

            // Auto-discover plugins in directory that aren't in installed.json
            val discoveredManifests = discoverPlugins()
            val installedIds = installed.map { it.id }.toSet()

            discoveredManifests.forEach { manifest ->
                if (manifest.id !in installedIds) {
                    // Find the JAR file for this manifest
                    val jarFile = pluginDir.listFiles { file -> file.extension == "jar" }
                        ?.find { readManifestFromJar(it)?.id == manifest.id }

                    if (jarFile != null) {
                        // Read icon XML from JAR
                        val iconXml = readIconFromJar(jarFile, manifest.iconPath)

                        val newPlugin = InstalledPlugin.fromManifest(
                            manifest = manifest,
                            jarFileName = jarFile.name,
                            iconXml = iconXml,
                            enabled = false
                        )
                        installed = installed + newPlugin
                        println("Auto-discovered plugin: ${manifest.name}")
                    }
                }
            }

            // Save updated list if we discovered new plugins
            if (installed.size > installedIds.size) {
                writeInstalledJson(installed)
            }

            _installedPlugins.value = installed

            // Load enabled plugins
            installed.filter { it.enabled }.forEach { plugin ->
                try {
                    loadPluginJar(plugin)
                } catch (e: Exception) {
                    println("Failed to load plugin ${plugin.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error in loadInstalledPlugins: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun discoverPlugins(): List<PluginManifest> = withContext(Dispatchers.IO) {
        val manifests = mutableListOf<PluginManifest>()
        pluginDir.listFiles { file -> file.extension == "jar" }?.forEach { jarFile ->
            try {
                readManifestFromJar(jarFile)?.let { manifests.add(it) }
            } catch (e: Exception) {
                println("Failed to read manifest from ${jarFile.name}: ${e.message}")
            }
        }
        manifests
    }

    override suspend fun installPlugin(sourcePath: String): InstalledPlugin? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || sourceFile.extension != "jar") {
                println("Invalid plugin file: $sourcePath")
                return@withContext null
            }

            // Read manifest from source JAR
            val manifest = readManifestFromJar(sourceFile) ?: run {
                println("No plugin.json found in JAR: $sourcePath")
                return@withContext null
            }

            // Check if already installed
            val existing = _installedPlugins.value.find { it.id == manifest.id }
            if (existing != null) {
                println("Plugin ${manifest.id} is already installed")
                return@withContext null
            }

            // Copy JAR to plugin directory
            val targetFile = File(pluginDir, "${manifest.id}-${manifest.version}.jar")
            sourceFile.copyTo(targetFile, overwrite = true)

            // Create installed plugin entry
            val installedPlugin = InstalledPlugin.fromManifest(
                manifest = manifest,
                jarFileName = targetFile.name,
                enabled = false
            )

            // Update installed list
            val newList = _installedPlugins.value + installedPlugin
            _installedPlugins.value = newList
            writeInstalledJson(newList)

            installedPlugin
        } catch (e: Exception) {
            println("Failed to install plugin: ${e.message}")
            null
        }
    }

    override suspend fun uninstallPlugin(pluginId: String) = withContext(Dispatchers.IO) {
        // Unload if loaded
        unloadPluginJar(pluginId)

        // Find and remove
        val plugin = _installedPlugins.value.find { it.id == pluginId } ?: return@withContext

        // Delete JAR file
        val jarFile = File(pluginDir, plugin.jarFileName)
        if (jarFile.exists()) {
            jarFile.delete()
        }

        // Update installed list
        val newList = _installedPlugins.value.filter { it.id != pluginId }
        _installedPlugins.value = newList
        writeInstalledJson(newList)
    }

    override suspend fun enablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = _installedPlugins.value.find { it.id == pluginId } ?: return@withContext

        // Load the plugin JAR
        try {
            loadPluginJar(plugin)

            // Update enabled state
            val newList = _installedPlugins.value.map {
                if (it.id == pluginId) it.copy(enabled = true) else it
            }
            _installedPlugins.value = newList
            writeInstalledJson(newList)
        } catch (e: Exception) {
            println("Failed to enable plugin $pluginId: ${e.message}")
        }
    }

    override suspend fun disablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        // Unload the plugin
        unloadPluginJar(pluginId)

        // Update enabled state
        val newList = _installedPlugins.value.map {
            if (it.id == pluginId) it.copy(enabled = false) else it
        }
        _installedPlugins.value = newList
        writeInstalledJson(newList)
    }

    override fun getPluginContent(pluginId: String): (@Composable (PluginContext) -> Unit)? {
        val loaded = loadedPlugins[pluginId] ?: return null
        return { context -> loaded.plugin.Content(context) }
    }

    override fun getPluginPanels(pluginId: String): List<PluginPanelDescriptor> {
        val loaded = loadedPlugins[pluginId] ?: return emptyList()
        return runCatching { loaded.plugin.panels() }.getOrElse { emptyList() }
    }

    override fun getPluginPanelContent(
        pluginId: String,
        panelId: String
    ): (@Composable (PluginContext) -> Unit)? {
        val loaded = loadedPlugins[pluginId] ?: return null
        val hasPanel = runCatching { loaded.plugin.panels().any { it.id == panelId } }.getOrDefault(false)
        if (!hasPanel) return null
        return { context -> loaded.plugin.PanelContent(panelId, context) }
    }

    override fun templateContributions(): List<ProjectTemplateContribution> =
        loadedPlugins.values.flatMap { runCatching { it.plugin.projectTemplates() }.getOrElse { emptyList() } }

    override fun getLoadedPlugin(pluginId: String): AzoraPlugin? {
        return loadedPlugins[pluginId]?.plugin
    }

    // Private helpers

    private fun loadPluginJar(installedPlugin: InstalledPlugin) {
        val jarFile = File(pluginDir, installedPlugin.jarFileName)
        if (!jarFile.exists()) {
            throw IllegalStateException("Plugin JAR not found: ${jarFile.absolutePath}")
        }

        // Read manifest to get main class
        val manifest = readManifestFromJar(jarFile)
            ?: throw IllegalStateException("No manifest found in plugin JAR")

        // Create class loader
        val classLoader = URLClassLoader(
            arrayOf(jarFile.toURI().toURL()),
            this::class.java.classLoader
        )

        // Load plugin class
        val pluginClass = classLoader.loadClass(manifest.mainClass)
        val plugin = pluginClass.getDeclaredConstructor().newInstance() as AzoraPlugin

        // Call onLoad
        plugin.onLoad()

        // Store loaded plugin
        loadedPlugins[installedPlugin.id] = LoadedPlugin(
            plugin = plugin,
            classLoader = classLoader,
            manifest = manifest
        )
    }

    private fun unloadPluginJar(pluginId: String) {
        val loaded = loadedPlugins.remove(pluginId) ?: return

        try {
            loaded.plugin.onUnload()
            loaded.classLoader.close()
        } catch (e: Exception) {
            println("Error unloading plugin $pluginId: ${e.message}")
        }
    }

    private fun readManifestFromJar(jarFile: File): PluginManifest? {
        return try {
            JarFile(jarFile).use { jar ->
                val entry = jar.getJarEntry("plugin.json") ?: return null
                val content = jar.getInputStream(entry).bufferedReader().readText()
                json.decodeFromString<PluginManifest>(content)
            }
        } catch (e: Exception) {
            println("Error reading manifest from ${jarFile.name}: ${e.message}")
            null
        }
    }

    private fun readIconFromJar(jarFile: File, iconPath: String): String? {
        return try {
            JarFile(jarFile).use { jar ->
                val entry = jar.getJarEntry(iconPath) ?: return null
                jar.getInputStream(entry).bufferedReader().readText()
            }
        } catch (e: Exception) {
            println("Error reading icon from ${jarFile.name}/$iconPath: ${e.message}")
            null
        }
    }

    private fun readInstalledJson(): List<InstalledPlugin> {
        return try {
            if (installedJsonFile.exists()) {
                json.decodeFromString<List<InstalledPlugin>>(installedJsonFile.readText())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error reading installed.json: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeInstalledJson(plugins: List<InstalledPlugin>) {
        try {
            val content = json.encodeToString<List<InstalledPlugin>>(plugins)
            installedJsonFile.writeText(content)
        } catch (e: Exception) {
            println("Error writing installed.json: ${e.message}")
            e.printStackTrace()
        }
    }

    private data class LoadedPlugin(
        val plugin: AzoraPlugin,
        val classLoader: URLClassLoader,
        val manifest: PluginManifest
    )
}