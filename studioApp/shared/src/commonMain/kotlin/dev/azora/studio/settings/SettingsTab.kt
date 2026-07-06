package dev.azora.studio.settings

/**
 * Available tabs in the settings panel.
 */
enum class SettingsTab(val label: String) {
    GENERAL("General"),
    THEME("Theme"),
    AZORA_SCRIPT("Azora Script"),
    SCENE_STUDIO("Scene Settings"),
    PLUGINS("Plugins"),
    LIBRARIES("Libraries")
}
