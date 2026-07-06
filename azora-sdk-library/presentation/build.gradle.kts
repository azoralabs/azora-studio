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

            implementation(projects.azoraSdkCore.io)
            implementation(projects.azoraSdkCore.project.domain)
            api(projects.azoraSdkLibrary.core)
        }
    }
}
