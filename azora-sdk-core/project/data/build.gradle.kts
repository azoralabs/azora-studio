plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.bundles.ktor.common)
            implementation(libs.koin.core)
            implementation(libs.compose.ui)

            implementation(projects.azoraSdkCore.domain)
            implementation(projects.azoraSdkCore.io)
            implementation(projects.azoraSdkCore.project.domain)
            implementation(projects.azoraSdkCore.util)

            implementation(projects.azoraLocal.database)
        }
    }
}
