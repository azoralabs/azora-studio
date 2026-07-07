plugins {
    alias(libs.plugins.convention.cmp.application)
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

            implementation(projects.studioApp.shared)

            implementation(projects.buildConfig)

            implementation(projects.azoraLocal.database)

            implementation(projects.azoraSdk.canvas.domain)
            implementation(projects.azoraSdk.canvas.presentation)

            // Universal compiler SDK — not used by Studio directly, but plugin JARs (React/KMP
            // generators) link against these classes at runtime via the parent classloader.
            implementation(projects.azoraSdk.compiler.scaffold)
            implementation(projects.azoraSdk.compiler.scene.data)
            implementation(projects.azoraSdk.compiler.scene.domain)

            implementation(projects.azoraSdk.color.presentation)

            implementation(projects.azoraSdk.docking.data)
            implementation(projects.azoraSdk.docking.domain)
            implementation(projects.azoraSdk.docking.presentation)

            implementation(projects.azoraSdk.nodes.domain)

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
            implementation(projects.azoraSdkLibrary.core)
            implementation(projects.azoraSdkLibrary.presentation)
        }

        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlin.stdlib)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(compose.desktop.currentOs)

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
        }
    }
}

// ── Bundled Azora Engine library ─────────────────────────────────────────
// Embeds the engine's .azlib bundle (built by ../azora-engine/tools/package.sh)
// as an app resource under bundled-libraries/; BundledLibraries installs it to
// ~/.azora/libraries on startup. Skipped gracefully when the dist isn't built.
val embedBundledLibraries = tasks.register<Copy>("embedBundledLibraries") {
    val engineDist = rootProject.projectDir.resolve("../azora-engine/dist")
    val outputDir = layout.buildDirectory.dir("generated/bundledLibraries/bundled-libraries").get().asFile
    from(engineDist) { include("*.azlib") }
    into(outputDir)
    doLast {
        outputDir.mkdirs()
        val names = outputDir.listFiles { f -> f.extension == "azlib" }?.map { it.name }?.sorted() ?: emptyList()
        outputDir.resolve("index.txt").writeText(names.joinToString("\n"))
        if (names.isEmpty()) {
            logger.warn("azora-engine dist not found at $engineDist — no library bundled (run azora-engine/tools/package.sh)")
        }
    }
}

kotlin.sourceSets.named("desktopMain") {
    resources.srcDir(layout.buildDirectory.dir("generated/bundledLibraries"))
}

tasks.matching { it.name == "desktopProcessResources" }.configureEach {
    dependsOn(embedBundledLibraries)
}

compose.desktop {
    application {
        mainClass = "dev.azora.studio.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
            )
            packageName = "Azora Studio"
            packageVersion = "1.0.0"
            description = "Azora Studio"
            vendor = "DoubleGArts"

            // jdk.unsupported: sun.misc.Unsafe used by datastore/atomicfu at runtime;
            // java.sql: needed by the bundled database driver. Neither is detected by jlink.
            modules("jdk.unsupported", "java.sql")

            macOS {
                bundleID = "com.azoralabs.azorastudio"
                dockName = "Azora Studio"
                iconFile.set(project.file("icons/azora_icon.icns"))
            }

            windows {
                // Stable GUID so MSI upgrades replace previous installs
                upgradeUuid = "F94ED144-7796-4061-B035-4BDFC7979A45"
                menuGroup = "Azora"
                perUserInstall = true
                dirChooser = true
                shortcut = true
                iconFile.set(project.file("icons/azora_icon.ico"))
            }

            linux {
                packageName = "azora-studio"
                menuGroup = "Development"
                iconFile.set(project.file("icons/azora_icon.png"))
            }
        }

        jvmArgs("-Xdock:name=Azora Studio")
    }
}
