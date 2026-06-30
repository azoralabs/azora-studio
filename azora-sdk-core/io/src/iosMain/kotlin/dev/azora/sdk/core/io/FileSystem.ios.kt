package dev.azora.sdk.core.io

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFilePosixPermissions
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDate
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile

/**
 * iOS (Kotlin/Native) implementation of [FileSystem] backed by `NSFileManager`.
 *
 * Relative paths are resolved against the app's Documents directory, mirroring the JVM
 * implementation's use of the user Documents folder.
 */
@OptIn(ExperimentalForeignApi::class)
actual class FileSystem {

    private val fileManager = NSFileManager.defaultManager

    private val baseDir: String = run {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        (paths.firstOrNull() as? String) ?: NSTemporaryDirectory()
    }

    init {
        if (!fileManager.fileExistsAtPath(baseDir)) {
            fileManager.createDirectoryAtPath(baseDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }

    private fun resolve(path: String): String =
        if (path.startsWith("/")) path else (baseDir as NSString).stringByAppendingPathComponent(path)

    private fun ensureParent(fullPath: String) {
        val parent = (fullPath as NSString).stringByDeletingLastPathComponent
        if (parent.isNotEmpty() && !fileManager.fileExistsAtPath(parent)) {
            fileManager.createDirectoryAtPath(parent, withIntermediateDirectories = true, attributes = null, error = null)
        }
    }

    private fun existsCheck(path: String): ExistsResult = memScoped {
        val isDir = alloc<BooleanVar>()
        val full = resolve(path)
        if (fileManager.fileExistsAtPath(full, isDirectory = isDir.ptr)) {
            ExistsResult.Exists(full, isDir.value)
        } else {
            ExistsResult.NotExists(path)
        }
    }

    // ========== File Operations ==========

    actual suspend fun writeToFile(path: String, content: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                ensureParent(full)
                val ok = (content as NSString).writeToFile(
                    full, atomically = true, encoding = NSUTF8StringEncoding, error = null
                )
                if (ok) FileSystemResult.Success(full)
                else FileSystemResult.Error("Failed to write file: $path")
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to write file: ${e.message}", e)
            }
        }

    actual suspend fun readFromFile(path: String): FileReadResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                val existing = existsCheck(path)
                if (existing is ExistsResult.NotExists) {
                    return@withContext FileReadResult.Error("File does not exist: $path")
                }
                if (existing is ExistsResult.Exists && existing.isDirectory) {
                    return@withContext FileReadResult.Error("Path is a directory, not a file: $path")
                }
                val content = NSString.stringWithContentsOfFile(full, NSUTF8StringEncoding, null)
                    ?: return@withContext FileReadResult.Error("Failed to read file: $path")
                FileReadResult.Success(content, full)
            } catch (e: Exception) {
                FileReadResult.Error("Failed to read file: ${e.message}", e)
            }
        }

    actual suspend fun appendToFile(path: String, content: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                ensureParent(full)
                val existing = NSString.stringWithContentsOfFile(full, NSUTF8StringEncoding, null) ?: ""
                val ok = ((existing + content) as NSString).writeToFile(
                    full, atomically = true, encoding = NSUTF8StringEncoding, error = null
                )
                if (ok) FileSystemResult.Success(full)
                else FileSystemResult.Error("Failed to append to file: $path")
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to append to file: ${e.message}", e)
            }
        }

    actual suspend fun deleteFile(path: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val existing = existsCheck(path)
                if (existing is ExistsResult.NotExists) {
                    return@withContext FileSystemResult.Error("File does not exist: $path")
                }
                if (existing is ExistsResult.Exists && existing.isDirectory) {
                    return@withContext FileSystemResult.Error("Path is a directory, not a file: $path")
                }
                val full = resolve(path)
                if (fileManager.removeItemAtPath(full, error = null)) FileSystemResult.Success(path)
                else FileSystemResult.Error("Failed to delete file: $path")
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to delete file: ${e.message}", e)
            }
        }

    actual suspend fun renameFile(oldPath: String, newPath: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val oldFull = resolve(oldPath)
                val newFull = resolve(newPath)
                if (!fileManager.fileExistsAtPath(oldFull)) {
                    return@withContext FileSystemResult.Error("Source file does not exist: $oldPath")
                }
                ensureParent(newFull)
                if (fileManager.moveItemAtPath(oldFull, toPath = newFull, error = null)) {
                    FileSystemResult.Success(newFull)
                } else {
                    FileSystemResult.Error("Failed to rename file from $oldPath to $newPath")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to rename file: ${e.message}", e)
            }
        }

    actual suspend fun copyFile(sourcePath: String, destinationPath: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val srcFull = resolve(sourcePath)
                val dstFull = resolve(destinationPath)
                if (!fileManager.fileExistsAtPath(srcFull)) {
                    return@withContext FileSystemResult.Error("Source file does not exist: $sourcePath")
                }
                ensureParent(dstFull)
                // copyItemAtPath fails if the destination already exists; remove it first.
                if (fileManager.fileExistsAtPath(dstFull)) {
                    fileManager.removeItemAtPath(dstFull, error = null)
                }
                if (fileManager.copyItemAtPath(srcFull, toPath = dstFull, error = null)) {
                    FileSystemResult.Success(dstFull)
                } else {
                    FileSystemResult.Error("Failed to copy file from $sourcePath to $destinationPath")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to copy file: ${e.message}", e)
            }
        }

    actual fun fileExists(path: String): ExistsResult = when (val r = existsCheck(path)) {
        is ExistsResult.Exists -> if (r.isDirectory) ExistsResult.NotExists(path) else r
        else -> r
    }

    // ========== Directory Operations ==========

    actual suspend fun createDirectory(path: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                if (fileManager.createDirectoryAtPath(full, withIntermediateDirectories = true, attributes = null, error = null) ||
                    fileManager.fileExistsAtPath(full)
                ) {
                    FileSystemResult.Success(full)
                } else {
                    FileSystemResult.Error("Failed to create directory: $path")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to create directory: ${e.message}", e)
            }
        }

    actual suspend fun deleteDirectory(path: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                val existing = existsCheck(path)
                if (existing is ExistsResult.NotExists) {
                    return@withContext FileSystemResult.Error("Directory does not exist: $path")
                }
                if (existing is ExistsResult.Exists && !existing.isDirectory) {
                    return@withContext FileSystemResult.Error("Path is not a directory: $path")
                }
                val contents = fileManager.contentsOfDirectoryAtPath(full, error = null)
                if (contents != null && contents.isNotEmpty()) {
                    return@withContext FileSystemResult.Error("Directory is not empty: $path")
                }
                if (fileManager.removeItemAtPath(full, error = null)) FileSystemResult.Success(path)
                else FileSystemResult.Error("Failed to delete directory: $path")
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to delete directory: ${e.message}", e)
            }
        }

    actual suspend fun deleteDirectoryRecursively(path: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                val existing = existsCheck(path)
                if (existing is ExistsResult.NotExists) {
                    return@withContext FileSystemResult.Error("Directory does not exist: $path")
                }
                if (existing is ExistsResult.Exists && !existing.isDirectory) {
                    return@withContext FileSystemResult.Error("Path is not a directory: $path")
                }
                if (fileManager.removeItemAtPath(full, error = null)) FileSystemResult.Success(path)
                else FileSystemResult.Error("Failed to delete directory recursively: $path")
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to delete directory recursively: ${e.message}", e)
            }
        }

    actual suspend fun renameDirectory(oldPath: String, newPath: String): FileSystemResult =
        withContext(Dispatchers.Default) {
            try {
                val oldFull = resolve(oldPath)
                val newFull = resolve(newPath)
                val existing = existsCheck(oldPath)
                if (existing is ExistsResult.NotExists) {
                    return@withContext FileSystemResult.Error("Source directory does not exist: $oldPath")
                }
                if (existing is ExistsResult.Exists && !existing.isDirectory) {
                    return@withContext FileSystemResult.Error("Source path is not a directory: $oldPath")
                }
                ensureParent(newFull)
                if (fileManager.moveItemAtPath(oldFull, toPath = newFull, error = null)) {
                    FileSystemResult.Success(newFull)
                } else {
                    FileSystemResult.Error("Failed to rename directory from $oldPath to $newPath")
                }
            } catch (e: Exception) {
                FileSystemResult.Error("Failed to rename directory: ${e.message}", e)
            }
        }

    actual fun directoryExists(path: String): ExistsResult = when (val r = existsCheck(path)) {
        is ExistsResult.Exists -> if (r.isDirectory) r else ExistsResult.NotExists(path)
        else -> r
    }

    actual suspend fun listDirectory(path: String, recursive: Boolean): ListResult =
        withContext(Dispatchers.Default) {
            try {
                val full = resolve(path)
                val existing = existsCheck(path)
                if (existing is ExistsResult.NotExists) {
                    return@withContext ListResult.Error("Directory does not exist: $path")
                }
                if (existing is ExistsResult.Exists && !existing.isDirectory) {
                    return@withContext ListResult.Error("Path is not a directory: $path")
                }

                val files = mutableListOf<FileInfo>()

                fun collect(dir: String) {
                    val names = fileManager.contentsOfDirectoryAtPath(dir, error = null) ?: return
                    names.forEach { nameAny ->
                        val name = nameAny as? String ?: return@forEach
                        val childPath = (dir as NSString).stringByAppendingPathComponent(name)
                        val attrs = fileManager.attributesOfItemAtPath(childPath, error = null)
                        val isDir = memScoped {
                            val flag = alloc<BooleanVar>()
                            fileManager.fileExistsAtPath(childPath, isDirectory = flag.ptr)
                            flag.value
                        }
                        val size = (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
                        val modified = ((attrs?.get(NSFileModificationDate) as? NSDate)
                            ?.timeIntervalSince1970 ?: 0.0) * 1000.0
                        files.add(
                            FileInfo(
                                name = name,
                                path = childPath,
                                isDirectory = isDir,
                                size = if (isDir) 0L else size,
                                lastModified = modified.toLong()
                            )
                        )
                        if (recursive && isDir) collect(childPath)
                    }
                }

                collect(full)
                ListResult.Success(files, full)
            } catch (e: Exception) {
                ListResult.Error("Failed to list directory: ${e.message}", e)
            }
        }

    // ========== Path Operations ==========

    actual fun getAbsolutePath(relativePath: String): String = resolve(relativePath)

    actual fun getBaseDirectory(): String = baseDir

    actual fun exists(path: String): ExistsResult = existsCheck(path)

    actual fun setExecutable(path: String): FileSystemResult {
        return try {
            val full = resolve(path)
            if (!fileManager.fileExistsAtPath(full)) {
                return FileSystemResult.Error("File not found: $path", null)
            }
            // 0o755 — owner rwx, group/other rx
            val ok = fileManager.setAttributes(
                mapOf<Any?, Any>(NSFilePosixPermissions to NSNumber(int = 493)),
                ofItemAtPath = full,
                error = null
            )
            if (ok) FileSystemResult.Success(full)
            else FileSystemResult.Error("Failed to set executable: $path")
        } catch (e: Exception) {
            FileSystemResult.Error("Failed to set executable: ${e.message}", e)
        }
    }
}
