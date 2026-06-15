package dev.azora.sdk.core.io

/**
 * Result of a directory listing operation
 */
sealed class ListResult {

    /**
     * Successful listing
     */
    data class Success(
        val files: List<FileInfo>,
        val path: String
    ) : ListResult()

    /**
     * Listing failed with error details
     */
    data class Error(val message: String, val exception: Throwable? = null) : ListResult()
}