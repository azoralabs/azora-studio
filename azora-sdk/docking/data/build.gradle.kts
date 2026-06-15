plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)

            implementation(projects.azoraSdkCore.domain)
            implementation(projects.azoraSdk.docking.domain)
            implementation(projects.azoraSdkCore.util)
        }
    }
}
