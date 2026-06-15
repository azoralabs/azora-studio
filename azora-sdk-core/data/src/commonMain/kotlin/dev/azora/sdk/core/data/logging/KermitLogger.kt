package dev.azora.sdk.core.data.logging

import co.touchlab.kermit.Logger
import dev.azora.sdk.core.domain.logging.AzoraLogger

/**
 * A [dev.azora.sdk.core.domain.logging.AzoraLogger] implementation that delegates logging operations
 * to the [KermitLogger]. This provides a consistent logging interface
 * for the Chirp framework or library.
 *
 * Kermit is a multiplatform logging library that supports various targets,
 * making it suitable for cross-platform Kotlin projects.
 */
object KermitLogger : dev.azora.sdk.core.domain.logging.AzoraLogger {

    /**
     * Logs a debug-level message.
     *
     * Use this for verbose information that may be useful during development
     * or debugging but not necessarily in production.
     *
     * @param message The message to log.
     */
    override fun debug(message: String) = Logger.d(message)

    /**
     * Logs an info-level message.
     *
     * Use this for general information about the application's normal operation.
     *
     * @param message The message to log.
     */
    override fun info(message: String) = Logger.i(message)

    /**
     * Logs a warning-level message.
     *
     * Use this for potentially harmful situations or unexpected states
     * that are not yet errors.
     *
     * @param message The message to log.
     */
    override fun warn(message: String) = Logger.w(message)

    /**
     * Logs an error-level message, optionally with a throwable.
     *
     * Use this for exceptions or critical issues that require attention.
     *
     * @param message The error message to log.
     * @param throwable An optional [Throwable] providing additional context for the error.
     */
    override fun error(message: String, throwable: Throwable?) = Logger.e(message, throwable)
}