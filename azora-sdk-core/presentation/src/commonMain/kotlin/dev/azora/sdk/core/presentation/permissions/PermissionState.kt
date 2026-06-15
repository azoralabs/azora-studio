package dev.azora.sdk.core.presentation.permissions

/**
 * Represents the possible states of a runtime permission.
 *
 * This enum is returned by [PermissionController.requestPermission] to indicate
 * the current status of a permission after a request has been made.
 *
 * ## State Transitions
 * ```
 * NOT_DETERMINED → (user grants) → GRANTED
 * NOT_DETERMINED → (user denies) → DENIED
 * DENIED → (user denies again with "don't ask") → PERMANENTLY_DENIED
 * ```
 *
 * ## Handling Each State
 * - **GRANTED**: Proceed with the feature requiring the permission
 * - **DENIED**: Show rationale explaining why the permission is needed
 * - **PERMANENTLY_DENIED**: Direct user to app settings to manually enable
 * - **NOT_DETERMINED**: Initial state before any request (rare in practice)
 *
 * @see PermissionController For requesting permissions
 * @see Permission For available permission types
 */
enum class PermissionState {

    /**
     * The user has granted the permission.
     *
     * The app can now use the feature that requires this permission.
     */
    GRANTED,

    /**
     * The user has denied the permission.
     *
     * The app should show a rationale explaining why the permission is needed.
     * A subsequent request may still show the system dialog.
     */
    DENIED,

    /**
     * The user has permanently denied the permission.
     *
     * On Android, this occurs when the user selects "Don't ask again".
     * On iOS, this occurs after the first denial (user must go to Settings).
     *
     * The app should direct the user to the system settings to manually
     * enable the permission.
     */
    PERMANENTLY_DENIED,

    /**
     * The permission status has not been determined yet.
     *
     * This is the initial state before any permission request has been made.
     * In practice, this state is rarely returned since [PermissionController.requestPermission]
     * triggers a request if the status is undetermined.
     */
    NOT_DETERMINED
}