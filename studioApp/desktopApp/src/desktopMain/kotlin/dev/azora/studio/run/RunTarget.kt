package dev.azora.studio.run

import dev.azora.sdk.core.project.domain.ProjectTemplate
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

    /** Whether the template can be run from Studio at all (drives toolbar visibility). */
    fun isRunnable(template: ProjectTemplate): Boolean = when (template) {
        ProjectTemplate.EMPTY, ProjectTemplate.DESKTOP, ProjectTemplate.WEB,
        ProjectTemplate.SERVER, ProjectTemplate.MOBILE, ProjectTemplate.MULTIPLATFORM -> true
        ProjectTemplate.WEBSITE, ProjectTemplate.AUDIO, ProjectTemplate.PIXEL -> false
    }

    /** Targets for [template]. Performs quick subprocess calls — run off the UI thread. */
    fun targetsFor(template: ProjectTemplate): List<RunTarget> = buildList {
        when (template) {
            ProjectTemplate.EMPTY, ProjectTemplate.SERVER ->
                add(RunTarget("jvm", "Run", RunTargetKind.JVM, "run"))
            ProjectTemplate.DESKTOP ->
                add(RunTarget("desktop", "Desktop", RunTargetKind.DESKTOP, "run"))
            ProjectTemplate.WEB ->
                add(RunTarget("web", "Web (browser)", RunTargetKind.WEB, "wasmJsBrowserDevelopmentRun"))
            ProjectTemplate.MOBILE -> {
                addAll(androidTargets(":composeApp:installDebug"))
                addAll(iosTargets())
            }
            ProjectTemplate.MULTIPLATFORM -> {
                add(RunTarget("desktop", "Desktop", RunTargetKind.DESKTOP, ":composeApp:run"))
                add(RunTarget("web", "Web (browser)", RunTargetKind.WEB, ":composeApp:wasmJsBrowserDevelopmentRun"))
                addAll(androidTargets(":composeApp:installDebug"))
                addAll(iosTargets())
            }
            ProjectTemplate.WEBSITE, ProjectTemplate.AUDIO, ProjectTemplate.PIXEL -> {}
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
