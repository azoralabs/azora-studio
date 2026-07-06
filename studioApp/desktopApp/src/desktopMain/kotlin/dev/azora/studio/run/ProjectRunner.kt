package dev.azora.studio.run

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import java.io.File
import kotlin.concurrent.thread

/** Matches the `http://localhost:<port>` URL a Kobweb server prints when it starts. */
private val URL_REGEX = Regex("https?://localhost:\\d+")

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

    /** A detached server (e.g. Kobweb) started by a run whose [RunTarget.stopTask] is set. */
    @Volatile
    private var detachedStopTask: String? = null
    @Volatile
    private var detachedProjectDir: File? = null

    var isRunning by mutableStateOf(false)
        private set

    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    init {
        // Make sure a running dev server (e.g. Vite on localhost) never outlives Studio: child
        // processes are not killed automatically when the JVM exits, so force-kill the process tree
        // on shutdown. Covers every exit path — window close, Cmd+Q, or process termination.
        Runtime.getRuntime().addShutdownHook(Thread {
            process?.let { proc ->
                runCatching { proc.descendants().forEach { it.destroyForcibly() } }
                proc.destroyForcibly()
            }
        })
    }

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
                    RunTargetKind.COMMAND -> runCommand(projectDir, target)
                    else -> {
                        val result = runGradle(projectDir, target.gradleTask ?: "run", emptyMap())
                        // Detached server (e.g. kobwebStart): task returns but the server keeps running.
                        if (result.ok && target.stopTask != null) {
                            detachedStopTask = target.stopTask
                            detachedProjectDir = projectDir
                            console.info("Server running in the background — use Stop to shut it down.")
                            // Open the browser to the URL the server printed (port may be dynamic).
                            URL_REGEX.find(result.output)?.value?.let { openInBrowser(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                console.error("Failed to run: ${e.message}")
            } finally {
                process = null
                // Stay "running" while a detached server is up; otherwise the task is done.
                if (detachedStopTask == null) isRunning = false
            }
        }
    }

    private data class GradleResult(val ok: Boolean, val output: String)

    private fun runGradle(projectDir: File, task: String, extraEnv: Map<String, String>): GradleResult {
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
        val output = StringBuilder()
        proc.inputStream.bufferedReader().forEachLine { console.println(it); output.appendLine(it) }
        val code = proc.waitFor()
        return if (code == 0) {
            console.info("Done (exit code 0)")
            GradleResult(true, output.toString())
        } else {
            console.error("Exited with code $code")
            GradleResult(false, output.toString())
        }
    }

    /** [RunTargetKind.COMMAND]: launches a shell command (e.g. an npm/Vite dev server) in
     *  [target.workingDir], streams its output, opens the browser at the first localhost URL it
     *  prints, and stays "running" until Stop kills the process tree. */
    private fun runCommand(projectDir: File, target: RunTarget) {
        val command = target.command
        if (command.isNullOrBlank()) { console.error("Run target \"${target.label}\" has no command."); return }
        val dir = target.workingDir?.let { File(projectDir, it) } ?: projectDir
        if (!dir.isDirectory) { console.error("Working directory not found: ${dir.absolutePath}"); return }
        val full = if (isWindows) listOf("cmd", "/c", command) else listOf("sh", "-lc", command)
        console.info("> $command   (in ${target.workingDir ?: "."})")
        val proc = ProcessBuilder(full).directory(dir).redirectErrorStream(true).start()
        process = proc
        var opened = false
        proc.inputStream.bufferedReader().forEachLine { line ->
            console.println(line)
            if (!opened) URL_REGEX.find(line)?.value?.let { url -> opened = true; openInBrowser(url) }
        }
        val code = proc.waitFor()
        if (code != 0) console.error("Exited with code $code")
    }

    /** Opens [url] in the platform default browser (best-effort). */
    private fun openInBrowser(url: String) {
        val cmd = when {
            isWindows -> listOf("cmd", "/c", "start", "", url)
            System.getProperty("os.name").lowercase().contains("mac") -> listOf("open", url)
            else -> listOf("xdg-open", url)
        }
        runCatching { ProcessBuilder(cmd).redirectErrorStream(true).start() }
            .onSuccess { console.info("Opening $url in your browser.") }
            .onFailure { console.info("Server is at $url — open it in your browser.") }
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
        if (!runGradle(projectDir, target.gradleTask ?: ":composeApp:installDebug", env).ok) return
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
        val stopTask = detachedStopTask
        val stopDir = detachedProjectDir
        detachedStopTask = null
        detachedProjectDir = null

        process?.let { proc ->
            val children = runCatching { proc.descendants().toList() }.getOrDefault(emptyList())
            children.forEach { it.destroy() }
            proc.destroy()
            // Escalate to a hard kill after a grace period: a dev server that ignores SIGTERM can
            // survive half-dead, keeping its port open without ever responding — which makes the
            // browser (and the next Run on that port) hang forever.
            thread(isDaemon = true, name = "azora-run-kill") {
                Thread.sleep(1500)
                children.forEach { child -> if (child.isAlive) child.destroyForcibly() }
                if (proc.isAlive) proc.destroyForcibly()
            }
            console.info("Stopped.")
        }
        process = null

        // A detached server (e.g. Kobweb) is shut down via its own gradle stop task.
        if (stopTask != null && stopDir != null) {
            console.info("Stopping server (gradlew $stopTask)…")
            thread(isDaemon = true, name = "azora-server-stop") {
                runCatching { runGradle(stopDir, stopTask, emptyMap()) }
                    .onFailure { console.error("Stop task failed: ${it.message}") }
            }
        }
        isRunning = false
    }
}
