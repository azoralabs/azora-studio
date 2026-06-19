package dev.azora.studio.run

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import java.io.File
import kotlin.concurrent.thread

/**
 * Runs the selected [RunTarget] for a generated project and streams output into the shared
 * [ConsoleOutputManager] (rendered by the Studio "Console" dock panel).
 *
 * - Desktop / Web / JVM / Server → the target's Gradle task.
 * - Android → boots the chosen emulator (if needed), `installDebug`, then launches the app.
 * - iOS → prints guidance (needs the Xcode project; see `iosApp/README.md`).
 *
 * Gradle always runs with Studio's own JVM (`java.home`) so it never picks up a too-new
 * system JDK that Gradle can't run on.
 */
class ProjectRunner(private val console: ConsoleOutputManager) {

    @Volatile
    private var process: Process? = null

    var isRunning by mutableStateOf(false)
        private set

    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    fun run(projectDir: File, target: RunTarget, appId: String) {
        stop()
        if (!projectDir.isDirectory) {
            console.error("Project directory not found: ${projectDir.absolutePath}")
            return
        }
        console.clear()
        console.info("Run ▸ ${target.label}")
        isRunning = true
        thread(isDaemon = true, name = "azora-project-runner") {
            try {
                when (target.kind) {
                    RunTargetKind.IOS -> {
                        console.info("iOS run isn't automated yet.")
                        console.info("Open ${projectDir.resolve("iosApp").absolutePath} in Xcode (see iosApp/README.md) and run on \"${target.label}\".")
                    }
                    RunTargetKind.ANDROID -> runAndroid(projectDir, target, appId)
                    else -> runGradle(projectDir, target.gradleTask ?: "run", emptyMap())
                }
            } catch (e: Exception) {
                console.error("Failed to run: ${e.message}")
            } finally {
                process = null
                isRunning = false
            }
        }
    }

    private fun runGradle(projectDir: File, task: String, extraEnv: Map<String, String>): Boolean {
        console.info("> gradlew $task")
        val wrapper = File(projectDir, if (isWindows) "gradlew.bat" else "gradlew")
        val launcher = when {
            wrapper.exists() -> wrapper.absolutePath
            isWindows -> "gradle.bat"
            else -> "gradle"
        }
        val builder = ProcessBuilder(launcher, task, "--console=plain")
            .directory(projectDir)
            .redirectErrorStream(true)
        System.getProperty("java.home")?.takeIf { it.isNotBlank() }?.let {
            builder.environment()["JAVA_HOME"] = it
        }
        extraEnv.forEach { (k, v) -> builder.environment()[k] = v }
        val proc = builder.start()
        process = proc
        proc.inputStream.bufferedReader().forEachLine { console.println(it) }
        val code = proc.waitFor()
        return if (code == 0) {
            console.info("Done (exit code 0)")
            true
        } else {
            console.error("Exited with code $code")
            false
        }
    }

    private fun runAndroid(projectDir: File, target: RunTarget, appId: String) {
        val adb = RunTargets.adb()
        if (adb == null) {
            console.error("adb not found — set ANDROID_HOME / install the Android SDK.")
            return
        }
        if (target.androidAvd != null) {
            console.info("Booting emulator \"${target.androidAvd}\"…")
            RunTargets.sdkDir()?.resolve("emulator/emulator")?.let { emu ->
                runCatching {
                    ProcessBuilder(emu.absolutePath, "-avd", target.androidAvd)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                }
            }
            runAndWait(listOf(adb.absolutePath, "wait-for-device"))
            // Wait until the device finishes booting.
            repeat(60) {
                val booted = capture(listOf(adb.absolutePath, "shell", "getprop", "sys.boot_completed")).trim()
                if (booted == "1") return@repeat
                Thread.sleep(2000)
            }
        }
        val env = buildMap { target.androidSerial?.let { put("ANDROID_SERIAL", it) } }
        console.info("Installing on ${target.label}…")
        if (!runGradle(projectDir, target.gradleTask ?: ":composeApp:installDebug", env)) return
        console.info("Launching $appId…")
        val serialArgs = target.androidSerial?.let { listOf("-s", it) } ?: emptyList()
        runAndWait(listOf(adb.absolutePath) + serialArgs + listOf("shell", "monkey", "-p", appId, "-c", "android.intent.category.LAUNCHER", "1"))
        console.info("Launched on ${target.label}.")
    }

    /** Runs a process, streaming its output to the console, and waits for it. */
    private fun runAndWait(cmd: List<String>) {
        val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
        process = proc
        proc.inputStream.bufferedReader().forEachLine { line -> if (line.isNotBlank()) console.println(line) }
        proc.waitFor()
    }

    private fun capture(cmd: List<String>): String =
        runCatching {
            val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val t = p.inputStream.bufferedReader().readText()
            p.waitFor()
            t
        }.getOrDefault("")

    fun stop() {
        process?.let { proc ->
            runCatching { proc.descendants().forEach { it.destroy() } }
            proc.destroy()
            console.info("Stopped.")
        }
        process = null
        isRunning = false
    }
}
