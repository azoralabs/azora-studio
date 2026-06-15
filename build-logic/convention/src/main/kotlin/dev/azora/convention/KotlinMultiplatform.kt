package dev.azora.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures Kotlin Multiplatform for library modules.
 *
 * This function sets up a complete multiplatform library targeting Android, iOS, and Desktop.
 * It configures:
 * - Android library target via [configureAndroidLibraryTarget]
 * - Desktop JVM target via [configureDesktopTarget]
 * - iOS framework binaries with proper naming
 * - Source set hierarchy via [applyHierarchyTemplate]
 * - Compiler options for expect/actual and opt-ins
 *
 * ## Compiler Options
 * - `-Xexpect-actual-classes` - Enable expect/actual class support
 * - `-opt-in=kotlin.RequiresOptIn` - Allow opt-in annotations
 * - `-opt-in=kotlin.time.ExperimentalTime` - Time API opt-in
 *
 * @see configureAndroidLibraryTarget For Android-specific configuration
 * @see configureDesktopTarget For Desktop-specific configuration
 * @see applyHierarchyTemplate For source set hierarchy
 */
internal fun Project.configureKotlinMultiplatform() {
    configureAndroidLibraryTarget()
    configureDesktopTarget()

    extensions.configure<KotlinMultiplatformExtension> {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = this@configureKotlinMultiplatform.pathToFrameworkName()
            }
        }

        applyHierarchyTemplate()

        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }
}
