import dev.azora.convention.*
import org.gradle.api.*
import org.gradle.kotlin.dsl.*

/**
 * Convention plugin for Kotlin Multiplatform library modules.
 *
 * ## Applied Plugins
 * - `kotlin.multiplatform` - Kotlin Multiplatform
 * - `com.android.kotlin.multiplatform.library` - Android library support (AGP 9 KMP plugin)
 * - `kotlin.plugin.serialization` - Kotlinx Serialization
 *
 * ## Configured Targets
 * - **Android** - Library with namespace derived from project path
 * - **iOS** - iosArm64, iosSimulatorArm64 with framework
 * - **Desktop** - JVM target with Java 17
 *
 * ## Source Set Hierarchy
 * See [dev.azora.convention.applyHierarchyTemplate]: intermediate source sets
 * `nonWebMain` (all targets), `mobileMain` (Android + iOS) and
 * `jvmCommonMain` (Android + Desktop) are available.
 *
 * ## Usage
 * ```kotlin
 * // In core/data/build.gradle.kts
 * plugins {
 *     alias(libs.plugins.convention.kmp.library)
 * }
 * ```
 */
class KmpLibraryConventionPlugin : Plugin<Project> {

    /**
     * Applies the Kotlin Multiplatform library configuration.
     *
     * @param target The Gradle project to configure
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            configureKotlinMultiplatform()

            dependencies {
                "commonMainImplementation"(libs.findLibrary("kotlinx-serialization-json").get())
                "commonMainImplementation"(libs.findLibrary("kotlinx-datetime").get())
                "commonTestImplementation"(libs.findLibrary("kotlin-test").get())
            }
        }
    }
}
