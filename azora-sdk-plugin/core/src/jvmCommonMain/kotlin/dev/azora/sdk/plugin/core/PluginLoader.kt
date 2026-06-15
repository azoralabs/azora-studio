package dev.azora.sdk.plugin.core

import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * Loads Azora plugins from JAR files.
 */
actual object PluginLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Load all plugins from a directory.
     *
     * @param pluginsDir Directory containing plugin JAR files
     * @return List of loaded plugins
     */
    fun loadPlugins(pluginsDir: File): List<AzoraPlugin> {
        if (!pluginsDir.exists() || !pluginsDir.isDirectory) {
            return emptyList()
        }

        return pluginsDir.listFiles { file ->
            file.extension == "jar"
        }?.mapNotNull { jarFile ->
            try {
                loadPlugin(jarFile)
            } catch (e: Exception) {
                println("Failed to load plugin from ${jarFile.name}: ${e.message}")
                null
            }
        } ?: emptyList()
    }

    /**
     * Load a single plugin from a JAR file.
     *
     * @param jarFile The plugin JAR file
     * @return The loaded plugin, or null if loading fails
     */
    fun loadPlugin(jarFile: File): AzoraPlugin? {
        val metadata = loadMetadata(jarFile) ?: return null

        val classLoader = URLClassLoader(
            arrayOf(jarFile.toURI().toURL()),
            AzoraPlugin::class.java.classLoader
        )

        val pluginClass = classLoader.loadClass(metadata.mainClass)

        // Try to get Kotlin object instance first
        val instance = try {
            pluginClass.kotlin.objectInstance as? AzoraPlugin
        } catch (e: Exception) {
            null
        } ?: try {
            // Fall back to instantiation
            pluginClass.getDeclaredConstructor().newInstance() as? AzoraPlugin
        } catch (e: Exception) {
            println("Failed to instantiate plugin ${metadata.name}: ${e.message}")
            null
        }

        instance?.onLoad()
        return instance
    }

    /**
     * Load plugin manifest from a JAR file.
     */
    fun loadMetadata(jarFile: File): PluginManifest? {
        return try {
            JarFile(jarFile).use { jar ->
                // Try plugin.json first
                var entry = jar.getEntry("plugin.json")

                // Fall back to manifest attribute
                if (entry == null) {
                    val manifest = jar.manifest
                    val mainClass = manifest?.mainAttributes?.getValue("Plugin-Class")
                    if (mainClass != null) {
                        return@use PluginManifest(
                            id = jarFile.nameWithoutExtension,
                            name = jarFile.nameWithoutExtension.replaceFirstChar { it.uppercase() },
                            version = "1.0.0",
                            description = "Plugin loaded from ${jarFile.name}",
                            mainClass = mainClass
                        )
                    }
                    return@use null
                }

                jar.getInputStream(entry).bufferedReader().use { reader ->
                    json.decodeFromString<PluginManifest>(reader.readText())
                }
            }
        } catch (e: Exception) {
            println("Failed to load metadata from ${jarFile.name}: ${e.message}")
            null
        }
    }

    /**
     * Get the plugins directory path.
     */
    fun getPluginsDirectory(): File {
        return File(System.getProperty("user.home"), ".azora/plugins").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }
}