package dev.azora.sdk.core.presentation.lifecycle

/**
 * Platform-specific controller for minimizing the application window.
 *
 * This expect class provides a unified API for minimizing the app across platforms.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val appMinimizer = rememberAppMinimizer()
 *
 *     Button(onClick = { appMinimizer.minimize() }) {
 *         Text("Minimize App")
 *     }
 * }
 * ```
 *
 * ## Platform Behavior
 * | Platform | Behavior |
 * |----------|----------|
 * | Android  | App moves to background, user sees home screen |
 * | iOS      | No effect (logged to console) |
 * | Desktop  | Window minimizes to taskbar/dock |
 * | Web      | Not supported |
 *
 * @see rememberAppMinimizer Factory function to create platform-specific instances
 */
expect class AppMinimizer() {

    /**
     * Minimizes the application window or moves the app to the background.
     *
     * On Android, this moves the app's task to the back of the activity stack.
     * On Desktop, this minimizes the window to the taskbar/dock.
     * On iOS, this is a no-op as programmatic minimization is not allowed.
     */
    fun minimize()
}