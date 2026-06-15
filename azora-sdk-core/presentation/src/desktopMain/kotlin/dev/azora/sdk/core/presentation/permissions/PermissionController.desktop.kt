package dev.azora.sdk.core.presentation.permissions

/**
 * Desktop (JVM) implementation of [PermissionController].
 *
 * This is a no-op implementation because desktop platforms (Windows, macOS, Linux)
 * do not have a runtime permission model like mobile platforms.
 *
 * ## Desktop Permission Model
 * On desktop platforms:
 * - Notifications are typically allowed by default (user manages via OS settings)
 * - Location access may require OS-level permission (handled outside the app)
 * - File system and camera access are handled at the OS level, not at runtime
 *
 * ## Behavior
 * All permission requests immediately return [PermissionState.GRANTED] to allow
 * the calling code to proceed with the requested functionality.
 *
 * @see PermissionController The common interface this implements
 */
actual class PermissionController {

    /**
     * Always returns [PermissionState.GRANTED] on desktop platforms.
     *
     * Desktop platforms don't have a runtime permission model, so this
     * implementation immediately grants all permissions. The actual
     * permission handling (if any) is done at the OS level.
     *
     * @param permission The requested permission (ignored on desktop)
     * @return Always [PermissionState.GRANTED]
     */
    actual suspend fun requestPermission(permission: Permission) = PermissionState.GRANTED
}