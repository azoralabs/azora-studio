package org.azora.studio.az_script

import org.azora.canvas.domain.interpreter.ConsoleOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopCSharpRunner : CSharpRunner {

    @Volatile
    private var runningProcess: Process? = null

    override suspend fun execute(csharpSource: String, console: ConsoleOutputManager) {
        withContext(Dispatchers.IO) {
            val tmpDir = File(System.getProperty("java.io.tmpdir"), "azora_cs_compile")
            tmpDir.mkdirs()

            val projectFile = File(tmpDir, "AzoraRun.csproj")
            val sourceFile = File(tmpDir, "Program.cs")

            try {
                // ── 1. Find dotnet ──────────────────────────────────
                val dotnet = resolveDotnet()
                if (dotnet == null) {
                    console.error("dotnet not found. Install the .NET SDK or add it to PATH.")
                    return@withContext
                }

                // ── 2. Write project file + source ──────────────────
                projectFile.writeText("""
                    <Project Sdk="Microsoft.NET.Sdk">
                      <PropertyGroup>
                        <OutputType>Exe</OutputType>
                        <TargetFramework>net10.0</TargetFramework>
                        <ImplicitUsings>enable</ImplicitUsings>
                      </PropertyGroup>
                    </Project>
                """.trimIndent())
                sourceFile.writeText(csharpSource)

                // ── 3. Build & run: dotnet run ──────────────────────
                console.info("Compiling with dotnet...")
                val runCmd = listOf(dotnet, "run", "--project", tmpDir.absolutePath)
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
                projectFile.delete()
                sourceFile.delete()
                // Clean build artifacts
                File(tmpDir, "bin").deleteRecursively()
                File(tmpDir, "obj").deleteRecursively()
            }
        }
    }

    override fun stop() {
        runningProcess?.destroyForcibly()
        runningProcess = null
    }

    private fun resolveDotnet(): String? {
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val cmd = if (isWindows) listOf("where", "dotnet") else listOf("which", "dotnet")
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val path = proc.inputStream.bufferedReader().readLine()?.trim()
            proc.waitFor()
            if (!path.isNullOrEmpty() && File(path).exists()) path else null
        } catch (_: Exception) { null }
    }
}
