package dev.azora.sdk.plugin.core

import kotlinx.serialization.Serializable

/**
 * Plugin manifest read from plugin.json inside the plugin JAR.
 */
@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String = "",
    val mainClass: String,
    val accentColor: String = "#FF9C27B0",
    val iconPath: String = "icon.xml",
    val minAppVersion: String = "1.0.0",
    val category: PluginCategory = PluginCategory.TOOL,
    val tags: List<String> = emptyList()
)