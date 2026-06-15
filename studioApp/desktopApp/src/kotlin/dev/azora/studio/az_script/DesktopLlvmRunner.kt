package org.azora.studio.az_script

import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopLlvmRunner : LlvmRunner {

    @Volatile
    private var runningProcess: Process? = null

    override suspend fun execute(llvmIrSource: String, console: ConsoleOutputManager) {
        withContext(Dispatchers.IO) {
            val tmpDir = File(System.getProperty("java.io.tmpdir"), "azora_llvm_run")
            tmpDir.mkdirs()

            val sourceFile = File(tmpDir, "main.ll")

            try {
                // ── 1. Find lli ──────────────────────────────────────
                val lli = resolveLli()
                if (lli == null) {
                    console.error("lli not found. Install LLVM or add it to PATH.")
                    return@withContext
                }

                // ── 2. Write LLVM IR source ──────────────────────────
                sourceFile.writeText(llvmIrSource)

                // ── 3. Run: lli main.ll ──────────────────────────────
                console.info("Running with lli...")
                val runCmd = listOf(lli, sourceFile.absolutePath)
                val runProcess = ProcessBuilder(runCmd)
                    .directory(tmpDir)
                    .start()

                runningProcess = runProcess

                // Stream stdout
                val stdoutThread = Thread {
                    runProcess.inputStream.bufferedReader().forEachLine { line ->
                        console.println(line)
                    }
                }.apply { isDaemon = true; start() }

                // Stream stderr
                val stderrThread = Thread {
                    runProcess.errorStream.bufferedReader().forEachLine { line ->
                        console.error(line)
                    }
                }.apply { isDaemon = true; start() }

                val exitCode = runProcess.waitFor()
                stdoutThread.join(2000)
                stderrThread.join(2000)
                runningProcess = null

                if (exitCode != 0) {
                    console.error("Process exited with code $exitCode.")
                }
            } catch (e: InterruptedException) {
                console.info("Execution interrupted.")
            } catch (e: Exception) {
                console.error("Error: ${e.message}")
            } finally {
                runningProcess = null
                sourceFile.delete()
            }
        }
    }

    override fun stop() {
        runningProcess?.destroyForcibly()
        runningProcess = null
    }

    private fun resolveLli(): String? {
        // 1. Check LLVM_HOME
        val llvmHome = System.getenv("LLVM_HOME")
        if (llvmHome != null) {
            val bin = File(llvmHome, "bin/lli")
            if (bin.exists()) return bin.absolutePath
        }

        // 2. Check Kotlin/Native bundled LLVM (konan)
        val konanHome = System.getenv("HOME")?.let { home ->
            val konanDir = File(home, ".konan/dependencies")
            if (konanDir.exists()) {
                konanDir.listFiles()
                    ?.filter { it.name.startsWith("llvm-") && it.isDirectory }
                    ?.sortedByDescending { it.name }
                    ?.firstOrNull()
            } else null
        }
        if (konanHome != null) {
            val bin = File(konanHome, "bin/lli")
            if (bin.exists()) return bin.absolutePath
        }

        // 3. Check PATH via `which`/`where`
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val cmd = if (isWindows) listOf("where", "lli") else listOf("which", "lli")
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val path = proc.inputStream.bufferedReader().readLine()?.trim()
            proc.waitFor()
            if (!path.isNullOrEmpty() && File(path).exists()) path else null
        } catch (_: Exception) { null }
    }
}
