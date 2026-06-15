import dev.azora.convention.configureDesktopTarget
import org.gradle.api.*

/**
 * Convention plugin for Compose Multiplatform application modules (Desktop only).
 *
 * ## Applied Plugins
 * - `kotlin.multiplatform` - Kotlin Multiplatform support
 * - `jetbrains.compose` - Compose Multiplatform
 * - `kotlin.plugin.compose` - Compose compiler plugin
 * - `kotlin.plugin.serialization` - Kotlinx Serialization
 *
 * ## Configured Targets
 * - **Desktop** - JVM target via [configureDesktopTarget]
 *
 * ## Usage
 * ```kotlin
 * // In studioApp/build.gradle.kts
 * plugins {
 *     alias(libs.plugins.convention.cmp.application)
 * }
 * ```
 */
class CmpApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            configureDesktopTarget()
        }
    }
}
