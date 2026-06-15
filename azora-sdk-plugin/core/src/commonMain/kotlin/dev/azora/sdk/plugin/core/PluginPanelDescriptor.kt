package dev.azora.sdk.plugin.core

/**
 * Describes a dockable panel contributed by a plugin.
 *
 * @property id Stable panel identifier (unique within the plugin).
 * @property title Human-readable panel title shown in the dock tab.
 * @property group Optional group name; panels sharing a group are laid out together.
 * @property minimumWidth Minimum panel width in dp.
 * @property minimumHeight Minimum panel height in dp.
 */
data class PluginPanelDescriptor(
    val id: String,
    val title: String,
    val group: String? = null,
    val minimumWidth: Float = 0f,
    val minimumHeight: Float = 0f
)
