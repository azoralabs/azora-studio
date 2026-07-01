import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.launcherApp.shared)
    implementation(projects.azoraSdkPlugin.core)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.ui.tooling.preview)
}

compose.desktop {
    application {
        mainClass = "dev.azora.launcher.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )
            packageName = "Azora Launcher"
            packageVersion = "1.0.0"
            description = "Launcher for Azora Studio"
            vendor = "Azora Labs"

            macOS {
                bundleID = "com.azoralabs.azorastudio.launcher"
                dockName = "Azora Launcher"
                iconFile.set(project.file("icons/azora_icon.icns"))
            }

            windows {
                // Stable GUID so MSI upgrades replace previous installs
                upgradeUuid = "772329EA-67E2-4455-8FE9-39D353071B05"
                menuGroup = "Azora"
                perUserInstall = true
                dirChooser = true
                shortcut = true
                iconFile.set(project.file("icons/azora_icon.ico"))
            }

            linux {
                packageName = "azora-launcher"
                menuGroup = "Development"
                iconFile.set(project.file("icons/azora_icon.png"))
            }
        }
    }
}