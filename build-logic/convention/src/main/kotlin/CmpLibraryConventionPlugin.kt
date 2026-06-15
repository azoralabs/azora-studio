import dev.azora.convention.libs
import org.gradle.api.*
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Compose Multiplatform library modules with WASM support.
 *
 * ## Applied Plugins
 * - `kmp.library.wasm` - Base KMP library configuration with WASM target
 * - `kotlin.plugin.compose` - Compose compiler plugin
 * - `jetbrains.compose` - Compose Multiplatform
 *
 * ## Default Dependencies
 * Adds to `commonMain`:
 * - `compose.ui` - Core Compose UI
 * - `compose.foundation` - Foundation components
 * - `compose.material3` - Material 3 components
 * - `compose.material-icons-core` - Material icons
 *
 * ## Usage
 * ```kotlin
 * // In core/designsystem/build.gradle.kts
 * plugins {
 *     alias(libs.plugins.convention.cmp.library)
 * }
 * ```
 *
 * @see KmpLibraryConventionPlugin Base KMP library plugin
 */
class CmpLibraryConventionPlugin : Plugin<Project> {

    /**
     * Applies the Compose Multiplatform library configuration.
     *
     * @param target The Gradle project to configure
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dev.azora.convention.kmp.library")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("compose-ui").get())
                "commonMainImplementation"(libs.findLibrary("compose-foundation").get())
                "commonMainImplementation"(libs.findLibrary("compose-material3").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material-icons-core").get())
            }
        }
    }
}