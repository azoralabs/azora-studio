package org.azora.studio.az_script

import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopJavaScriptRunner : JavaScriptRunner {

    @Volatile
    private var runningProcess: Process? = null

    override suspend fun execute(jsSource: String, console: ConsoleOutputManager) {
        withContext(Dispatchers.IO) {
            val tmpDir = File(System.getProperty("java.io.tmpdir"), "azora_js_run")
            tmpDir.mkdirs()

            val sourceFile = File(tmpDir, "main.js")

            try {
                // ── 1. Find node ────────────────────────────────────
                val node = resolveNode()
                if (node == null) {
                    console.error("node not found. Install Node.js or add it to PATH.")
                    return@withContext
                }

                // ── 2. Write source ─────────────────────────────────
                sourceFile.writeText(jsSource)

                // ── 3. Run: node main.js ────────────────────────────
                console.info("Running with Node.js...")
                val runCmd = listOf(node, sourceFile.absolutePath)
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

    private fun resolveNode(): String? {
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val cmd = if (isWindows) listOf("where", "node") else listOf("which", "node")
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val path = proc.inputStream.bufferedReader().readLine()?.trim()
            proc.waitFor()
            if (!path.isNullOrEmpty() && File(path).exists()) path else null
        } catch (_: Exception) { null }
    }
}
