plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)

            implementation(projects.azoraSdkCore.project.domain)
        }

        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }

        jvmCommonMain.dependencies {
            implementation(libs.kotlin.reflect)
        }
    }
}