package org.azora.studio.az_script

import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopKotlinRunner : KotlinRunner {

    @Volatile
    private var runningProcess: Process? = null

    override suspend fun execute(kotlinSource: String, console: ConsoleOutputManager) {
        withContext(Dispatchers.IO) {
            val tmpDir = File(System.getProperty("java.io.tmpdir"), "azora_kt_compile")
            tmpDir.mkdirs()

            val sourceFile = File(tmpDir, "Main.kt")
            val outputJar = File(tmpDir, "Main.jar")

            try {
                sourceFile.writeText(kotlinSource)

                // ── 1. Find kotlinc ──────────────────────────────────
                val kotlinc = resolveKotlinc()
                if (kotlinc == null) {
                    console.error("kotlinc not found. Install the Kotlin compiler or add it to PATH.")
                    return@withContext
                }

                // ── 2. Compile: kotlinc Main.kt -include-runtime -d Main.jar ──
                console.info("Compiling with kotlinc...")
                val compileCmd = listOf(
                    kotlinc,
                    sourceFile.absolutePath,
                    "-include-runtime",
                    "-d", outputJar.absolutePath,
                    "-nowarn"
                )
                val compileProcess = ProcessBuilder(compileCmd)
                    .directory(tmpDir)
                    .redirectErrorStream(true)
                    .start()

                runningProcess = compileProcess
                val compileOutput = compileProcess.inputStream.bufferedReader().readText()
                val compileExit = compileProcess.waitFor()
                runningProcess = null

                if (compileExit != 0) {
                    for (line in compileOutput.lines()) {
                        if (line.isNotBlank()) console.error(line)
                    }
                    console.error("Compilation failed (exit code $compileExit).")
                    return@withContext
                }

                // ── 3. Execute: java -jar Main.jar ───────────────────
                console.info("Running JVM bytecode...")
                val java = resolveJava()
                val runCmd = listOf(java, "-jar", outputJar.absolutePath)
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
                // Cleanup temp files
                sourceFile.delete()
                outputJar.delete()
            }
        }
    }

    override fun stop() {
        runningProcess?.destroyForcibly()
        runningProcess = null
    }

    private fun resolveKotlinc(): String? {
        // 1. Check KOTLIN_HOME
        val kotlinHome = System.getenv("KOTLIN_HOME")
        if (kotlinHome != null) {
            val bin = File(kotlinHome, "bin/kotlinc")
            if (bin.exists()) return bin.absolutePath
            val binCmd = File(kotlinHome, "bin/kotlinc.bat")
            if (binCmd.exists()) return binCmd.absolutePath
        }

        // 2. Check PATH via `which`/`where`
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val cmd = if (isWindows) listOf("where", "kotlinc") else listOf("which", "kotlinc")
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val path = proc.inputStream.bufferedReader().readLine()?.trim()
            proc.waitFor()
            if (!path.isNullOrEmpty() && File(path).exists()) path else null
        } catch (_: Exception) { null }
    }

    private fun resolveJava(): String {
        val javaHome = System.getProperty("java.home")
        val javaBin = File(javaHome, "bin/java")
        return if (javaBin.exists()) javaBin.absolutePath else "java"
    }
}
