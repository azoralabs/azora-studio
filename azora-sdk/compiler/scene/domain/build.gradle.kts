plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)

            // Generic slot-graph operations (AzoraSlotGraph) behind SceneComponentTree.
            api(projects.azoraSdk.canvas.domain)
        }
    }
}
