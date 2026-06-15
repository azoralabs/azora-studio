plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.bundles.koin.common)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)

            implementation(projects.azoraSdk.docking.domain)
            implementation(projects.azoraSdk.docking.data)
            implementation(projects.azoraSdkCore.component)
            implementation(projects.azoraSdkCore.theme)
            implementation(projects.azoraSdkCore.presentation)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    packageOfResClass = "azora.azora_sdk.docking.presentation.generated.resources"
}
