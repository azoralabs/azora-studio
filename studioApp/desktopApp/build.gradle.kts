import java.io.File

plugins {
    alias(libs.plugins.convention.cmp.application)
}

fun isWindowsHost(): Boolean =
    System.getProperty("os.name").lowercase().contains("windows")

fun azoraLangSourceDir(): File? {
    val isWindows = isWindowsHost()
    fun File.isSource(): Boolean {
        if (!isDirectory) return false
        val script = File(this, if (isWindows) "install.bat" else "install.sh")
        val gradlew = File(this, if (isWindows) "gradlew.bat" else "gradlew")
        return script.exists() && gradlew.exists()
    }

    System.getenv("AZORALANG_SOURCE")?.let(::File)?.takeIf { it.isSource() }?.let { return it }

    val studioRepoRoot = rootDir
    listOf(
        File(studioRepoRoot.parentFile, "azora-lang"),
        File(studioRepoRoot, "../azora-lang").canonicalFile
    ).firstOrNull { it.isSource() }?.let { return it }

    val workspace = File(System.getProperty("user.home"), "Work/AzoraTech/azora-lang")
    if (workspace.isSource()) return workspace
    return null
}

fun azoraLangNewestSourceMtime(sourceDir: File): Long {
    val roots = listOf("compiler/src", "app/src", "build-tool/src", "build-config/src")
        .map { File(sourceDir, it) }
        .filter { it.isDirectory }
    var newest = 0L
    for (root in roots) {
        root.walkTopDown().forEach { f ->
            if (f.isFile && (f.extension == "kt" || f.extension == "kts")) {
                val m = f.lastModified()
                if (m > newest) newest = m
            }
        }
    }
    return newest
}

fun azoraLangNewestInstallMtime(installDir: File): Long {
    val versionFile = File(installDir, "VERSION")
    val libDir = File(installDir, "lib")
    val libNewest = libDir.listFiles { f -> f.extension == "jar" }?.maxOfOrNull { it.lastModified() } ?: 0L
    val versionMtime = if (versionFile.exists()) versionFile.lastModified() else 0L
    return maxOf(libNewest, versionMtime)
}

fun Project.runAzoraLangInstaller(sourceDir: File): Boolean {
    val isWindows = isWindowsHost()
    val script = File(sourceDir, if (isWindows) "install.bat" else "install.sh")
    if (!script.exists()) {
        logger.warn("[azora-lang] installer not found at ${script.absolutePath}")
        return false
    }
    logger.lifecycle("[azora-lang] running ${script.name} from ${sourceDir.absolutePath}")
    val cmd = if (isWindows) listOf("cmd.exe", "/c", script.absolutePath)
              else listOf("/bin/bash", script.absolutePath)
    val output = providers.exec {
        commandLine(cmd)
        workingDir = sourceDir
        isIgnoreExitValue = true
    }
    val exit = output.result.get().exitValue
    output.standardOutput.asText.get().lineSequence().forEach { line ->
        if (line.isNotBlank()) logger.lifecycle("[azora-lang] $line")
    }
    output.standardError.asText.orNull?.lineSequence()?.forEach { line ->
        if (line.isNotBlank()) logger.warn("[azora-lang] $line")
    }
    if (exit != 0) {
        logger.warn("[azora-lang] installer exited with code $exit")
        return false
    }
    return true
}

fun Project.ensureAzoraLangInstalled(installDir: File) {
    val libDir = File(installDir, "lib")
    val installed = libDir.isDirectory && (libDir.listFiles { f -> f.extension == "jar" }?.isNotEmpty() == true)
    val source = azoraLangSourceDir()

    if (!installed) {
        if (source == null) {
            logger.warn(
                "[azora-lang] not installed at ${installDir.absolutePath} and no source directory found. " +
                "Set AZORALANG_SOURCE or place azora-lang next to azora-studio."
            )
            return
        }
        logger.lifecycle("[azora-lang] not installed; bootstrapping from ${source.absolutePath}")
        runAzoraLangInstaller(source)
        return
    }

    if (source == null) return // installed and no source to compare against

    val sourceMtime = azoraLangNewestSourceMtime(source)
    val installMtime = azoraLangNewestInstallMtime(installDir)
    if (sourceMtime > installMtime) {
        logger.lifecycle(
            "[azora-lang] source is newer than install (${(sourceMtime - installMtime) / 1000}s); rebuilding"
        )
        runAzoraLangInstaller(source)
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.koin.common)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.serialization.json)

            implementation(projects.buildConfig)

            implementation(projects.azoraLocal.database)

            implementation(projects.azoraSdk.canvas.domain)
            implementation(projects.azoraSdk.canvas.presentation)

            implementation(projects.azoraSdk.color.presentation)

            implementation(projects.azoraSdk.docking.data)
            implementation(projects.azoraSdk.docking.domain)
            implementation(projects.azoraSdk.docking.presentation)

            implementation(projects.azoraSdkCore.component)
            implementation(projects.azoraSdkCore.data)
            implementation(projects.azoraSdkCore.domain)
            implementation(projects.azoraSdkCore.io)
            implementation(projects.azoraSdkCore.presentation)
            implementation(projects.azoraSdkCore.project.domain)
            implementation(projects.azoraSdkCore.project.presentation)
            implementation(projects.azoraSdkCore.theme)
            implementation(projects.azoraSdkCore.util)

            implementation(projects.azoraSdkPlugin.core)
            implementation(projects.azoraSdkPlugin.presentation)
        }

        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlin.stdlib)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(compose.desktop.currentOs)

            // Azora Language — auto-install/refresh from source if missing or stale,
            // then load all JARs from the installation into the desktop classpath.
            val azoraLangHome = System.getenv("AZORA_HOME") ?: (System.getProperty("user.home") + "/.azoralang")
            project.ensureAzoraLangInstalled(file(azoraLangHome))
            val azoraLibDir = file("$azoraLangHome/lib")
            if (azoraLibDir.isDirectory) {
                azoraLibDir.listFiles { f -> f.extension == "jar" }?.forEach {
                    implementation(files(it))
                }
            }

            // LWJGL Vulkan + Shaderc (GLSL→SPIR-V)
            val lwjglVersion = "3.3.5"
            implementation("org.lwjgl:lwjgl:$lwjglVersion")
            implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
            implementation("org.lwjgl:lwjgl-shaderc:$lwjglVersion")

            // LWJGL core natives
            listOf("natives-macos-arm64", "natives-macos", "natives-windows", "natives-linux").forEach { classifier ->
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$classifier")
                runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:$classifier")
            }
            // MoltenVK bundled in macOS Vulkan natives
            runtimeOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion:natives-macos-arm64")
            runtimeOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion:natives-macos")

            // Jolt Physics JNI - add when native binaries are available:
            // val joltVersion = "3.5.2"
            // implementation("com.github.stephengold:jolt-jni-Windows64:$joltVersion")
            // runtimeOnly("com.github.stephengold:jolt-jni-MacOSX_ARM64:$joltVersion:DebugSp")
            // runtimeOnly("com.github.stephengold:jolt-jni-MacOSX64:$joltVersion:DebugSp")
            // runtimeOnly("com.github.stephengold:jolt-jni-Windows64:$joltVersion:DebugSp")
            // runtimeOnly("com.github.stephengold:jolt-jni-Linux64:$joltVersion:DebugSp")
        }
    }
}

compose.resources {
    packageOfResClass = "azora.azora_studio.app.generated.resources"
}

compose.desktop {
    application {
        mainClass = "dev.azora.studio.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Azora Studio"
            packageVersion = "1.0.0"
            description = "Azora Studio"
            vendor = "DoubleGArts"

            // TODO: add azora_icon.icns / azora_icon.ico to src/desktopMain/resources
            // before running the Dmg/Msi packaging tasks (only azora_icon.png exists today).
            macOS {
                dockName = "Azora Studio"
                iconFile.set(project.file("src/desktopMain/resources/azora_icon.icns"))
            }

            windows {
                iconFile.set(project.file("src/desktopMain/resources/azora_icon.ico"))
            }

            linux {
                iconFile.set(project.file("src/desktopMain/resources/azora_icon.png"))
            }
        }

        jvmArgs("-Xdock:name=Azora Studio")
    }
}
