plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)

            api(projects.azoraSdk.compiler.scene.domain)
            implementation(projects.azoraSdkCore.io)
        }
    }
}
