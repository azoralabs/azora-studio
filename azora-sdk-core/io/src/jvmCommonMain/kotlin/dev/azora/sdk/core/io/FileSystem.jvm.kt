package dev.azora.sdk.core.io

import kotlinx.coroutines.*
import java.io.File

actual class FileSystem {

    private val baseDir = File(System.getProperty("user.home") + "/Documents")

    init {
        baseDir.mkdirs()
    }

    private fun getFile(path: String): File {
        // If path is absolute, use it directly; otherwise resolve against baseDir
        val file = File(path)
        return if (file.isAbsolute) file else File(baseDir, path)
    }

    // ========== File Operations ==========

    actual suspend fun writeToFile(path: String, content: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile(path)
                file.parentFile?.mkdirs()
                file.writeText(content)
                FileSystemResult.Success(file.absolutePath)
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to write file: ${e.message}", e)
            }
        }
    }

    actual suspend fun readFromFile(path: String): FileReadResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile(path)
                if (!file.exists()) {
                    return@withContext FileReadResult.Error("File does not exist: $path")
                }
                if (file.isDirectory) {
                    return@withContext FileReadResult.Error("Path is a directory, not a file: $path")
                }
                val content = file.readText()
                FileReadResult.Success(content, file.absolutePath)
            } catch (e: Exception) {
                FileReadResult.Error("Failed to read file: ${e.message}", e)
            }
        }
    }

    actual suspend fun appendToFile(path: String, content: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile(path)
                file.parentFile?.mkdirs()
                file.appendText(content)
                FileSystemResult.Success(file.absolutePath)
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to append to file: ${e.message}", e)
            }
        }
    }

    actual suspend fun deleteFile(path: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = getFile(path)
                if (!file.exists()) {
                    return@withContext FileSystemResult.Error("File does not exist: $path")
                }
                if (file.isDirectory) {
                    return@withContext FileSystemResult.Error("Path is a directory, not a file: $path")
                }
                val deleted = file.delete()
                if (deleted) {
                    FileSystemResult.Success(path)
                } else {
                    FileSystemResult.Error("Failed to delete file: $path")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to delete file: ${e.message}", e)
            }
        }
    }

    actual suspend fun renameFile(oldPath: String, newPath: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val oldFile = getFile(oldPath)
                val newFile = getFile(newPath)

                if (!oldFile.exists()) {
                    return@withContext FileSystemResult.Error("Source file does not exist: $oldPath")
                }
                if (oldFile.isDirectory) {
                    return@withContext FileSystemResult.Error("Source path is a directory, not a file: $oldPath")
                }

                newFile.parentFile?.mkdirs()
                val renamed = oldFile.renameTo(newFile)

                if (renamed) {
                    FileSystemResult.Success(newFile.absolutePath)
                } else {
                    FileSystemResult.Error("Failed to rename file from $oldPath to $newPath")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to rename file: ${e.message}", e)
            }
        }
    }

    actual suspend fun copyFile(sourcePath: String, destinationPath: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = getFile(sourcePath)
                val destFile = getFile(destinationPath)

                if (!sourceFile.exists()) {
                    return@withContext FileSystemResult.Error("Source file does not exist: $sourcePath")
                }
                if (sourceFile.isDirectory) {
                    return@withContext FileSystemResult.Error("Source path is a directory, not a file: $sourcePath")
                }

                destFile.parentFile?.mkdirs()
                sourceFile.copyTo(destFile, overwrite = true)
                FileSystemResult.Success(destFile.absolutePath)
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to copy file: ${e.message}", e)
            }
        }
    }

    actual fun fileExists(path: String): ExistsResult {
        return try {
            val file = getFile(path)
            if (file.exists() && file.isFile) {
                ExistsResult.Exists(file.absolutePath, false)
            } else {
                ExistsResult.NotExists(path)
            }
        } catch (e: Exception) {
            ExistsResult.Error("Failed to check file existence: ${e.message}", e)
        }
    }

    // ========== Directory Operations ==========

    actual suspend fun createDirectory(path: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val dir = getFile(path)
                val created = dir.mkdirs()
                if (created || dir.exists()) {
                    FileSystemResult.Success(dir.absolutePath)
                } else {
                    FileSystemResult.Error("Failed to create directory: $path")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to create directory: ${e.message}", e)
            }
        }
    }

    actual suspend fun deleteDirectory(path: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val dir = getFile(path)
                if (!dir.exists()) {
                    return@withContext FileSystemResult.Error("Directory does not exist: $path")
                }
                if (!dir.isDirectory) {
                    return@withContext FileSystemResult.Error("Path is not a directory: $path")
                }
                if (dir.listFiles()?.isNotEmpty() == true) {
                    return@withContext FileSystemResult.Error("Directory is not empty: $path")
                }

                val deleted = dir.delete()
                if (deleted) {
                    FileSystemResult.Success(path)
                } else {
                    FileSystemResult.Error("Failed to delete directory: $path")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to delete directory: ${e.message}", e)
            }
        }
    }

    actual suspend fun deleteDirectoryRecursively(path: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val dir = getFile(path)
                if (!dir.exists()) {
                    return@withContext FileSystemResult.Error("Directory does not exist: $path")
                }
                if (!dir.isDirectory) {
                    return@withContext FileSystemResult.Error("Path is not a directory: $path")
                }

                val deleted = dir.deleteRecursively()
                if (deleted) {
                    FileSystemResult.Success(path)
                } else {
                    FileSystemResult.Error("Failed to delete directory recursively: $path")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to delete directory recursively: ${e.message}", e)
            }
        }
    }

    actual suspend fun renameDirectory(oldPath: String, newPath: String): FileSystemResult {
        return withContext(Dispatchers.IO) {
            try {
                val oldDir = getFile(oldPath)
                val newDir = getFile(newPath)

                if (!oldDir.exists()) {
                    return@withContext FileSystemResult.Error("Source directory does not exist: $oldPath")
                }
                if (!oldDir.isDirectory) {
                    return@withContext FileSystemResult.Error("Source path is not a directory: $oldPath")
                }

                newDir.parentFile?.mkdirs()
                val renamed = oldDir.renameTo(newDir)

                if (renamed) {
                    FileSystemResult.Success(newDir.absolutePath)
                } else {
                    FileSystemResult.Error("Failed to rename directory from $oldPath to $newPath")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to rename directory: ${e.message}", e)
            }
        }
    }

    actual fun directoryExists(path: String): ExistsResult {
        return try {
            val dir = getFile(path)
            if (dir.exists() && dir.isDirectory) {
                ExistsResult.Exists(dir.absolutePath, true)
            } else {
                ExistsResult.NotExists(path)
            }
        } catch (e: Exception) {
            ExistsResult.Error("Failed to check directory existence: ${e.message}", e)
        }
    }

    actual suspend fun listDirectory(path: String, recursive: Boolean): ListResult {
        return withContext(Dispatchers.IO) {
            try {
                val dir = getFile(path)
                if (!dir.exists()) {
                    return@withContext ListResult.Error("Directory does not exist: $path")
                }
                if (!dir.isDirectory) {
                    return@withContext ListResult.Error("Path is not a directory: $path")
                }

                val files = mutableListOf<FileInfo>()

                fun collectFiles(directory: File) {
                    directory.listFiles()?.forEach { file ->
                        files.add(
                            FileInfo(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = file.isDirectory,
                                size = if (file.isFile) file.length() else 0L,
                                lastModified = file.lastModified()
                            )
                        )
                        if (recursive && file.isDirectory) {
                            collectFiles(file)
                        }
                    }
                }

                collectFiles(dir)
                ListResult.Success(files, dir.absolutePath)
            } catch (e: Exception) {
                ListResult.Error("Failed to list directory: ${e.message}", e)
            }
        }
    }

    // ========== Path Operations ==========

    actual fun getAbsolutePath(relativePath: String): String {
        return getFile(relativePath).absolutePath
    }

    actual fun getBaseDirectory(): String {
        return baseDir.absolutePath
    }

    actual fun exists(path: String): ExistsResult {
        return try {
            val file = getFile(path)
            if (file.exists()) {
                ExistsResult.Exists(file.absolutePath, file.isDirectory)
            } else {
                ExistsResult.NotExists(path)
            }
        } catch (e: Exception) {
            ExistsResult.Error("Failed to check existence: ${e.message}", e)
        }
    }

    actual fun setExecutable(path: String): FileSystemResult {
        return try {
            val file = getFile(path)
            if (file.exists()) {
                file.setExecutable(true, false)
                FileSystemResult.Success(file.absolutePath)
            } else {
                FileSystemResult.Error("File not found: $path", null)
            }
        } catch (e: Exception) {
            FileSystemResult.Error("Failed to set executable: ${e.message}", e)
        }
    }
}