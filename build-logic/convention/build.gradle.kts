import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "dev.azora.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.androidx.room.gradle.plugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "dev.azora.convention.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("cmpLibrary") {
            id = "dev.azora.convention.cmp.library"
            implementationClass = "CmpLibraryConventionPlugin"
        }
        register("cmpApplication") {
            id = "dev.azora.convention.cmp.application"
            implementationClass = "CmpApplicationConventionPlugin"
        }
        register("cmpFeature") {
            id = "dev.azora.convention.cmp.feature"
            implementationClass = "CmpFeatureConventionPlugin"
        }
        register("cmpFirebase") {
            id = "dev.azora.convention.cmp.firebase"
            implementationClass = "CmpFirebaseConventionPlugin"
        }
        register("room") {
            id = "dev.azora.convention.room"
            implementationClass = "RoomConventionPlugin"
        }
    }
}