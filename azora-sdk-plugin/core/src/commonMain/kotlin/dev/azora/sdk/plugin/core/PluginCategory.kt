package dev.azora.sdk.plugin.core

import kotlinx.serialization.Serializable

/**
 * Plugin categories for organization in the UI.
 */
@Serializable
enum class PluginCategory {
    TOOL,           // General purpose tools
    EDITOR,         // Content editors (like Pixie)
    GENERATOR,      // Content generators
    INTEGRATION,    // External service integrations
    UTILITY,        // Utility plugins
    THEME           // Visual themes
}