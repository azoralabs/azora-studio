package dev.azora.sdk.core.presentation.permissions

/**
 * Enum representing runtime permissions that the application may request from the user.
 *
 * These permissions are managed through the [PermissionController] and require
 * user consent on platforms that enforce runtime permission models (Android, iOS).
 */
enum class Permission {

    /**
     * Permission to send push notifications to the user.
     *
     * Required for:
     * - Sending updates
     * - Notifying users of system messages
     * - Delivering time-sensitive alerts
     */
    NOTIFICATIONS,

    /**
     * Permission to access the device's location.
     *
     * Required for:
     * - Google maps
     * - Access Google location
     * - Geocoding address
     */
    LOCATION
}