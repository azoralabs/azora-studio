plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.koin.common)
            implementation(libs.compose.components.resources)

            implementation(projects.azoraSdkCore.io)
            implementation(projects.azoraSdkCore.project.domain)
            implementation(projects.azoraSdkCore.theme)
            implementation(projects.azoraSdkPlugin.core)
        }
    }
}