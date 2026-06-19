package dev.azora.studio.run

import dev.azora.sdk.core.project.domain.BUILTIN_TEMPLATE_ID_EMPTY
import dev.azora.sdk.core.project.domain.ProjectRunTargetKind
import dev.azora.sdk.plugin.presentation.PluginManager
import java.io.File

enum class RunTargetKind { JVM, DESKTOP, WEB, ANDROID, IOS }

/**
 * A runnable target for the current project (e.g. Desktop, Web, a specific Android
 * device/emulator, or an iOS simulator). Selected from the top-bar target dropdown.
 */
data class RunTarget(
    val id: String,
    val label: String,
    val kind: RunTargetKind,
    /** Gradle task to run for this target (null for targets handled outside Gradle, e.g. iOS). */
    val gradleTask: String?,
    val androidSerial: String? = null,
    val androidAvd: String? = null,
    val iosUdid: String? = null,
    /**
     * Optional Gradle task that stops a detached, long-running server started by [gradleTask].
     *
     * When set, [gradleTask] is expected to launch a background server process and return (e.g.
     * Kobweb's `kobwebStart`). The run stays "running" until this stop task is invoked. Null for
     * ordinary blocking tasks that are killed by destroying the process.
     */
    val stopTask: String? = null,
)

/**
 * Builds the list of run targets for a project template, enumerating Android
 * devices/emulators and iOS simulators from the local toolchains.
 */
object RunTargets {

    fun sdkDir(): File? =
        (System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: System.getProperty("user.home")?.let { "$it/Library/Android/sdk" })
            ?.let(::File)?.takeIf { it.isDirectory }

    fun adb(): File? = sdkDir()?.resolve("platform-tools/adb")?.takeIf { it.canExecute() }
    private fun emulator(): File? = sdkDir()?.resolve("emulator/emulator")?.takeIf { it.canExecute() }

    /** Whether the template can be run from Studio at all (drives toolbar visibility). Cheap (no subprocess). */
    fun isRunnable(templateId: String, pluginManager: PluginManager): Boolean =
        templateId == BUILTIN_TEMPLATE_ID_EMPTY ||
            pluginManager.templateContributions()
                .firstOrNull { it.id == templateId }
                ?.runTargets
                ?.isNotEmpty() == true

    /**
     * Targets for [templateId]. The builtin "empty" template contributes a plain "Run"; every other
     * template's targets come from the matching plugin's contribution. Android/iOS-kind targets are
     * expanded to per-device/per-simulator targets. Performs subprocess calls — run off the UI thread.
     */
    fun targetsFor(templateId: String, pluginManager: PluginManager): List<RunTarget> = buildList {
        if (templateId == BUILTIN_TEMPLATE_ID_EMPTY) {
            add(RunTarget("run", "Run", RunTargetKind.JVM, "run"))
            return@buildList
        }
        val contribution = pluginManager.templateContributions().firstOrNull { it.id == templateId } ?: return@buildList
        contribution.runTargets.forEach { target ->
            when (target.kind) {
                ProjectRunTargetKind.GRADLE -> add(
                    RunTarget(target.id, target.label, RunTargetKind.JVM, target.gradleTask, stopTask = target.stopTask)
                )
                ProjectRunTargetKind.ANDROID -> addAll(androidTargets(target.gradleTask))
                ProjectRunTargetKind.IOS -> addAll(iosTargets())
            }
        }
    }

    private fun androidTargets(installTask: String): List<RunTarget> = buildList {
        adb()?.let { adb ->
            runCatching {
                exec(listOf(adb.absolutePath, "devices")).lineSequence()
                    .drop(1)
                    .map { it.trim() }
                    .filter { it.endsWith("device") }
                    .forEach { line ->
                        val serial = line.substringBefore('\t').substringBefore(' ').trim()
                        if (serial.isNotEmpty()) {
                            add(RunTarget("android-dev-$serial", "Android device · $serial", RunTargetKind.ANDROID, installTask, androidSerial = serial))
                        }
                    }
            }
        }
        emulator()?.let { emu ->
            runCatching {
                exec(listOf(emu.absolutePath, "-list-avds")).lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { avd ->
                        add(RunTarget("android-avd-$avd", "Android emulator · $avd", RunTargetKind.ANDROID, installTask, androidAvd = avd))
                    }
            }
        }
    }

    private fun iosTargets(): List<RunTarget> = buildList {
        runCatching {
            val text = exec(listOf("xcrun", "simctl", "list", "devices", "available"))
            Regex("""^\s+(.+?) \(([0-9A-Fa-f-]{36})\) \((?:Booted|Shutdown)\)""", RegexOption.MULTILINE)
                .findAll(text)
                .forEach { m ->
                    val name = m.groupValues[1]
                    val udid = m.groupValues[2]
                    if (name.contains("iPhone") || name.contains("iPad")) {
                        add(RunTarget("ios-$udid", "iOS Simulator · $name", RunTargetKind.IOS, null, iosUdid = udid))
                    }
                }
        }
    }

    private fun exec(cmd: List<String>): String {
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText()
        p.waitFor()
        return text
    }
}
