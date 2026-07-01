package dev.azora.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures the Android target for Kotlin Multiplatform library modules.
 *
 * Uses the `com.android.kotlin.multiplatform.library` plugin (required since AGP 9)
 * instead of the legacy `com.android.library` + `androidTarget()` combination.
 *
 * Configuration:
 * - Namespace derived from the project path (see [pathToPackageName])
 * - Compile/Min SDK versions from `android-compileSdk` / `android-minSdk`
 *   in the version catalog
 * - JVM target set to Java 17
 * - Android resources enabled (required for Compose resources on Android)
 */
internal fun Project.configureAndroidLibraryTarget() {
    extensions.configure<KotlinMultiplatformExtension> {
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
            namespace = this@configureAndroidLibraryTarget.pathToPackageName()
            compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()
            minSdk = libs.findVersion("android-minSdk").get().toString().toInt()

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }

            androidResources {
                enable = true
            }
        }
    }
}
