import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
}

// Scaffold for shared Studio code across platforms. Studio's full common UI/logic
// currently lives in :studioApp:desktopApp and will be migrated here once Studio is
// detangled from its desktop-only dependencies (azora-lang, LWJGL/Vulkan).
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "StudioShared"
            isStatic = true
        }
    }

    jvm()

    androidLibrary {
        namespace = "dev.azora.studio.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        androidMain.dependencies {
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
        commonMain.dependencies {
            implementation(project(":azora-sdk-core:project:domain"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)

            implementation(libs.bundles.koin.common)
            implementation(libs.kotlinx.serialization.json)

            implementation(projects.buildConfig)

            implementation(projects.azoraLocal.database)

            implementation(projects.azoraSdk.canvas.domain)
            implementation(projects.azoraSdk.canvas.presentation)

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
            implementation(projects.azoraSdkCore.project.data)
            implementation(projects.azoraSdkCore.project.presentation)
            implementation(projects.azoraSdkCore.theme)
            implementation(projects.azoraSdkCore.util)

            implementation(projects.azoraSdkPlugin.core)
            implementation(projects.azoraSdkPlugin.presentation)
            implementation(projects.azoraSdkLibrary.core)
            implementation(projects.azoraSdkLibrary.presentation)
        }
    }
}

// Studio's Compose resources live here and are consumed by :studioApp:desktopApp too,
// so the generated Res class is public and uses the package the sources import.
compose.resources {
    publicResClass = true
    packageOfResClass = "azora.azora_studio.app.generated.resources"
}
