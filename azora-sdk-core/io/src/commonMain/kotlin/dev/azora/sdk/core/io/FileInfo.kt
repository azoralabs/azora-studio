package dev.azora.sdk.core.io

/**
 * Information about a file or directory
 */
data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)