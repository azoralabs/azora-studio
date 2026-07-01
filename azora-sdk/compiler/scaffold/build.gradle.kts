plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        desktopMain.dependencies {
            implementation(libs.kotlin.stdlib)

            implementation(projects.azoraSdkCore.io)
            implementation(projects.azoraSdkCore.project.domain)
        }
    }
}
