package dev.azora.sdk.plugin.core

/**
 * State of a loaded plugin instance.
 */
data class LoadedPlugin(
    val installed: InstalledPlugin,
    val instance: AzoraPlugin,
    val loader: PlatformPluginLoader? = null
)
