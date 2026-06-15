package dev.azora.sdk.core.io

/**
 * Result of a read operation
 */
sealed class FileReadResult {

    /**
     * Successful read with content
     */
    data class Success(val content: String, val path: String) : FileReadResult()

    /**
     * Read failed with error details
     */
    data class Error(val message: String, val exception: Throwable? = null) : FileReadResult()
}