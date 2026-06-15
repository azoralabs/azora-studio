package dev.azora.sdk.core.io

/**
 * Cross-platform file system interface providing comprehensive file and directory operations
 */
expect class FileSystem {

    // ========== File Operations ==========

    /**
     * Writes content to a file at the specified path.
     * Creates parent directories if they don't exist.
     *
     * @param path Relative or absolute file path
     * @param content Content to write
     * @return Result of the write operation
     */
    suspend fun writeToFile(path: String, content: String): FileSystemResult

    /**
     * Reads content from a file at the specified path.
     *
     * @param path Relative or absolute file path
     * @return Result containing the file content or error
     */
    suspend fun readFromFile(path: String): FileReadResult

    /**
     * Appends content to an existing file or creates it if it doesn't exist.
     *
     * @param path Relative or absolute file path
     * @param content Content to append
     * @return Result of the append operation
     */
    suspend fun appendToFile(path: String, content: String): FileSystemResult

    /**
     * Deletes a file at the specified path.
     *
     * @param path Relative or absolute file path
     * @return Result of the delete operation
     */
    suspend fun deleteFile(path: String): FileSystemResult

    /**
     * Renames or moves a file.
     *
     * @param oldPath Current file path
     * @param newPath New file path
     * @return Result of the rename operation
     */
    suspend fun renameFile(oldPath: String, newPath: String): FileSystemResult

    /**
     * Copies a file to a new location.
     *
     * @param sourcePath Source file path
     * @param destinationPath Destination file path
     * @return Result of the copy operation
     */
    suspend fun copyFile(sourcePath: String, destinationPath: String): FileSystemResult

    /**
     * Checks if a file exists.
     *
     * @param path File path to check
     * @return Result indicating existence and type
     */
    fun fileExists(path: String): ExistsResult

    // ========== Directory Operations ==========

    /**
     * Creates a directory at the specified path.
     * Creates parent directories if they don't exist.
     *
     * @param path Directory path
     * @return Result of the create operation
     */
    suspend fun createDirectory(path: String): FileSystemResult

    /**
     * Deletes an empty directory.
     *
     * @param path Directory path
     * @return Result of the delete operation
     */
    suspend fun deleteDirectory(path: String): FileSystemResult

    /**
     * Deletes a directory and all its contents recursively.
     *
     * @param path Directory path
     * @return Result of the delete operation
     */
    suspend fun deleteDirectoryRecursively(path: String): FileSystemResult

    /**
     * Renames or moves a directory.
     *
     * @param oldPath Current directory path
     * @param newPath New directory path
     * @return Result of the rename operation
     */
    suspend fun renameDirectory(oldPath: String, newPath: String): FileSystemResult

    /**
     * Checks if a directory exists.
     *
     * @param path Directory path to check
     * @return Result indicating existence
     */
    fun directoryExists(path: String): ExistsResult

    /**
     * Lists all files and directories in the specified directory.
     *
     * @param path Directory path
     * @param recursive Whether to list contents recursively
     * @return Result containing list of files and directories
     */
    suspend fun listDirectory(path: String, recursive: Boolean = false): ListResult

    // ========== Path Operations ==========

    /**
     * Gets the absolute path for a relative path.
     *
     * @param relativePath Relative path
     * @return Absolute path
     */
    fun getAbsolutePath(relativePath: String): String

    /**
     * Gets the base directory used for relative paths.
     *
     * @return Base directory path
     */
    fun getBaseDirectory(): String

    /**
     * Checks if a path exists (file or directory).
     *
     * @param path Path to check
     * @return Result indicating existence and type
     */
    fun exists(path: String): ExistsResult

    /**
     * Sets a file as executable (Unix-like systems).
     *
     * @param path Path to the file
     * @return Result indicating success or failure
     */
    fun setExecutable(path: String): FileSystemResult
}