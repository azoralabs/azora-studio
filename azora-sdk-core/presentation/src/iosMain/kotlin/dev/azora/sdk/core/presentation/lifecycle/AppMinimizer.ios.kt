package dev.azora.sdk.core.presentation.lifecycle

/**
 * iOS implementation of [AppMinimizer].
 *
 * iOS does not permit an app to background itself programmatically (Apple rejects apps that try),
 * so [minimize] is intentionally a no-op. The user backgrounds the app via the Home gesture.
 */
actual class AppMinimizer actual constructor() {
    actual fun minimize() {
        // No-op: iOS provides no public API for an app to minimize itself.
    }
}
