package dev.azora.sdk.core.domain.logging

/**
 * No-op implementation of [AzoraLogger].
 *
 * This logger intentionally performs no logging and discards all messages.
 * It is useful in the following scenarios:
 * - Unit and integration testing
 * - UI previews where dependency injection is required
 * - Environments where logging is not desired or must be disabled
 * - Dependency injection as a default or fallback logger
 * - Performance-sensitive code paths where logging overhead should be avoided
 *
 * Since this implementation does nothing, it is safe to use across all platforms
 * without causing side effects or requiring platform-specific dependencies.
 */
class FakeLogger : AzoraLogger {

    /**
     * Ignores the provided debug message.
     *
     * @param message The debug message (discarded)
     */
    override fun debug(message: String) {}

    /**
     * Ignores the provided informational message.
     *
     * @param message The informational message (discarded)
     */
    override fun info(message: String) {}

    /**
     * Ignores the provided warning message.
     *
     * @param message The warning message (discarded)
     */
    override fun warn(message: String) {}

    /**
     * Ignores the provided error message and optional throwable.
     *
     * @param message The error message (discarded)
     * @param throwable Optional exception or error (discarded)
     */
    override fun error(message: String, throwable: Throwable?) {}
}