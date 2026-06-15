plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.bundles.ktor.common)
            implementation(libs.touchlab.kermit)
            implementation(libs.koin.core)
            implementation(libs.datastore.preferences)
            implementation(libs.sqlite.bundled)

            implementation(projects.buildConfig)

            implementation(projects.azoraSdkCore.domain)

            implementation(projects.azoraShared)
        }

        jvmCommonMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
