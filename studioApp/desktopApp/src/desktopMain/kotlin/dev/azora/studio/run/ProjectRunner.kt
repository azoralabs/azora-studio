package dev.azora.studio.run

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import dev.azora.sdk.core.project.domain.ProjectTemplate
import java.io.File
import kotlin.concurrent.thread

/**
 * Runs a generated project's Gradle task as a child process and streams its output
 * (stdout + stderr) into the shared [ConsoleOutputManager], which the Studio "Console"
 * dock panel renders live.
 */
class ProjectRunner(private val console: ConsoleOutputManager) {

    @Volatile
    private var process: Process? = null

    /** Observable so the toolbar can enable/disable Run/Stop. */
    var isRunning by mutableStateOf(false)
        private set

    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    /**
     * Runs [task] with the project's Gradle wrapper in [projectDir], streaming output to the console.
     * Any currently-running process is stopped first.
     */
    fun run(projectDir: File, task: String) {
        stop()
        if (!projectDir.isDirectory) {
            console.error("Project directory not found: ${projectDir.absolutePath}")
            return
        }
        console.clear()
        console.info("> gradlew $task  (${projectDir.name})")
        isRunning = true
        thread(isDaemon = true, name = "azora-project-runner") {
            try {
                val wrapper = File(projectDir, if (isWindows) "gradlew.bat" else "gradlew")
                val launcher = when {
                    wrapper.exists() -> wrapper.absolutePath
                    isWindows -> "gradle.bat"
                    else -> "gradle"
                }
                val builder = ProcessBuilder(launcher, task, "--console=plain")
                    .directory(projectDir)
                    .redirectErrorStream(true)
                // Run Gradle with Studio's own (Gradle-compatible) JVM, so it doesn't pick up
                // a too-new system JDK (e.g. Homebrew's JDK 26) that Gradle can't run on.
                System.getProperty("java.home")?.takeIf { it.isNotBlank() }?.let {
                    builder.environment()["JAVA_HOME"] = it
                }
                val proc = builder.start()
                process = proc
                proc.inputStream.bufferedReader().forEachLine { line -> console.println(line) }
                val code = proc.waitFor()
                if (code == 0) console.info("Process finished (exit code 0)")
                else console.error("Process exited with code $code")
            } catch (e: Exception) {
                console.error("Failed to run project: ${e.message}")
            } finally {
                process = null
                isRunning = false
            }
        }
    }

    /** Stops the running process (and its children). */
    fun stop() {
        process?.let { proc ->
            runCatching { proc.descendants().forEach { it.destroy() } }
            proc.destroy()
            console.info("Stopped.")
        }
        process = null
        isRunning = false
    }

    companion object {
        /**
         * Gradle task used to run a project of the given [template] from Studio, or
         * `null` if running from Studio isn't supported for it yet.
         */
        fun runTaskFor(template: ProjectTemplate): String? = when (template) {
            ProjectTemplate.EMPTY,
            ProjectTemplate.DESKTOP,
            ProjectTemplate.SERVER -> "run"
            ProjectTemplate.WEB -> "wasmJsBrowserDevelopmentRun"
            // Multiplatform: run the desktop target from Studio.
            ProjectTemplate.MULTIPLATFORM -> ":composeApp:run"
            // Mobile (Android/iOS) runs on a device/emulator, not streamed to the console.
            ProjectTemplate.WEBSITE,
            ProjectTemplate.MOBILE,
            ProjectTemplate.AUDIO,
            ProjectTemplate.PIXEL -> null
        }
    }
}
