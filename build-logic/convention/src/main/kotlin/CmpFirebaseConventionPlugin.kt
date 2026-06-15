import dev.azora.convention.libs
import org.gradle.api.*
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Firebase integration in Compose Multiplatform modules.
 *
 * This plugin configures Firebase dependencies for multiplatform projects using:
 * - **GitLive Firebase** wrappers for common/iOS code
 * - **Official Firebase SDK** for Android via BOM
 *
 * ## Default Dependencies
 * ### Common (all platforms via GitLive)
 * - `firebase-storage-gitlive` - Cloud Storage
 * - `firebase-common-gitlive` - Common Firebase utilities
 *
 * ### Android (official SDK)
 * - `firebase-bom` - Bill of Materials for version management
 * - `firebase-storage` - Cloud Storage
 * - `firebase-common` - Common Firebase utilities
 *
 * ## Important
 * On Android, the Firebase BOM must be applied as a platform dependency
 * **before** individual Firebase libraries to ensure consistent versioning.
 *
 * ## Usage
 * ```kotlin
 * // In feature/map/presentation/build.gradle.kts
 * plugins {
 *     alias(libs.plugins.convention.cmp.feature)
 *     alias(libs.plugins.convention.cmp.firebase)
 * }
 * ```
 *
 * @see <a href="https://github.com/nicholashakes/firebase-kotlin-sdk">GitLive Firebase SDK</a>
 */
class CmpFirebaseConventionPlugin : Plugin<Project> {

    /**
     * Applies Firebase dependencies to the target project for multiplatform use.
     *
     * Uses nonWebMain source set for GitLive Firebase wrappers since they don't
     * support WASM. Firebase functionality is only available on Android, iOS, and Desktop.
     *
     * @param target The Gradle project to configure
     */
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                "nonWebMainImplementation"(libs.findLibrary("firebase.common.gitlive").get())
                "androidMainImplementation"(platform(libs.findLibrary("firebase.bom").get()))
                "androidMainImplementation"(libs.findLibrary("firebase.common").get())
            }
        }
    }
}