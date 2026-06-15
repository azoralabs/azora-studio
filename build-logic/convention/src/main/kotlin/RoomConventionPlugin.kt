import androidx.room.gradle.RoomExtension
import dev.azora.convention.libs
import org.gradle.api.*
import org.gradle.kotlin.dsl.*

/**
 * Convention plugin for Room database integration in Kotlin Multiplatform.
 *
 * This plugin configures Room for multiplatform projects, setting up the schema
 * directory and applying KSP for all supported platforms.
 *
 * ## Applied Plugins
 * - `com.google.devtools.ksp` - Kotlin Symbol Processing
 * - `androidx.room` - Room database plugin
 *
 * ## Configuration
 * - **Schema Directory**: `$projectDir/schemas` for migration tracking
 *
 * ## Default Dependencies
 * - `androidx-room-runtime` - Room runtime (commonMain API)
 * - `sqlite-bundled` - SQLite bundled implementation
 * - `androidx-room-compiler` - KSP processor for all targets:
 *   - Android, iOS (arm64, simulatorArm64), Desktop
 *
 * ## Usage
 * ```kotlin
 * // In core/database/build.gradle.kts
 * plugins {
 *     alias(libs.plugins.convention.kmp.library)
 *     alias(libs.plugins.convention.room)
 * }
 * ```
 *
 * ## Schema Migrations
 * Room schemas are exported to `schemas/` directory for migration testing.
 * These should be committed to version control.
 */
class RoomConventionPlugin : Plugin<Project> {

    /**
     * Applies Room database configuration to the target project.
     *
     * Sets up KSP for Room annotation processing across all supported platforms
     * (Android, iOS, Desktop), configures the schema export directory for
     * migration tracking, and adds Room runtime dependencies.
     *
     * @param target The Gradle project to configure
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("androidx.room")
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "nonWebMainApi"(libs.findLibrary("androidx-room-runtime").get())
                "nonWebMainApi"(libs.findLibrary("sqlite-bundled").get())
                "kspAndroid"(libs.findLibrary("androidx-room-compiler").get())
                "kspIosSimulatorArm64"(libs.findLibrary("androidx-room-compiler").get())
                "kspIosArm64"(libs.findLibrary("androidx-room-compiler").get())
                "kspDesktop"(libs.findLibrary("androidx-room-compiler").get())
            }
        }
    }
}