package dev.azora.sdk.core.io

/**
 * Result types for file system operations
 */
sealed class FileSystemResult {

    /**
     * Successful operation with path
     */
    data class Success(val path: String) : FileSystemResult()

    /**
     * Operation failed with error details
     */
    data class Error(val message: String, val exception: Throwable? = null) : FileSystemResult()
}