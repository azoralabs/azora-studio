package dev.azora.sdk.core.io

/**
 * Result of a file/directory existence check
 */
sealed class ExistsResult {

    /**
     * Path exists
     */
    data class Exists(val path: String, val isDirectory: Boolean) : ExistsResult()

    /**
     * Path does not exist
     */
    data class NotExists(val path: String) : ExistsResult()

    /**
     * Check failed with error details
     */
    data class Error(val message: String, val exception: Throwable? = null) : ExistsResult()
}