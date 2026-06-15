import dev.azora.convention.libs
import org.gradle.api.*
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Compose Multiplatform feature modules with WASM support.
 *
 * This plugin extends [CmpLibraryConventionPlugin] with feature-specific dependencies
 * including navigation, dependency injection, and ViewModel support.
 *
 * ## Applied Plugins
 * - `cmp.library` - Base CMP library configuration with WASM target
 *
 * @see CmpLibraryConventionPlugin Base CMP library plugin
 */
class CmpFeatureConventionPlugin : Plugin<Project> {

    /**
     * Applies the Compose Multiplatform feature module configuration with WASM support.
     *
     * @param target The Gradle project to configure
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dev.azora.convention.cmp.library")
            }

            dependencies {
                "commonMainImplementation"(project(":azora-sdk-core:presentation"))
                "commonMainImplementation"(project(":azora-sdk-core:theme"))

                "commonMainImplementation"(platform(libs.findLibrary("koin-bom").get()))
                "androidMainImplementation"(platform(libs.findLibrary("koin-bom").get()))

                "commonMainImplementation"(libs.findLibrary("koin-compose").get())
                "commonMainImplementation"(libs.findLibrary("koin-compose-viewmodel").get())

                "commonMainImplementation"(libs.findLibrary("compose-runtime").get())

                "commonMainImplementation"(libs.findLibrary("jetbrains-navigation3-ui").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-lifecycle-viewmodel-nav3").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-lifecycle-viewmodel").get())

                "commonMainImplementation"(libs.findLibrary("jetbrains-savedstate").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-bundle").get())

                "androidMainImplementation"(libs.findLibrary("koin-android").get())
                "androidMainImplementation"(libs.findLibrary("koin-androidx-compose").get())
                "androidMainImplementation"(libs.findLibrary("koin-core-viewmodel").get())
            }
        }
    }
}
