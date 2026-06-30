plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.material3.adaptive)
            implementation(libs.bundles.koin.common)
            implementation(libs.jetbrains.lifecycle.viewmodel.nav3)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.compose.components.resources)

            implementation(projects.azoraSdkCore.domain)
            implementation(projects.azoraSdkCore.project.domain)
            implementation(projects.azoraSdk.canvas.domain)
            implementation(projects.azoraShared)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
        }

        desktopMain.dependencies {
            implementation(libs.javacv.platform)
        }
    }
}
