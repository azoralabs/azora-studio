plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(projects.azoraSdkCore.domain)
            implementation(projects.azoraSdkCore.util)
            implementation(projects.azoraSdkCore.project.domain)
        }
    }
}
