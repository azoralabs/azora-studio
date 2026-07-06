plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.json)

            implementation(projects.azoraSdkCore.project.domain)
            implementation(projects.azoraSdkCore.io)
            implementation(projects.azoraSdkCore.domain)
        }
    }
}
