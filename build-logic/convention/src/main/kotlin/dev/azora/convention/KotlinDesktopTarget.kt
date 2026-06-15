package dev.azora.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.*

/**
 * Configures the Desktop (JVM) target for Kotlin Multiplatform.
 *
 * Sets up a JVM target named "desktop" with:
 * - JVM target set to Java 17
 * - Applied to all compilations (main, test)
 *
 * The target name "desktop" creates source sets:
 * - `desktopMain` - Desktop-specific code
 * - `desktopTest` - Desktop-specific tests
 *
 * ## Platform Support
 * The desktop target supports:
 * - Windows (x64, arm64)
 * - macOS (x64, arm64)
 * - Linux (x64, arm64)
 */
internal fun Project.configureDesktopTarget() {
    extensions.configure<KotlinMultiplatformExtension> {
        jvm("desktop") {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
            }
        }
    }
}