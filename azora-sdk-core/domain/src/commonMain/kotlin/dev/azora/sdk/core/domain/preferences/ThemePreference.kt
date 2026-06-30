package dev.azora.sdk.core.domain.preferences

/**
 * Represents the available theme preferences for the application.
 *
 * Used to determine how the app's UI should be displayed
 * based on the user's selected appearance setting.
 */
enum class ThemePreference {

    /** Always use the light theme, regardless of system settings. */
    LIGHT,

    /** Always use the dark theme, regardless of system settings. */
    DARK,

    /** Automatically match the device's current system theme. */
    SYSTEM
}