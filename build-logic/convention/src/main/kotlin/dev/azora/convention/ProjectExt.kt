package dev.azora.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.kotlin.dsl.getByType

/**
 * Provides access to the version catalog named "libs".
 *
 * This extension property allows convention plugins to access dependencies
 * and versions defined in `gradle/libs.versions.toml`.
 *
 * ## Usage
 * ```kotlin
 * dependencies {
 *     implementation(libs.findLibrary("kotlinx-coroutines").get())
 * }
 *
 * val sdkVersion = libs.findVersion("android-compileSdk").get().toString()
 * ```
 *
 * @see VersionCatalog For available lookup methods
 */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")