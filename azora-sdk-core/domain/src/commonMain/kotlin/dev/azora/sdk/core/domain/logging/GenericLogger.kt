package dev.azora.sdk.core.domain.logging

/**
 * Defines a generic logging interface.
 *
 * This interface abstracts the underlying logging implementation, allowing
 * different loggers (e.g., Kermit, Timber, Console, etc.) to be plugged in
 * without changing the rest of the codebase.
 *
 * Each method corresponds to a standard log level.
 */
interface AzoraLogger {

    /**
     * Logs a debug-level message.
     *
     * Use this for detailed diagnostic information that can help during
     * development or troubleshooting but is typically disabled in production builds.
     *
     * @param message The message to log.
     */
    fun debug(message: String)

    /**
     * Logs an info-level message.
     *
     * Use this for general operational messages that highlight the progress
     * of the application at a coarse-grained level.
     *
     * @param message The message to log.
     */
    fun info(message: String)

    /**
     * Logs a warning-level message.
     *
     * Use this for potentially problematic situations that are not necessarily
     * errors but may require attention.
     *
     * @param message The message to log.
     */
    fun warn(message: String)

    /**
     * Logs an error-level message, optionally including an exception.
     *
     * Use this for issues that cause failures or require immediate attention.
     *
     * @param message The error message to log.
     * @param throwable An optional [Throwable] providing additional details about the error.
     */
    fun error(message: String, throwable: Throwable? = null)
}