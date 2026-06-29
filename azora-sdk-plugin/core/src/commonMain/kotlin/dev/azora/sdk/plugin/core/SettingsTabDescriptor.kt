package dev.azora.sdk.plugin.core

/**
 * Describes a settings tab contributed by a plugin, surfaced by the host in its Settings screen
 * alongside the host's built-in tabs. The host stays agnostic of the tab's content — the plugin
 * renders it via [AzoraPlugin.settingsTabContent].
 *
 * @property id Stable tab identifier (unique within the plugin). The host keys plugin tab content by
 *   `pluginId + id`.
 * @property label Human-readable tab label shown in the settings sidebar.
 */
data class SettingsTabDescriptor(
    val id: String,
    val label: String
)
