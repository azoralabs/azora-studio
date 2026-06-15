package dev.azora.sdk.plugin.core

import kotlinx.serialization.Serializable

/**
 * Represents an installed plugin with its enabled state.
 */
@Serializable
data class InstalledPlugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val accentColor: String,
    val iconXml: String? = null,
    val jarFileName: String,
    val enabled: Boolean = false
) {
    companion object {
        fun fromManifest(
            manifest: PluginManifest,
            jarFileName: String,
            iconXml: String? = null,
            enabled: Boolean = false
        ) = InstalledPlugin(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            description = manifest.description,
            author = manifest.author,
            accentColor = manifest.accentColor,
            iconXml = iconXml,
            jarFileName = jarFileName,
            enabled = enabled
        )
    }
}