package org.azora.studio.azora_lang

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import java.io.File

object AzoraLangManager {

    private val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("windows")
    private val homeDir: File = File(System.getProperty("user.home"))

    private val installDir: File
        get() = System.getenv("AZORA_HOME")?.let(::File) ?: File(homeDir, ".azoralang")

    fun isInstalled(): Boolean {
        val binary = File(installDir, "bin/${binaryName()}")
        val libDir = File(installDir, "lib")
        return binary.exists() && libDir.isDirectory && (libDir.listFiles { f -> f.extension == "jar" }?.isNotEmpty() == true)
    }

    fun installedVersion(): String? {
        val versionFile = File(installDir, "VERSION")
        if (!versionFile.exists()) return null
        return runCatching { versionFile.readText().trim().ifEmpty { null } }.getOrNull()
    }

    fun expectedVersionFromSource(sourceDir: File): String? {
        val buildConfig = File(
            sourceDir,
            "build-config/src/commonMain/kotlin/dev/azora/lang/BuildConfig.kt"
        )
        if (!buildConfig.exists()) return null
        val match = Regex("""const\s+val\s+VERSION\s*=\s*"([^"]+)"""")
            .find(buildConfig.readText())
        return match?.groupValues?.getOrNull(1)
    }

    fun findSourceDir(): File? {
        // 1. Explicit env var override
        System.getenv("AZORALANG_SOURCE")?.let { File(it) }
            ?.takeIf { it.isInstallSource() }?.let { return it }

        // 2. Sibling of the studio repo (the typical dev layout)
        val workingDir = File(System.getProperty("user.dir"))
        sequenceOf(workingDir, workingDir.parentFile)
            .filterNotNull()
            .map { File(it.parentFile ?: it, "azora-lang") }
            .firstOrNull { it.isInstallSource() }
            ?.let { return it }

        // 3. Conventional workspace location
        val workspace = File(homeDir, "Work/AzoraTech/azora-lang")
        if (workspace.isInstallSource()) return workspace

        return null
    }

    suspend fun ensureInstalled(
        onStatus: (String) -> Unit = {},
        console: ConsoleOutputManager? = null
    ): Result {
        val installed = isInstalled()
        val source = findSourceDir()

        if (installed && source == null) {
            return Result.AlreadyInstalled(installedVersion() ?: "unknown")
        }

        if (installed && source != null) {
            val drift = checkDrift(source)
            if (drift == null) {
                return Result.AlreadyInstalled(installedVersion() ?: "unknown")
            }
            val from = installedVersion() ?: "unknown"
            onStatus("Updating Azora language ($from → ${drift.expectedVersion ?: "source"})...")
            console?.info(
                "Azora language source is newer than install (${drift.reason}); rebuilding from ${source.absolutePath}"
            )
            val result = runInstaller(source, console)
            return when (result) {
                is Result.Installed -> Result.Updated(from = from, to = result.version)
                else -> result
            }
        }

        if (!installed && source == null) {
            return Result.NoSource(
                "Azora language is not installed and no source directory was found. " +
                        "Set AZORALANG_SOURCE or place the azora-lang repository next to azora-studio."
            )
        }

        // Not installed, source available — fresh install.
        val expected = expectedVersionFromSource(source!!)
        val versionLabel = expected ?: "from ${source.name}"
        onStatus("Installing Azora language ($versionLabel) — this may take a minute...")
        console?.info("Azora language not found at ${installDir.absolutePath}; installing from ${source.absolutePath}")

        return runInstaller(source, console)
    }

    private data class Drift(val expectedVersion: String?, val reason: String)

    private fun checkDrift(sourceDir: File): Drift? {
        val expected = expectedVersionFromSource(sourceDir)
        val installed = installedVersion()
        if (expected != null && installed != null && expected != installed) {
            return Drift(expected, "version $installed → $expected")
        }

        val installMtime = newestInstallMtime() ?: return Drift(expected, "install timestamps unavailable")
        val sourceMtime = newestSourceMtime(sourceDir) ?: return null
        return if (sourceMtime > installMtime) {
            Drift(expected, "source modified ${(sourceMtime - installMtime) / 1000}s after install")
        } else null
    }

    private fun newestInstallMtime(): Long? {
        val candidates = listOfNotNull(
            File(installDir, "VERSION").takeIf { it.exists() },
            File(installDir, "lib").takeIf { it.isDirectory }
        )
        if (candidates.isEmpty()) return null
        val direct = candidates.maxOf { it.lastModified() }
        val libDir = File(installDir, "lib")
        val libNewest = libDir.listFiles { f -> f.extension == "jar" }
            ?.maxOfOrNull { it.lastModified() } ?: 0L
        return maxOf(direct, libNewest).takeIf { it > 0 }
    }

    private fun newestSourceMtime(sourceDir: File): Long? {
        val roots = listOf("compiler/src", "app/src", "build-tool/src", "build-config/src")
            .map { File(sourceDir, it) }
            .filter { it.isDirectory }
        if (roots.isEmpty()) return null

        var newest = 0L
        for (root in roots) {
            root.walkTopDown().forEach { f ->
                if (f.isFile && (f.extension == "kt" || f.extension == "kts")) {
                    val m = f.lastModified()
                    if (m > newest) newest = m
                }
            }
        }
        return newest.takeIf { it > 0 }
    }

    private suspend fun runInstaller(sourceDir: File, console: ConsoleOutputManager?): Result =
        withContext(Dispatchers.IO) {
            val script = File(sourceDir, if (isWindows) "install.bat" else "install.sh")
            if (!script.exists()) {
                return@withContext Result.Failed("Installer script not found at ${script.absolutePath}")
            }

            val command = if (isWindows) {
                listOf("cmd.exe", "/c", script.absolutePath)
            } else {
                listOf("/bin/bash", script.absolutePath)
            }

            val process = try {
                ProcessBuilder(command)
                    .directory(sourceDir)
                    .redirectErrorStream(true)
                    .start()
            } catch (e: Exception) {
                return@withContext Result.Failed("Failed to launch installer: ${e.message}")
            }

            process.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) console?.info(line)
                    line = reader.readLine()
                }
            }

            val exit = process.waitFor()
            if (exit == 0 && isInstalled()) {
                Result.Installed(installedVersion() ?: "unknown")
            } else {
                Result.Failed("Installer exited with code $exit")
            }
        }

    private fun File.isInstallSource(): Boolean {
        if (!isDirectory) return false
        val script = File(this, if (isWindows) "install.bat" else "install.sh")
        val gradlew = File(this, if (isWindows) "gradlew.bat" else "gradlew")
        return script.exists() && gradlew.exists()
    }

    private fun binaryName(): String = if (isWindows) "azora.bat" else "azora"

    sealed class Result {
        data class AlreadyInstalled(val version: String) : Result()
        data class Installed(val version: String) : Result()
        data class Updated(val from: String, val to: String) : Result()
        data class NoSource(val message: String) : Result()
        data class Failed(val message: String) : Result()
    }
}
